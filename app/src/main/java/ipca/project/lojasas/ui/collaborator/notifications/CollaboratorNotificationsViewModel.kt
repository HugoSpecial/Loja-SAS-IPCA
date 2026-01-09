package ipca.project.lojasas.ui.collaborator.notifications

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Notification

data class NotificationListState(
    val notifications: List<Notification> = emptyList(),     // Lista visível (filtrada)
    val allNotifications: List<Notification> = emptyList(),  // Lista completa (backup)
    val unreadCount: Int = 0,
    val selectedFilter: String? = null,                      // Null = "Tudo"
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

    // --- LÓGICA DE FILTRAGEM CENTRALIZADA ---
    // Define aqui quais tipos pertencem a qual botão
    private fun matchesFilter(notification: Notification, filterKey: String?): Boolean {
        if (filterKey == null) return true // "Tudo"

        return when (filterKey) {
            "GROUP_PEDIDOS" -> notification.type in listOf("pedido_novo", "pedido_agendado", "pedido_estado", "resposta_entrega")
            "GROUP_CANDIDATURAS" -> notification.type in listOf("candidatura_nova", "candidatura_estado")
            "GROUP_JUSTIFICACOES" -> notification.type == "resposta_entrega_rejeitada"
            "GROUP_SISTEMA" -> notification.type in listOf("validade_alerta", "sistema_aviso")
            else -> notification.type == filterKey // Fallback
        }
    }

    fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
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

                // Reaplica o filtro atual automaticamente quando chegam novos dados
                val currentFilter = uiState.value.selectedFilter
                val filteredList = list.filter { matchesFilter(it, currentFilter) }

                uiState.value = uiState.value.copy(
                    allNotifications = list,
                    notifications = filteredList,
                    unreadCount = naoLidas,
                    isLoading = false,
                    error = null
                )
            }
    }

    fun filterByType(filterKey: String?) {
        val masterList = uiState.value.allNotifications
        val newList = masterList.filter { matchesFilter(it, filterKey) }

        uiState.value = uiState.value.copy(selectedFilter = filterKey, notifications = newList)
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isEmpty()) return
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnFailureListener { Log.e("NotifVM", "Erro ao marcar como lida", it) }
    }

    // --- JUSTIFICAÇÕES & DETALHES ---

    fun checkDeliveryStatus(deliveryId: String, onResult: (String?) -> Unit) {
        if (deliveryId.isEmpty()) {
            onResult(null)
            return
        }
        db.collection("delivery").document(deliveryId).get()
            .addOnSuccessListener { document ->
                val reason = document.getString("reason")
                if (!reason.isNullOrBlank()) {
                    onResult(reason)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
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
            .addOnFailureListener { onResult("Erro ao carregar", "--") }
    }

    fun handleJustificationDecision(notification: Notification, accepted: Boolean, onSuccess: () -> Unit) {
        val deliveryId = notification.relatedId
        val beneficiaryUserId = notification.senderId

        if (deliveryId.isEmpty()) return

        if (accepted) {
            justifyBeneficiary(deliveryId) {
                markAsRead(notification.docId)
                onSuccess()
            }
            return
        }

        if (beneficiaryUserId.isNotEmpty()) {
            applyFaultToBeneficiary(userId = beneficiaryUserId, deliveryId = deliveryId) {
                markAsRead(notification.docId)
                onSuccess()
            }
        } else {
            uiState.value = uiState.value.copy(error = "Erro: ID do beneficiário não encontrado.")
        }
    }

    private fun justifyBeneficiary(deliveryId: String, onComplete: () -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("delivery").document(deliveryId)
            .update(
                mapOf(
                    "reason" to "Justificado",
                    "state" to "CANCELADO"
                )
            )
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isLoading = false)
                onComplete()
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao justificar")
            }
    }

    private fun applyFaultToBeneficiary(userId: String, deliveryId: String, onComplete: () -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        val userRef = db.collection("users").document(userId)
        val deliveryRef = db.collection("delivery").document(deliveryId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentFaults = (userSnapshot.getLong("fault") ?: 0).toInt()

            // Incrementa faltas
            val newFaults = currentFaults + 1
            // Se tiver 2 ou mais faltas, perde estatuto de beneficiário (exemplo)
            val isBeneficiary = newFaults < 2

            transaction.update(userRef, "fault", newFaults)
            transaction.update(userRef, "isBeneficiary", isBeneficiary)

            transaction.update(deliveryRef, "state", "CANCELADO")
            transaction.update(deliveryRef, "reason", "Justificação Rejeitada (Falta Aplicada)")
            transaction.update(deliveryRef, "delivered", false)

        }.addOnSuccessListener {
            uiState.value = uiState.value.copy(isLoading = false)
            onComplete()
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${e.message}")
        }
    }
}