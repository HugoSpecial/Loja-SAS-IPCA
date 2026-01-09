package ipca.project.lojasas.ui.collaborator.orders

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
    val proposals: List<ProposalDelivery> = emptyList(),
    val currentCollaboratorName: String? = null
)

class OrderDetailViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var proposalsListener: ListenerRegistration? = null

    var uiState = mutableStateOf(OrderDetailState())
        private set

    /** Buscar detalhes do pedido */
    fun fetchOrder(docId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        fetchCurrentCollaboratorName()

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

                        // Parse seguro do Estado
                        val acceptStr = snapshot.getString("accept") ?: "PENDENTE"
                        order.accept = try {
                            OrderState.valueOf(acceptStr)
                        } catch (e: Exception) {
                            OrderState.PENDENTE
                        }

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
                        listenToProposals(docId)

                    } catch (e: Exception) {
                        Log.e("OrderDetailVM", "Erro no parse", e)
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao ler dados.")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Pedido não encontrado.")
                }
            }
    }

    private fun fetchCurrentCollaboratorName() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name")
                    uiState.value = uiState.value.copy(currentCollaboratorName = name)
                }
            }
    }

    private fun listenToProposals(orderId: String) {
        proposalsListener?.remove()
        proposalsListener = db.collection("orders").document(orderId)
            .collection("proposals")
            .orderBy("proposalDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ProposalDelivery::class.java)?.apply {
                            docId = doc.id
                        }
                    }
                    uiState.value = uiState.value.copy(proposals = list)
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

    // --- APROVAR PEDIDO (CORRIGIDO) ---
    fun approveOrder(orderId: String) {
        val currentOrder = uiState.value.order ?: return

        // Validação de segurança
        if (orderId.isBlank()) {
            uiState.value = uiState.value.copy(error = "Erro: ID do pedido inválido.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        // 1. Abater stock
        removeStockFIFO(currentOrder.items) { success ->
            if (success) {
                finalizeApproval(orderId, currentOrder)
            } else {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao atualizar stock. Verifique validades.")
            }
        }
    }

    private fun finalizeApproval(orderId: String, currentOrder: Order) {
        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        // Garante que usamos o nome exato do ENUM
        val updates = mapOf(
            "accept" to OrderState.ACEITE.name,
            "evaluationDate" to now,
            "evaluatedBy" to collaboratorId
        )

        db.collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("OrderVM", "3. Pedido marcado como ACEITE. A criar entrega...")

                // Criação do objeto Delivery
                val delivery = Delivery(
                    orderId = orderId,
                    delivered = false,
                    state = DeliveryState.PENDENTE,
                    reason = null,
                    surveyDate = currentOrder.surveyDate,
                    evaluatedBy = null,
                    evaluationDate = null
                )

                db.collection("delivery").document(orderId)
                    .set(delivery)
                    .addOnSuccessListener {
                        Log.d("OrderVM", "4. Entrega criada. SUCESSO TOTAL.")
                        // SÓ AQUI é que fechamos o ecrã
                        uiState.value = uiState.value.copy(
                            operationSuccess = true,
                            isLoading = false
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("OrderVM", "Erro ao criar entrega", e)
                        uiState.value = uiState.value.copy(isLoading = false, error = "Pedido aceite, mas falha na entrega: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("OrderVM", "Erro ao atualizar estado do pedido", e)
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao aprovar: ${e.message}")
            }
    }

    fun rejectOrProposeDate(orderId: String, reason: String, proposedDate: Date?) {
        val currentOrder = uiState.value.order ?: return
        if (currentOrder.accept != OrderState.PENDENTE) {
            uiState.value = uiState.value.copy(error = "Pedido já finalizado.")
            return
        }

        val collaboratorId = Firebase.auth.currentUser?.uid ?: ""
        val now = Date()

        if (proposedDate != null) {
            val proposal = ProposalDelivery(
                docId = null, confirmed = false, newDate = proposedDate,
                proposedBy = collaboratorId, proposalDate = now
            )
            db.collection("orders").document(orderId).collection("proposals").add(proposal)
                .addOnSuccessListener {
                    uiState.value = uiState.value.copy(isLoading = false)
                }
                .addOnFailureListener {
                    uiState.value = uiState.value.copy(isLoading = false, error = it.message)
                }
        } else {
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
                        operationSuccess = true,
                        isLoading = false
                    )
                }
                .addOnFailureListener { e ->
                    uiState.value = uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun acceptProposal(orderId: String, proposalId: String) {
        val proposal = uiState.value.proposals.find { it.docId == proposalId } ?: return
        val newDate = proposal.newDate ?: return

        val batch = db.batch()
        val orderRef = db.collection("orders").document(orderId)
        val proposalRef = orderRef.collection("proposals").document(proposalId)

        batch.update(orderRef, "surveyDate", newDate)
        batch.update(proposalRef, "confirmed", true)

        batch.commit()
            .addOnSuccessListener { Log.d("OrderVM", "Data aceite.") }
            .addOnFailureListener { e -> uiState.value = uiState.value.copy(error = "Erro: ${e.message}") }
    }

    private fun removeStockFIFO(items: List<OrderItem>, onComplete: (Boolean) -> Unit) {
        if (items.isEmpty()) {
            onComplete(true)
            return
        }

        var processedCount = 0
        var hasError = false
        val today = Date()

        for (item in items) {
            val name = item.name ?: continue
            val qtyRequired = item.quantity ?: 0

            if (qtyRequired <= 0) {
                processedCount++
                if (processedCount == items.size) onComplete(!hasError)
                continue
            }

            db.collection("products").whereEqualTo("name", name).limit(1).get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        val productRef = snap.documents[0].reference

                        db.runTransaction { transaction ->
                            val snapshotProduct = transaction.get(productRef)
                            val product = snapshotProduct.toObject(Product::class.java)

                            if (product != null) {
                                val sortedBatches = product.batches.sortedBy { it.validity }.toMutableList()
                                var remaining = qtyRequired

                                val iterator = sortedBatches.listIterator()

                                while (iterator.hasNext() && remaining > 0) {
                                    val batch = iterator.next()
                                    if (batch.validity != null && batch.validity!!.before(today)) continue

                                    if (batch.quantity <= remaining) {
                                        remaining -= batch.quantity
                                        iterator.remove()
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
                        processedCount++
                        if (processedCount == items.size) onComplete(!hasError)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        proposalsListener?.remove()
    }
}