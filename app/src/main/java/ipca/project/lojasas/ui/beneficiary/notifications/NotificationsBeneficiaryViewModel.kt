package ipca.project.lojasas.ui.beneficiary.notifications

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Notification

data class BeneficiaryNotificationListState(
    val notifications: List<Notification> = emptyList(),
    val allNotifications: List<Notification> = emptyList(), // Guarda sempre a lista completa
    val unreadCount: Int = 0,
    val selectedFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsBeneficiaryViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(BeneficiaryNotificationListState())
        private set

    init {
        // Tenta carregar ao iniciar, mas pode falhar se o login não estiver pronto.
        // Por isso chamamos novamente na View.
        fetchNotifications()
    }

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

        // Usa o índice: recipientId ASC, date DESC
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

                        // Filtra apenas para o perfil BENEFICIARIO
                        if (n != null && n.targetProfile == "BENEFICIARIO") {
                            n.docId = doc.id
                            list.add(n)
                        }
                    } catch (e: Exception) {
                        Log.e("NotifBeneficiaryVM", "Erro ao ler notificação: ${doc.id}", e)
                    }
                }

                val naoLidas = list.count { !it.read }

                // Quando os dados chegam, reaplica o filtro que estiver selecionado
                val currentFilter = uiState.value.selectedFilter
                val filteredList = applyFilterLogic(list, currentFilter)

                uiState.value = uiState.value.copy(
                    allNotifications = list,
                    notifications = filteredList,
                    unreadCount = naoLidas,
                    isLoading = false,
                    error = null
                )
            }
    }

    // --- LÓGICA DE FILTRO ---
    fun filterByType(category: String?) {
        val masterList = uiState.value.allNotifications
        val newList = applyFilterLogic(masterList, category)

        uiState.value = uiState.value.copy(
            selectedFilter = category,
            notifications = newList
        )
    }

    private fun applyFilterLogic(list: List<Notification>, category: String?): List<Notification> {
        return when (category) {
            null -> list // Mostrar Tudo
            "cat_pedidos" -> list.filter {
                it.type == "pedido_novo" || it.type == "pedido_estado"
            }
            "cat_levantamentos" -> list.filter {
                it.title.contains("Levantamento", ignoreCase = true) || it.type == "pedido_agendado"
            }
            "cat_candidaturas" -> list.filter {
                it.type.startsWith("candidatura")
            }
            "cat_Entrega" -> list.filter {
                it.type.equals("entrega_rejeitada") || it.type.equals("entrega")
            }
            else -> list
        }
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnFailureListener {
                Log.e("NotifBeneficiaryVM", "Erro ao marcar como lida", it)
            }
    }
}