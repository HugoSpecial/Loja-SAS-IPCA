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

    suspend fun isBeneficiario(uid: String): Boolean? {
        val doc = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()

        return doc.getBoolean("isBeneficiary")
    }

    fun login(onNavigate: (String) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        val email = uiState.value.email ?: ""
        val password = uiState.value.password ?: ""

        val auth = Firebase.auth

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // 🔥 Verificar beneficiario dentro de coroutine
                    viewModelScope.launch {
                        val benef = isBeneficiario(uid)

                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = null
                        )

                        if (benef == true) {
                            onNavigate("home") // Beneficiário
                        } else {
                            onNavigate("candidature") // Não-beneficiário
                        }
                    }

                } else {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Wrong password or no internet connection"
                    )
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