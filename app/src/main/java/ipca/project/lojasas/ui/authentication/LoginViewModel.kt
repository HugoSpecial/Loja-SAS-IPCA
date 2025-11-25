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
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.signInWithEmailAndPassword(uiState.value.email!!, uiState.value.password!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    viewModelScope.launch {
                        try {
                            val userDoc = db.collection("users").document(uid).get().await()

                            val isBeneficiary = userDoc.getBoolean("isBeneficiary") ?: false
                            val isCollaborator = userDoc.getBoolean("isCollaborator") ?: false
                            val candidatureId = userDoc.getString("candidatureId")

                            uiState.value = uiState.value.copy(isLoading = false)

                            if (isCollaborator) {
                                onNavigate("colaborador")
                            }
                            else if (isBeneficiary) {
                                onNavigate("home")
                            }
                            else {
                                if (!candidatureId.isNullOrEmpty()) {
                                    onNavigate("await-candidature")
                                } else {
                                    onNavigate("candidature")
                                }
                            }

                        } catch (e: Exception) {
                            uiState.value = uiState.value.copy(isLoading = false, error = e.message)
                        }
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Login falhou.")
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