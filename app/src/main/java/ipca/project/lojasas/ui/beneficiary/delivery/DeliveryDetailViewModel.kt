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

    // Busca os dados da notificação (Título e Corpo)
    fun fetchNotificationData(notificationId: String) {
        if (notificationId.isEmpty()) return

        db.collection("notifications").document(notificationId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val title = document.getString("title") ?: ""
                    val body = document.getString("body") ?: ""

                    uiState = uiState.copy(
                        notificationTitle = title,
                        notificationBody = body
                    )
                }
            }
            .addOnFailureListener {
                uiState = uiState.copy(error = "Erro ao carregar notificação")
            }
    }

    fun onUserNoteChange(newText: String) {
        uiState = uiState.copy(userNote = newText)
    }

    // --- FUNÇÃO CORRIGIDA ---
    fun saveDeliveryNote(idPassed: String?) {
        if (idPassed == null) return

        uiState = uiState.copy(isLoading = true, error = null)

        // 1. Primeiro, tentamos ver se o ID passado é um "orderId" associado a uma entrega
        db.collection("deliveries")
            .whereEqualTo("orderId", idPassed)
            .get()
            .addOnSuccessListener { documents ->

                if (!documents.isEmpty) {
                    // CASO A: Encontrámos a entrega através do Order ID!
                    // Vamos atualizar a primeira entrega encontrada (geralmente a mais recente)
                    val deliveryDoc = documents.documents[0]
                    updateDeliveryDocument(deliveryDoc.id)
                } else {
                    // CASO B: Não encontrámos pelo Order ID.
                    // Assumimos que o ID passado é o ID direto da entrega.
                    updateDeliveryDocument(idPassed)
                }
            }
            .addOnFailureListener { e ->
                uiState = uiState.copy(isLoading = false, error = e.message)
            }
    }

    // Função auxiliar para fazer o update final
    private fun updateDeliveryDocument(docId: String) {
        db.collection("deliveries").document(docId)
            .update("beneficiaryNote", uiState.userNote)
            .addOnSuccessListener {
                uiState = uiState.copy(isLoading = false, isSaved = true)
            }
            .addOnFailureListener { e ->
                // Aqui apanhamos o erro NOT_FOUND se mesmo assim não existir
                val msg = if (e.message?.contains("NOT_FOUND") == true) "Entrega não encontrada." else e.message
                uiState = uiState.copy(isLoading = false, error = msg)
            }
    }
}