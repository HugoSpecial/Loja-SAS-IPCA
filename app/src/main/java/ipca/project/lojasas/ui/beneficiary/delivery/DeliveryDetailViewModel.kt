package ipca.project.lojasas.ui.beneficiary.delivery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
    private val auth = Firebase.auth

    // 1. Busca dados da notificação (para mostrar no topo)
    fun fetchNotificationData(notificationId: String) {
        if (notificationId.isEmpty()) return

        db.collection("notifications").document(notificationId).get()
            .addOnSuccessListener {
                uiState = uiState.copy(
                    notificationTitle = it.getString("title") ?: "",
                    notificationBody = it.getString("body") ?: ""
                )
            }
    }

    // 2. Verifica se já existe nota (para bloquear se necessário)
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

    // 3. GRAVAR NOTA + ENVIAR NOTIFICAÇÃO AO COLABORADOR
    fun saveDeliveryNote(orderId: String, noteToSave: String) {
        if (orderId.isEmpty()) return

        uiState = uiState.copy(isLoading = true, error = null)

        // PASSO A: Gravar a nota na entrega
        db.collection("delivery").document(orderId)
            .update("beneficiaryNote", noteToSave)
            .addOnSuccessListener {

                // PASSO B: Se gravou bem, envia a notificação para o colaborador
                sendCollaboratorNotification(orderId, noteToSave)

            }
            .addOnFailureListener { e ->
                uiState = uiState.copy(isLoading = false, error = "Erro ao gravar: ${e.message}")
            }
    }

    // Função auxiliar para criar a notificação
    private fun sendCollaboratorNotification(orderId: String, message: String) {
        val currentUser = auth.currentUser?.uid ?: ""

        // Cria o objeto da notificação
        val notification = hashMapOf(
            "title" to "Nova resposta de Entrega",
            "body" to "O beneficiário respondeu: $message",
            "date" to com.google.firebase.Timestamp.now(),
            "read" to false,
            "type" to "resposta_entrega", // Tipo especial para o colaborador identificar
            "relatedId" to orderId,        // ID da entrega para o colaborador abrir
            "senderId" to currentUser,
            "receiverId" to "collaborator" // Podes ajustar se quiseres mandar para um colaborador específico
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                // SUCESSO TOTAL: Nota gravada e Notificação enviada
                uiState = uiState.copy(isLoading = false, isSaved = true)
            }
            .addOnFailureListener {
                // Se falhar a notificação, assumimos que está salvo na mesma, mas avisamos (opcional)
                uiState = uiState.copy(isLoading = false, isSaved = true)
            }
    }
}