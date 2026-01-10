package ipca.project.lojasas.ui.beneficiary.profile

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.User
import ipca.project.lojasas.TAG

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val preferences: String = "",
    val fault: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

class ProfileViewModel : ViewModel() {

    // Estado da UI
    var uiState = mutableStateOf(ProfileState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Variável para controlar o listener (para poder desligá-lo depois)
    private var userListener: ListenerRegistration? = null

    // --- Funções de atualização (Auto-save) ---

    fun onPhoneChange(newValue: String) {
        uiState.value = uiState.value.copy(phone = newValue)
        saveToFirestore()
    }

    fun onPreferencesChange(newValue: String) {
        uiState.value = uiState.value.copy(preferences = newValue)
        saveToFirestore()
    }

    // --- Função interna para salvar na Firebase ---
    private fun saveToFirestore() {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "phone" to uiState.value.phone,
            "preferences" to uiState.value.preferences
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    success = "Guardado",
                    error = null
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao salvar: ${e.message}"
                )
            }
    }

    // --- LISTENER EM TEMPO REAL ---
    // Substituímos o 'get()' pelo 'addSnapshotListener'
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        // Se já estivermos a escutar, não criamos outro listener
        if (userListener != null) return

        uiState.value = uiState.value.copy(isLoading = true)

        userListener = db.collection("users").document(uid)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)

                    if (user != null) {
                        // Atualiza o estado sempre que a BD mudar
                        uiState.value = uiState.value.copy(
                            name = user.name ?: "",
                            email = user.email ?: "",
                            // Só atualizamos phone/prefs se o user NÃO estiver a editar no momento
                            // (Para simplificar, aqui atualizamos sempre, mas em apps complexas
                            // pode ser preciso verificar foco)
                            phone = user.phone ?: "",
                            preferences = user.preferences ?: "",
                            fault = user.fault, // <--- Aqui as faltas atualizam sozinhas
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false)
                }
            }
    }

    // --- Logout ---
    fun logout(onLogoutSuccess: () -> Unit) {
        try {
            // Remove o listener antes de sair para evitar erros
            userListener?.remove()
            userListener = null

            auth.signOut()
            uiState.value = ProfileState() // Reseta o estado
            onLogoutSuccess()
        } catch (e: Exception) {
            uiState.value = uiState.value.copy(error = e.message)
        }
    }

    // Limpeza quando o ViewModel é destruído (ex: sair do ecrã)
    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}