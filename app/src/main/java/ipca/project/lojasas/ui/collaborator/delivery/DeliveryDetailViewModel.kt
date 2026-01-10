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
    val justificationStatus: String? = null
)

class DeliveryDetailViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(DeliveryDetailState())
        private set

    fun fetchDelivery(deliveryId: String) {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        db.collection("delivery").document(deliveryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val delivery = Delivery(
                            docId = snapshot.id,
                            orderId = snapshot.getString("orderId"),
                            delivered = snapshot.getBoolean("delivered") ?: false,
                            state = runCatching {
                                DeliveryState.valueOf(snapshot.getString("state") ?: "PENDENTE")
                            }.getOrDefault(DeliveryState.PENDENTE),
                            reason = snapshot.getString("reason"),
                            beneficiaryNote = snapshot.getString("beneficiaryNote"),
                            justificationStatus = snapshot.getString("justificationStatus"),
                            surveyDate = snapshot.getDate("surveyDate"),
                            evaluatedBy = snapshot.getString("evaluatedBy"),
                            evaluationDate = snapshot.getDate("evaluationDate")
                        )

                        uiState.value = uiState.value.copy(delivery = delivery)

                        delivery.evaluatedBy?.let { fetchUser(it, isEvaluator = true) }

                        delivery.orderId?.let { orderId ->
                            db.collection("orders").document(orderId).get()
                                .addOnSuccessListener { orderSnapshot ->
                                    val order = orderSnapshot.toObject(Order::class.java)?.apply { docId = orderSnapshot.id }
                                    uiState.value = uiState.value.copy(
                                        order = order,
                                        userName = order?.userName,
                                        isLoading = false
                                    )
                                    order?.userId?.let { fetchUser(it, isEvaluator = false) }
                                    fetchProducts()
                                }
                        } ?: run {
                            uiState.value = uiState.value.copy(isLoading = false)
                        }

                    } catch (e: Exception) {
                        Log.e("DeliveryDetailVM", "Erro no parse", e)
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Entrega não encontrada.")
                }
            }
    }

    // --- LÓGICA DE JUSTIFICAÇÃO ATUALIZADA (COM RESET) ---
    fun handleJustification(deliveryId: String, accept: Boolean) {
        val userId = uiState.value.order?.userId ?: return
        val deliveryRef = db.collection("delivery").document(deliveryId)
        val userRef = db.collection("users").document(userId)

        uiState.value = uiState.value.copy(isLoading = true)

        if (accept) {
            // Se ACEITE: Apenas atualiza a entrega
            deliveryRef.update("justificationStatus", "ACEITE")
                .addOnSuccessListener {
                    uiState.value = uiState.value.copy(isLoading = false, justificationStatus = "ACEITE")
                }
                .addOnFailureListener { e ->
                    uiState.value = uiState.value.copy(isLoading = false, error = e.message)
                }
        } else {
            // Se RECUSADA: Transação para ler faltas e bloquear se necessário
            db.runTransaction { transaction ->
                // 1. Ler dados atuais do utilizador
                val userSnapshot = transaction.get(userRef)
                val currentFaults = userSnapshot.getLong("fault")?.toInt() ?: 0

                // 2. Calcular novas faltas
                val newFaults = currentFaults + 1

                // 3. Lógica de atualização
                if (newFaults >= 2) {
                    // SE ATINGIU O LIMITE (2):
                    // - Revoga estatuto de beneficiário
                    // - Limpa as faltas (reseta para 0)
                    // - Limpa o ID da candidatura (para obrigar nova candidatura)
                    transaction.update(userRef, "isBeneficiary", false)
                    transaction.update(userRef, "fault", 0)
                    transaction.update(userRef, "candidatureId", "") // Limpa o ID
                } else {
                    // SE AINDA NÃO ATINGIU O LIMITE:
                    // - Apenas incrementa as faltas
                    transaction.update(userRef, "fault", newFaults)
                }

                // 4. Atualiza estado da entrega
                transaction.update(deliveryRef, "justificationStatus", "RECUSADA")
            }
                .addOnSuccessListener {
                    uiState.value = uiState.value.copy(isLoading = false, justificationStatus = "RECUSADA")
                }
                .addOnFailureListener { e ->
                    uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao atualizar faltas: ${e.message}")
                }
        }
    }

    private fun fetchUser(userId: String?, isEvaluator: Boolean) {
        if (userId.isNullOrBlank()) return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.getString("name")
                    val phone = snapshot.getString("phone")
                    val notes = snapshot.getString("preferences")

                    uiState.value = if (isEvaluator) {
                        uiState.value.copy(evaluatorName = name)
                    } else {
                        uiState.value.copy(userName = name, userPhone = phone, userNotes = notes)
                    }
                }
            }
    }

    fun fetchProducts() {
        db.collection("products").get().addOnSuccessListener { snapshot ->
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
            "evaluatedBy" to collaboratorId,
            "delivered" to false
        )

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("delivery").document(deliveryId)
            .update(updates)
            .addOnSuccessListener {

                currentDelivery.orderId?.let { orderId ->
                    returnStock(orderId)
                }

                // Notificação ao utilizador
                val userIdSafe = currentOrder?.userId
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
                    db.collection("notifications").add(notification)
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

    private fun returnStock(orderId: String) {
        db.collection("orders").document(orderId).get().addOnSuccessListener { snapshot ->
            val orderItemsRaw = snapshot.get("items") as? List<Map<String, Any>> ?: return@addOnSuccessListener

            for (itemMap in orderItemsRaw) {
                val name = itemMap["name"] as? String ?: continue
                val quantity = (itemMap["quantity"] as? Long)?.toInt() ?: 0

                if (quantity > 0) {
                    db.collection("products")
                        .whereEqualTo("name", name)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { productSnap ->
                            if (!productSnap.isEmpty) {
                                val productDoc = productSnap.documents[0]
                                val productRef = productDoc.reference

                                db.runTransaction { transaction ->
                                    val snapshotProduct = transaction.get(productRef)
                                    val batchesRaw = snapshotProduct.get("batches") as? List<Map<String, Any>>

                                    val batches = mutableListOf<ProductBatch>()
                                    if (batchesRaw != null) {
                                        for (b in batchesRaw) {
                                            val validDate = (b["validity"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                                            val qtd = (b["quantity"] as? Long)?.toInt() ?: 0
                                            batches.add(ProductBatch(validity = validDate, quantity = qtd))
                                        }
                                    }

                                    if (batches.isNotEmpty()) {
                                        val targetBatch = batches.maxByOrNull { it.validity?.time ?: 0L }
                                        if (targetBatch != null) {
                                            targetBatch.quantity += quantity
                                        }
                                    } else {
                                        batches.add(ProductBatch(validity = Date(), quantity = quantity))
                                    }

                                    transaction.update(productRef, "batches", batches)
                                }
                            }
                        }
                }
            }
        }
    }
}