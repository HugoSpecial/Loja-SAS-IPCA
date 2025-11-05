package ipca.project.lojasas.ui.authentication

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import ipca.project.lojasas.TAG

data class ForgotPasswordState (
    var email : String? = null,
    var error : String? = null,
    var isLoading : Boolean = false
)

class ForgotPasswordViewModel : ViewModel() {

    var uiState = mutableStateOf(ForgotPasswordState())
        private set

    fun updateEmail(email: String) {
        uiState.value = uiState.value.copy(email = email)
    }

    fun forgotPassword(onForgotPasswordResult: (Boolean) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (uiState.value.email.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Email is required"
            )
            onForgotPasswordResult(false)
            return
        }

        val auth = Firebase.auth
        auth.sendPasswordResetEmail(uiState.value.email!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sendPasswordResetEmail:success")
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Password reset email sent successfully!" // ← Mensagem de sucesso no error
                    )
                    onForgotPasswordResult(true)
                } else {
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)

                    // Mensagem de erro mais apropriada
                    val errorMessage = when {
                        task.exception is FirebaseAuthInvalidUserException ->
                            "No account found with this email"
                        task.exception is FirebaseAuthInvalidCredentialsException ->
                            "Invalid email format"
                        else ->
                            "Failed to send reset email. Please check your connection and try again."
                    }

                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    onForgotPasswordResult(false)
                }
            }
    }
}