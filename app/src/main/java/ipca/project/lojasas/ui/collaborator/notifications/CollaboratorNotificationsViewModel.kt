package ipca.project.lojasas.ui.collaborator.notifications

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Notification
// Certifica-te que tens este import ou usa a String "CANCELADO" diretamente
import ipca.project.lojasas.models.DeliveryState

data class NotificationListState(
    val notifications: List<Notification> = emptyList(),
    val allNotifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsCollaboratorViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(NotificationListState())
        private set

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        db.collection("notifications")
            .where(
                Filter.or(
                    Filter.equalTo("recipientId", userId),
                    Filter.equalTo("targetProfile", "COLABORADOR")
                )
            )
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = value?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.apply { docId = doc.id }
                } ?: emptyList()

                val naoLidas = list.count { !it.read }
                val currentFilter = uiState.value.selectedFilter
                val filteredList = if (currentFilter == null) list else list.filter { it.type == currentFilter }

                uiState.value = uiState.value.copy(
                    allNotifications = list,
                    notifications = filteredList,
                    unreadCount = naoLidas,
                    isLoading = false,
                    error = null
                )
            }
    }

    fun filterByType(type: String?) {
        val masterList = uiState.value.allNotifications
        val newList = if (type == null) masterList else masterList.filter { it.type == type }
        uiState.value = uiState.value.copy(selectedFilter = type, notifications = newList)
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnFailureListener { Log.e("NotifVM", "Erro ao marcar como lida", it) }
    }

    // --- FUNÇÃO CORRIGIDA ---
    fun handleJustificationDecision(notification: Notification, accepted: Boolean, onSuccess: () -> Unit) {
        val deliveryId = notification.relatedId
        // O beneficiário que enviou a justificação está no senderId
        val beneficiaryUserId = notification.senderId

        if (deliveryId.isEmpty()) return

        // 1. CASO ACEITE: Não faz nada, só marca notificação como lida
        if (accepted) {
            markAsRead(notification.docId)
            onSuccess()
            return
        }

        // 2. CASO RECUSADO (Aplicar Falta): Executa a lógica do FaultDelivery
        if (beneficiaryUserId.isNotEmpty()) {
            applyFaultToBeneficiary(userId = beneficiaryUserId, deliveryId = deliveryId) {
                // Se correr bem, marca como lida e fecha o popup
                markAsRead(notification.docId)
                onSuccess()
            }
        } else {
            uiState.value = uiState.value.copy(error = "Erro: ID do beneficiário não encontrado.")
        }
    }

    // Lógica copiada/adaptada do teu DeliveryViewModel (FaultDelivery)
    private fun applyFaultToBeneficiary(userId: String, deliveryId: String, onComplete: () -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        val userRef = db.collection("users").document(userId)
        val deliveryRef = db.collection("delivery").document(deliveryId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)

            // Incrementa faltas
            val currentFaults = (userSnapshot.getLong("fault") ?: 0).toInt()
            val newFaults = currentFaults + 1

            // Se tiver 2 ou mais faltas, deixa de ser beneficiário
            val isBeneficiary = newFaults < 2

            // Atualiza User
            transaction.update(userRef, "fault", newFaults)
            transaction.update(userRef, "isBeneficiary", isBeneficiary)

            // Atualiza Delivery para CANCELADO (Falta Injustificada)
            // Se não tiveres acesso ao Enum DeliveryState aqui, usa a string "CANCELADO"
            transaction.update(deliveryRef, "state", "CANCELADO")
            transaction.update(deliveryRef, "reason", "Justificação Rejeitada / Falta de comparência")
            transaction.update(deliveryRef, "delivered", false)

        }.addOnSuccessListener {
            uiState.value = uiState.value.copy(isLoading = false)
            Log.d("NotifVM", "Falta aplicada com sucesso.")
            onComplete()

        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Erro ao aplicar falta: ${e.message}"
            )
        }
    }

    fun fetchBeneficiaryDetails(userId: String, onResult: (String, String) -> Unit) {
        if (userId.isEmpty()) {
            onResult("Desconhecido", "--")
            return
        }
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Sem nome"
                val phone = doc.getString("phone") ?: "--"
                onResult(name, phone)
            }
            .addOnFailureListener {
                onResult("Erro ao carregar", "--")
            }
    }
}