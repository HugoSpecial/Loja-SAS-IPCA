package ipca.project.lojasas.ui.collaborator.orders

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.*
import java.util.Date

data class OrderDetailState(
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
                        order.accept = runCatching {
                            OrderState.valueOf(snapshot.getString("accept") ?: "PENDENTE")
                        }.getOrDefault(OrderState.PENDENTE)
                        order.evaluatedBy = snapshot.getString("evaluatedBy")
                        order.evaluationDate = snapshot.getDate("evaluationDate")
                        order.rejectReason = snapshot.getString("rejectReason")

                        order.items = (snapshot.get("items") as? List<Map<String, Any>>)
                            ?.mapNotNull { map ->
                                val name = map["name"] as? String
                                val qty = (map["quantity"] as? Long)?.toInt()
                                if (name != null && qty != null) OrderItem(name, qty) else null
                            }?.toMutableList() ?: mutableListOf()

                        uiState.value = uiState.value.copy(
                            order = order,
                            isLoading = false,
                            error = null
                        )

                        order.userId?.let { fetchUser(it, isEvaluator = false) }
                        order.evaluatedBy?.let { fetchUser(it, isEvaluator = true) }
                        fetchProducts()

                    } catch (e: Exception) {
                        Log.e("OrderDetailVM", "Erro no parse", e)
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao ler dados.")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Pedido não encontrado.")
                }
            }
    }

    private fun fetchUser(userId: String, isEvaluator: Boolean) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: ""
                    val phone = snapshot.getString("phone") ?: ""
                    val notes = snapshot.getString("preferences") ?: ""

                    uiState.value = if (isEvaluator) {
                        uiState.value.copy(evaluatorName = name)
                    } else {
                        uiState.value.copy(userName = name, userPhone = phone, userNotes = notes)
                    }
                }
            }
    }

    fun fetchProducts() {
        db.collection("products").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject(Product::class.java) } ?: emptyList()
            uiState.value = uiState.value.copy(products = list)
        }
    }

    /** * APROVAR PEDIDO
     * AGORA: Retira o stock (FIFO) -> Atualiza Estado -> Cria Entrega
     */
    fun approveOrder(orderId: String) {
        val currentOrder = uiState.value.order ?: return
        if (currentOrder.accept != OrderState.PENDENTE) {
            uiState.value = uiState.value.copy(error = "Pedido já finalizado.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        // 1. PRIMEIRO: Retirar produtos do stock (FIFO)
        removeStockFIFO(currentOrder.items) { success ->
            if (success) {
                // 2. SE SUCESSO: Atualizar estado e criar entrega
                finalizeApproval(orderId, currentOrder)
            } else {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao atualizar stock. Verifique quantidades.")
            }
        }
    }

    private fun finalizeApproval(orderId: String, currentOrder: Order) {
        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        db.collection("orders").document(orderId)
            .update(
                mapOf(
                    "accept" to OrderState.ACEITE.name,
                    "evaluationDate" to now,
                    "evaluatedBy" to collaboratorId
                )
            )
            .addOnSuccessListener {
                db.collection("delivery").document(orderId)
                    .set(
                        Delivery(
                            orderId = orderId,
                            delivered = false,
                            state = DeliveryState.PENDENTE,
                            reason = null,
                            surveyDate = currentOrder.surveyDate,
                            evaluatedBy = null,
                            evaluationDate = null
                        )
                    )
                    .addOnSuccessListener {
                        fetchOrder(orderId)
                        uiState.value = uiState.value.copy(
                            order = currentOrder.copy(accept = OrderState.ACEITE),
                            operationSuccess = true,
                            isLoading = false
                        )
                        fetchUser(collaboratorId, isEvaluator = true)
                    }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    /** * REJEITAR PEDIDO
     * AGORA: Apenas muda o estado. NÃO devolve stock (porque ainda não foi tirado).
     */
    fun rejectOrProposeDate(orderId: String, reason: String, proposedDate: Date?) {
        val currentOrder = uiState.value.order ?: return
        if (currentOrder.accept != OrderState.PENDENTE) {
            uiState.value = uiState.value.copy(error = "Pedido já finalizado.")
            return
        }

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        if (proposedDate != null) {
            // Proposta de Data
            val proposal = ProposalDelivery(
                docId = null, confirmed = false, newDate = proposedDate,
                proposedBy = collaboratorId, proposalDate = now
            )
            db.collection("orders").document(orderId).collection("proposals").add(proposal)
                .addOnSuccessListener {
                    uiState.value = uiState.value.copy(operationSuccess = true, isLoading = false)
                }
        } else {
            // Rejeição
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
                    // NOTA: Removido o returnStock() daqui
                    Log.d("OrderVM", "Pedido REJEITADO. Stock mantido (não foi retirado antes).")

                    uiState.value = uiState.value.copy(
                        order = currentOrder.copy(accept = OrderState.REJEITADA),
                        operationSuccess = true,
                        isLoading = false
                    )
                    fetchUser(collaboratorId, isEvaluator = true)
                }
                .addOnFailureListener { e ->
                    uiState.value = uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    // --- NOVA FUNÇÃO: REMOVER DO STOCK (FIFO) ---
    private fun removeStockFIFO(items: List<OrderItem>, onComplete: (Boolean) -> Unit) {
        if (items.isEmpty()) {
            onComplete(true)
            return
        }

        var processedCount = 0
        var hasError = false

        for (item in items) {
            val name = item.name ?: continue
            val qtyRequired = item.quantity ?: 0

            if (qtyRequired <= 0) {
                processedCount++
                if (processedCount == items.size) onComplete(!hasError)
                continue
            }

            // Encontrar produto pelo nome
            db.collection("products").whereEqualTo("name", name).limit(1).get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        val productRef = snap.documents[0].reference

                        db.runTransaction { transaction ->
                            val snapshotProduct = transaction.get(productRef)
                            val product = snapshotProduct.toObject(Product::class.java)

                            if (product != null) {
                                // Ordenar por validade (Mais antigo primeiro)
                                val sortedBatches = product.batches.sortedBy { it.validity }.toMutableList()
                                var remaining = qtyRequired

                                val iterator = sortedBatches.listIterator()
                                while (iterator.hasNext() && remaining > 0) {
                                    val batch = iterator.next()
                                    if (batch.quantity <= remaining) {
                                        remaining -= batch.quantity
                                        iterator.remove() // Remove lote vazio
                                    } else {
                                        batch.quantity -= remaining
                                        remaining = 0
                                    }
                                }
                                transaction.update(productRef, "batches", sortedBatches)
                            }
                        }.addOnCompleteListener {
                            processedCount++
                            if (processedCount == items.size) onComplete(!hasError)
                        }.addOnFailureListener {
                            hasError = true
                            processedCount++
                            if (processedCount == items.size) onComplete(false)
                        }
                    } else {
                        // Produto não encontrado
                        processedCount++
                        if (processedCount == items.size) onComplete(!hasError)
                    }
                }
        }
    }
}