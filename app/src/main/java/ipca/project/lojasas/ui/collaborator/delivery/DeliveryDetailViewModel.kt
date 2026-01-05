package ipca.project.lojasas.ui.collaborator.delivery

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.*
import java.util.Date

data class DeliveryDetailState(
    val delivery: Delivery? = null,
    val order: Order? = null,
    val userName: String? = null,
    val userPhone: String? = null,
    val userNotes: String? = null,
    val evaluatorName: String? = null,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val operationSuccess: Boolean = false,
)

class DeliveryDetailViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(DeliveryDetailState())
        private set

    fun fetchDelivery(deliveryId: String) {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        db.collection("delivery").document(deliveryId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        val delivery = Delivery(
                            docId = snapshot.id,
                            orderId = snapshot.getString("orderId"),
                            delivered = snapshot.getBoolean("delivered") ?: false,
                            state = runCatching {
                                DeliveryState.valueOf(snapshot.getString("state") ?: "PENDENTE")
                            }.getOrDefault(DeliveryState.PENDENTE),
                            reason = snapshot.getString("reason"),
                            surveyDate = snapshot.getDate("surveyDate"),
                            evaluatedBy = snapshot.getString("evaluatedBy"),
                            evaluationDate = snapshot.getDate("evaluationDate")
                        )

                        uiState.value = uiState.value.copy(delivery = delivery)

                        // Busca nome do avaliador se existir
                        delivery.evaluatedBy?.let { fetchUser(it, isEvaluator = true) }

                        delivery.orderId?.let { orderId ->
                            db.collection("orders").document(orderId)
                                .get()
                                .addOnSuccessListener { orderSnapshot ->
                                    val order = orderSnapshot.toObject(Order::class.java)?.apply { docId = orderSnapshot.id }

                                    // --- CORREÇÃO AQUI ---
                                    uiState.value = uiState.value.copy(
                                        order = order,
                                        userName = order?.userName,
                                        isLoading = false
                                    )

                                    // Depois buscamos os detalhes extra (Telefone, notas)
                                    order?.userId?.let { fetchUser(it, isEvaluator = false) }

                                    fetchProducts()
                                }
                                .addOnFailureListener { e ->
                                    uiState.value = uiState.value.copy(error = e.message, isLoading = false)
                                }
                        } ?: run {
                            uiState.value = uiState.value.copy(isLoading = false)
                        }

                    } catch (e: Exception) {
                        Log.e("DeliveryDetailVM", "Erro no parse", e)
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao ler dados: ${e.message}")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Entrega não encontrada.")
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    private fun fetchUser(userId: String?, isEvaluator: Boolean) {
        if (userId.isNullOrBlank()) return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.getString("name") ?: ""
                    val phone = snapshot.getString("phone") ?: ""
                    val notes = snapshot.getString("preferences") ?: ""

                    uiState.value = if (isEvaluator) {
                        uiState.value.copy(evaluatorName = name)
                    } else {
                        // Atualizamos o resto dos dados, mantendo o nome (ou atualizando se mudou)
                        uiState.value.copy(userName = name, userPhone = phone, userNotes = notes)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeliveryDetailVM", "Erro ao buscar o utilizador: $userId", e)
            }
    }

    fun fetchProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                uiState.value = uiState.value.copy(products = list)
            }
    }

    fun approveDelivery(deliveryId: String) {
        val currentDelivery = uiState.value.delivery ?: return
        if (currentDelivery.state != DeliveryState.PENDENTE) return

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("delivery").document(deliveryId)
            .update(
                mapOf(
                    "state" to DeliveryState.ENTREGUE.name,
                    "delivered" to true,
                    "evaluationDate" to now,
                    "evaluatedBy" to collaboratorId
                )
            )
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(
                    delivery = currentDelivery.copy(
                        state = DeliveryState.ENTREGUE,
                        delivered = true,
                        evaluationDate = now,
                        evaluatedBy = collaboratorId
                    ),
                    operationSuccess = true,
                    isLoading = false
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    fun rejectDelivery(deliveryId: String) {
        val currentDelivery = uiState.value.delivery ?: return
        if (currentDelivery.state != DeliveryState.PENDENTE) return

        val currentOrder = uiState.value.order

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        val updates = mapOf(
            "state" to DeliveryState.CANCELADO.name,
            "evaluationDate" to now,
            "evaluatedBy" to collaboratorId
        )

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("delivery").document(deliveryId)
            .update(updates)
            .addOnSuccessListener {

                val userIdSafe = currentOrder?.userId

                //Notificação
                if (!userIdSafe.isNullOrBlank()) {
                    val notification = Notification(
                        title = "Entrega não recolhida",
                        body = "A sua entrega foi cancelada porque não foi recolhida no horário estipulado.",
                        date = Date(),
                        read = false,
                        type = "entrega_rejeitada",
                        relatedId = deliveryId,
                        targetProfile = "BENEFICIARIO",
                        recipientId = userIdSafe
                    )

                    db.collection("notifications")
                        .add(notification)
                        .addOnFailureListener { e ->
                            Log.e("DeliveryDetailVM", "Erro ao enviar notificação", e)
                        }
                }

                uiState.value = uiState.value.copy(
                    delivery = currentDelivery.copy(
                        state = DeliveryState.CANCELADO,
                        evaluatedBy = collaboratorId,
                        evaluationDate = now
                    ),
                    operationSuccess = true,
                    isLoading = false
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }
}