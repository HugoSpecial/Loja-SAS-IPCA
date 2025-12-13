package ipca.project.lojasas.ui.authentication

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.TAG
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.getValue

data class LoginState (
    var email : String? = null,
    var password : String? = null,
    var error : String? = null,
    var isLoading : Boolean = false
)

class LoginViewModel : ViewModel() {

    var uiState = mutableStateOf(LoginState())
        private set

    fun updateEmail(email: String) {
        uiState.value = uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        uiState.value = uiState.value.copy(password = password)
    }

    fun login(onNavigate: (String) -> Unit) {
        // 1. Validação de segurança ANTES de chamar o Firebase
        val email = uiState.value.email
        val password = uiState.value.password

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            uiState.value = uiState.value.copy(error = "Preencha todos os campos.")
            return
        }

        // Inicia loading
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            try {
                // 2. Autenticação (Login) usando await()
                // Isto suspende a execução até o login terminar, sem bloquear a thread principal
                auth.signInWithEmailAndPassword(email, password).await()

                // Se passou daqui, o login está feito. Agora vamos buscar os dados.
                val uid = auth.currentUser?.uid ?: throw Exception("Erro ao obter UID.")

                // 3. Buscar dados extra no Firestore
                val userDoc = db.collection("users").document(uid).get().await()

                // Leitura segura dos campos (evita crashes se o campo não existir)
                val isBeneficiary = userDoc.getBoolean("isBeneficiary") ?: false
                val isCollaborator = userDoc.getBoolean("isCollaborator") ?: false
                val candidatureId = userDoc.getString("candidatureId")

                // Sucesso! Parar loading
                uiState.value = uiState.value.copy(isLoading = false)

                // 4. Lógica de Navegação
                if (isCollaborator) {
                    onNavigate("collaborator")
                } else if (isBeneficiary) {
                    onNavigate("home")
                } else {
                    if (!candidatureId.isNullOrEmpty()) {
                        onNavigate("await-candidature")
                    } else {
                        onNavigate("candidature")
                    }
                }

            } catch (e: FirebaseAuthInvalidUserException) {
                // Conta não existe ou foi desativada
                uiState.value =
                    uiState.value.copy(isLoading = false, error = "Conta não encontrada.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                // Password errada
                uiState.value =
                    uiState.value.copy(isLoading = false, error = "Email ou password incorretos.")
            } catch (e: Exception) {
                // Outros erros (ex: Sem internet)
                uiState.value =
                    uiState.value.copy(isLoading = false, error = e.message ?: "Erro desconhecido.")
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        try {
            Firebase.auth.signOut()
            Log.d(TAG, "User logged out successfully")
            // Limpa o estado
            uiState.value = LoginState()
            onLogoutSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            uiState.value = uiState.value.copy(
                error = e.message ?: "Erro ao fazer logout"
            )
        }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }
}