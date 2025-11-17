package ipca.project.lojasas.ui.profile

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import ipca.example.lojasas.models.Utilizador
import kotlinx.coroutines.launch

data class ProfileState(
    val user: Utilizador? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isEditing: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG = "ProfileViewModel"

    var uiState = mutableStateOf(ProfileState())
        private set

    fun logout(onLogoutSuccess: () -> Unit) {
        try {
            auth.signOut()
            Log.d(TAG, "User logged out successfully")
            // Reset state
            uiState.value = ProfileState()
            onLogoutSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            uiState.value = uiState.value.copy(
                error = e.message ?: "Erro ao fazer logout"
            )
        }
    }
}