package ipca.project.lojasas.ui.beneficiary.delivery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class DeliveryDetailState(
    val notificationTitle: String = "",
    val notificationBody: String = "",
    val userNote: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class DeliveryDetailViewModel : ViewModel() {

    var uiState by mutableStateOf(DeliveryDetailState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun fetchNotificationData(notificationId: String) {
        if (notificationId.isEmpty() || notificationId == "sem_notificacao") return

        db.collection("notifications").document(notificationId).get()
            .addOnSuccessListener {
                uiState = uiState.copy(
                    notificationTitle = it.getString("title") ?: "",
                    notificationBody = it.getString("body") ?: ""
                )
            }
    }

    fun checkExistingNote(orderId: String) {
        if (orderId.isEmpty()) return

        uiState = uiState.copy(isLoading = true)

        db.collection("delivery").document(orderId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val existingNote = document.getString("beneficiaryNote")
                    if (!existingNote.isNullOrEmpty()) {
                        uiState = uiState.copy(
                            userNote = existingNote,
                            isSaved = true,
                            isLoading = false
                        )
                    } else {
                        uiState = uiState.copy(isLoading = false, isSaved = false)
                    }
                } else {
                    uiState = uiState.copy(isLoading = false)
                }
            }
            .addOnFailureListener {
                uiState = uiState.copy(isLoading = false, error = "Erro ao carregar dados.")
            }
    }

    fun onUserNoteChange(newText: String) {
        uiState = uiState.copy(userNote = newText)
    }

    fun saveDeliveryNote(orderId: String, noteToSave: String) {
        if (orderId.isEmpty()) return

        uiState = uiState.copy(isLoading = true, error = null)

        db.collection("delivery").document(orderId)
            .update("beneficiaryNote", noteToSave)
            .addOnSuccessListener {
                sendCollaboratorNotification(orderId, noteToSave)
            }
            .addOnFailureListener { e ->
                uiState = uiState.copy(isLoading = false, error = "Erro ao gravar: ${e.message}")
            }
    }

    private fun sendCollaboratorNotification(orderId: String, message: String) {
        val currentUserId = auth.currentUser?.uid ?: ""

        val notification = hashMapOf(
            "title" to "Nova Resposta de Entrega",
            "body" to "Beneficiário respondeu: $message",
            "date" to Timestamp.now(),
            "read" to false,
            "type" to "resposta_entrega_rejeitada",
            "relatedId" to orderId,
            "senderId" to currentUserId,
            "recipientId" to "",
            "targetProfile" to "COLABORADOR"
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                uiState = uiState.copy(isLoading = false, isSaved = true)
            }
            .addOnFailureListener {
                uiState = uiState.copy(isLoading = false, isSaved = true)
            }
    }
}