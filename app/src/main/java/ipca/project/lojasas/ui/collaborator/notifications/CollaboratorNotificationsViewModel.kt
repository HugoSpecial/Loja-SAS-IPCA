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
    val notifications: List<Notification> = emptyList(), // Lista visível (filtrada)
    val allNotifications: List<Notification> = emptyList(), // Cache de todas
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
        // Tenta carregar no início (pode falhar se o login não estiver pronto)
        fetchNotifications()
    }

    // --- CORREÇÃO: Função pública (sem 'private') ---
    fun fetchNotifications() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            uiState.value = uiState.value.copy(
                error = "Utilizador não autenticado",
                isLoading = false,
                notifications = emptyList()
            )
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        // Query: Todas as notificações do user, ordenadas por data
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = mutableListOf<Notification>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val n = doc.toObject(Notification::class.java)

                        // FILTRO MANUAL: Só aceitamos se for para COLABORADOR
                        if (n != null && n.targetProfile == "COLABORADOR") {
                            n.docId = doc.id
                            list.add(n)
                        }
                    } catch (e: Exception) {
                        Log.e("NotifVM", "Erro ao ler notificacao: ${doc.id}", e)
                    }
                }

                // Contar não lidas
                val naoLidas = list.count { !it.read }

                // Aplicar o filtro atual (se houver) aos novos dados
                val currentFilter = uiState.value.selectedFilter

                val filteredList = if (currentFilter == null) {
                    list
                } else {
                    list.filter { it.type == currentFilter }
                }

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

        val newList = if (type == null) {
            masterList
        } else {
            masterList.filter { it.type == type }
        }

        uiState.value = uiState.value.copy(
            selectedFilter = type,
            notifications = newList
        )
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnFailureListener {
                Log.e("NotifVM", "Erro ao marcar como lida", it)
            }
    }
}