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

    // --- NOVA FUNÇÃO: Trata da decisão de Justificação ---
    fun handleJustificationDecision(notification: Notification, accepted: Boolean, onSuccess: () -> Unit) {
        val deliveryId = notification.relatedId
        if (deliveryId.isEmpty()) return

        // Define o novo estado da entrega
        val newState = if (accepted) "JUSTIFICADO" else "FALTA_INJUSTIFICADA"

        // 1. Atualiza a entrega na coleção 'delivery'
        db.collection("delivery").document(deliveryId)
            .update("state", newState)
            .addOnSuccessListener {
                // 2. Marca a notificação como lida/resolvida
                markAsRead(notification.docId)
                onSuccess()
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(error = "Erro ao atualizar entrega.")
            }
    }
}