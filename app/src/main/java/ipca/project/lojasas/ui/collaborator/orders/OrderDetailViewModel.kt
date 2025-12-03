package ipca.project.lojasas.ui.collaborator.orders

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.ProductTest
import java.util.Date

data class OrderDetailState(
    val order: Order? = null,
    val userName: String? = null,
    val userPhone: String? = null,
    val userNotes: String? = null,
    val evaluatorName: String? = null,
    val products: List<ProductTest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val operationSuccess: Boolean = false,
)

class OrderDetailViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(OrderDetailState())
        private set

    /** Buscar detalhes do pedido */
    fun fetchOrder(docId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders").document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val order = Order()

                        order.docId = snapshot.id
                        order.orderDate = snapshot.getDate("orderDate")
                        order.surveyDate = snapshot.getDate("surveyDate")
                        order.userId = snapshot.getString("userId")

                        // Estado
                        order.accept = runCatching {
                            OrderState.valueOf(snapshot.getString("accept") ?: "PENDENTE")
                        }.getOrDefault(OrderState.PENDENTE)

                        order.evaluatedBy = snapshot.getString("evaluatedBy")
                        order.evaluationDate = snapshot.getDate("evaluationDate")

                        order.rejectReason = snapshot.getString("rejectReason")

                        // Itens
                        order.items = (snapshot.get("items") as? List<Map<String, Any>>)
                            ?.mapNotNull { map ->
                                val name = map["name"] as? String
                                val qty = (map["quantity"] as? Long)?.toInt()
                                if (name != null && qty != null) OrderItem(name, qty) else null
                            }
                            ?.toMutableList() ?: mutableListOf()

                        uiState.value = uiState.value.copy(
                            order = order,
                            isLoading = false,
                            error = null
                        )

                        // Buscar dados do beneficiário
                        order.userId?.let { fetchUser(it, isEvaluator = false) }

                        // Buscar dados do colaborador
                        order.evaluatedBy?.let { fetchUser(it, isEvaluator = true) }

                        fetchProducts()

                    } catch (e: Exception) {
                        Log.e("OrderDetailVM", "Erro no parse", e)
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao ler dados: ${e.message}"
                        )
                    }
                } else {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Pedido não encontrado."
                    )
                }
            }
    }

    /** Buscar info de um user (beneficiário ou colaborador) */
    private fun fetchUser(userId: String, isEvaluator: Boolean) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {

                    val name = snapshot.getString("name") ?: ""
                    val phone = snapshot.getString("phone") ?: ""
                    val notes = snapshot.getString("preferences") ?: ""

                    uiState.value =
                        if (isEvaluator) {
                            uiState.value.copy(
                                evaluatorName = name
                            )
                        } else {
                            uiState.value.copy(
                                userName = name,
                                userPhone = phone,
                                userNotes = notes
                            )
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OrderDetailVM", "Erro ao buscar o utilizador: $userId", e)
            }
    }

    fun fetchProducts() {
        db.collection("products")
            .addSnapshotListener { snapshot, _ ->

                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ProductTest::class.java)
                } ?: emptyList()

                uiState.value = uiState.value.copy(
                    products = list
                )
            }
    }


    /** Aprovar pedido */
    fun approveOrder(orderId: String) {
        val currentOrder = uiState.value.order
        if (currentOrder?.accept != OrderState.PENDENTE) {
            uiState.value = uiState.value.copy(error = "Pedido já finalizado.")
            return
        }

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders").document(orderId)
            .update(
                mapOf(
                    "accept" to OrderState.ACEITE.name,
                    "evaluationDate" to now,
                    "evaluatedBy" to collaboratorId
                )
            )
            .addOnSuccessListener {
                currentOrder.items?.let { updateStock(it) }

                createDelivery(orderId)

                uiState.value = uiState.value.copy(
                    order = currentOrder.copy(
                        accept = OrderState.ACEITE,
                        evaluatedBy = collaboratorId,
                        evaluationDate = now
                    ),
                    operationSuccess = true,
                    isLoading = false
                )

                fetchUser(collaboratorId, isEvaluator = true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    /** Rejeitar pedido */
    fun rejectOrder(orderId: String, reason: String) {
        val currentOrder = uiState.value.order
        if (currentOrder?.accept != OrderState.PENDENTE) {
            uiState.value = uiState.value.copy(error = "Pedido já finalizado.")
            return
        }

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        val updates = mapOf(
            "accept" to OrderState.REJEITADA.name,
            "rejectReason" to reason,
            "evaluationDate" to now,
            "evaluatedBy" to collaboratorId
        )

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(
                    order = currentOrder.copy(
                        accept = OrderState.REJEITADA,
                        evaluatedBy = collaboratorId,
                        evaluationDate = now,
                        rejectReason = reason,
                    ),
                    operationSuccess = true,
                    isLoading = false
                )

                fetchUser(collaboratorId, isEvaluator = true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    private fun updateStock(orderItems: List<OrderItem>) {
        orderItems.forEach { orderItem ->
            db.collection("products")
                .whereEqualTo("name", orderItem.name)
                .get()
                .addOnSuccessListener { snapshots ->
                    for (doc in snapshots.documents) {
                        val product = doc.toObject(ProductTest::class.java)
                        product?.let {
                            val updatedBatches = it.batches.map { batch ->
                                val newQty =
                                    (batch.quantity - orderItem.quantity!!).coerceAtLeast(0)
                                batch.copy(quantity = newQty)
                            }
                            doc.reference.update("batches", updatedBatches)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "OrderDetailVM",
                        "Erro ao atualizar stock do produto ${orderItem.name}",
                        e
                    )
                }
        }
    }

    private fun createDelivery(orderId: String) {
        val delivery = Delivery(
            docId = null,
            orderId,
            delivered = false,
            state = DeliveryState.PENDENTE,
            reason = null
        )
        db.collection("deliveries").add(delivery)
            .addOnSuccessListener { docRef ->
                docRef.update("docId", docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e("OrderDetailVM", "Erro ao criar entrega para o pedido $orderId", e)
            }
    }
}
