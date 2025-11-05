package ipca.project.lojasas.ui.authentication

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import ipca.project.lojasas.TAG

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

    fun login(onLoginSuccess: () -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (uiState.value.email.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Email is required"
            )
            return
        }

        if (uiState.value.password.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Password is required"
            )
            return
        }

        var auth: FirebaseAuth
        auth = Firebase.auth
        auth.signInWithEmailAndPassword(
            uiState.value.email!!,
            uiState.value.password!!
        )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    //updateUI(user)
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    onLoginSuccess()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Wrong password or no internet connection"
                    )
                }
            }
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