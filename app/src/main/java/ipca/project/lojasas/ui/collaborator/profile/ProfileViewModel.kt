package ipca.project.lojasas.ui.collaborator.profile

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.User
import ipca.project.lojasas.TAG

data class ProfileCollaboratorState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val preferences: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null // Pode servir para mostrar um "visto" verde discreto
)

class ProfileCollaboratorViewModel : ViewModel() {

    // Estado da UI
    var uiState = mutableStateOf(ProfileCollaboratorState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // --- Funções de atualização (Agora com AUTO-SAVE) ---

    // Nota: Nome e Email geralmente são read-only neste design,
    // mas se forem editáveis, a lógica seria igual à do phone.
    fun onNameChange(newValue: String) {
        uiState.value = uiState.value.copy(name = newValue)
    }

    fun onEmailChange(newValue: String) {
        uiState.value = uiState.value.copy(email = newValue)
    }

    fun onPhoneChange(newValue: String) {
        // 1. Atualiza o valor localmente para a UI responder rápido
        uiState.value = uiState.value.copy(phone = newValue)

        // 2. Chama a gravação automática
        saveToFirestore()
    }

    fun onPreferencesChange(newValue: String) {
        // 1. Atualiza o valor localmente
        uiState.value = uiState.value.copy(preferences = newValue)

        // 2. Chama a gravação automática
        saveToFirestore()
    }

    // --- Função interna para salvar na Firebase (Silenciosa) ---
    private fun saveToFirestore() {
        val uid = auth.currentUser?.uid ?: return

        // IMPORTANTE: No auto-save, NÃO colocamos isLoading = true.
        // Se colocarmos, o teclado pode fechar ou a UI piscar enquanto o utilizador digita.
        // uiState.value = uiState.value.copy(isLoading = true) <--- REMOVIDO

        val updates = hashMapOf<String, Any>(
            // O design diz que só editamos telemóvel e preferências,
            // mas envio tudo por segurança (ou podes remover name/email daqui)
            "name" to uiState.value.name,
            "email" to uiState.value.email,
            "phone" to uiState.value.phone,
            "preferences" to uiState.value.preferences
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                // Sucesso silencioso
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    success = "Guardado", // Podes usar isto na UI para mostrar um ícone pequeno
                    error = null
                )
                Log.d(TAG, "Auto-save success")
            }
            .addOnFailureListener { e ->
                // Erro silencioso (ou mostramos se for crítico)
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao salvar: ${e.message}"
                )
                Log.e(TAG, "Auto-save error", e)
            }
    }

    // --- Função para carregar os dados iniciais ---
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)

                if (user != null) {
                    uiState.value = uiState.value.copy(
                        name = user.name ?: "",
                        email = user.email ?: "",
                        phone = user.phone ?: "",
                        preferences = user.preferences ?: "",
                        isLoading = false,
                        error = null
                    )
                } else {
                    uiState.value = uiState.value.copy(isLoading = false)
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
    }

    // --- Logout ---
    fun logout(onLogoutSuccess: () -> Unit) {
        try {
            auth.signOut()
            Log.d(TAG, "User logged out successfully")
            uiState.value = ProfileCollaboratorState() // Reseta o estado
            onLogoutSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            uiState.value = uiState.value.copy(
                error = e.message ?: "Erro ao fazer logout"
            )
        }
    }
}