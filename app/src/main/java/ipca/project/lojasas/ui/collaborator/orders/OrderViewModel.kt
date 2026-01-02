package ipca.project.lojasas.ui.collaborator.orders

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState

data class OrderListState(
    val orders: List<Order> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(OrderListState())
        private set

    init {
        fetchOrders()
    }

    private fun fetchOrders() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = mutableListOf<Order>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val o = Order()
                        o.docId = doc.id

                        // Campos básicos
                        o.userId = doc.getString("userId") ?: "--"
                        o.userName = doc.getString("userName") ?: "--"

                        // Datas
                        o.orderDate = doc.getTimestamp("orderDate")?.toDate()
                        o.surveyDate = doc.getTimestamp("surveyDate")?.toDate()

                        // Estado
                        val stateStr = doc.getString("accept") ?: "PENDENTE"
                        try {
                            o.accept = OrderState.valueOf(stateStr)
                        } catch (e: Exception) {
                            o.accept = OrderState.PENDENTE
                        }

                        // --- NOVA PARTE: LER ITENS (ARRAY) ---
                        // O Firestore devolve arrays como List<HashMap>
                        val itemsRaw = doc.get("items") as? List<Map<String, Any>>

                        if (itemsRaw != null) {
                            for (itemMap in itemsRaw) {
                                val name = itemMap["name"] as? String
                                // No Firestore, números costumam vir como Long, por isso fazemos o cast seguro
                                val quantityLong = itemMap["quantity"] as? Long
                                val quantity = quantityLong?.toInt() ?: 0

                                val item = OrderItem(
                                    name = name,
                                    quantity = quantity
                                )
                                o.items.add(item)
                            }
                        }
                        // -------------------------------------

                        list.add(o)

                    } catch (e: Exception) {
                        Log.e("OrderVM", "Erro ao ler documento: ${doc.id}", e)
                    }
                }

                // Contagem de pendentes
                val pendentes = list.count { it.accept == OrderState.PENDENTE }

                // Ordenar: Pendentes primeiro, depois mais recentes
                val sortedList = list.sortedWith(
                    compareBy<Order> { it.accept != OrderState.PENDENTE }
                        .thenByDescending { it.orderDate }
                )

                uiState.value = uiState.value.copy(
                    orders = sortedList,
                    pendingCount = pendentes,
                    isLoading = false,
                    error = null
                )
            }
    }

    //Função em Teste (Pires Lindu)
    fun marcarFaltaEntrega(userId: String, deliveryId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        val userId = db.collection("users").document(userId)
        val deliveryId = db.collection("delivery").document(deliveryId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userId)

            val currentFaults = (userSnapshot.getLong("fault") ?: 0).toInt()

            val newFaults = currentFaults + 1

            val isBeneficiary = newFaults < 2

            // Atualizar User na BD
            transaction.update(userId, "fault", newFaults)
            transaction.update(userId, "isBeneficiary", isBeneficiary)

            // Atualizar Delivery para CANCELADO
            transaction.update(deliveryId, "state", DeliveryState.CANCELADO.name)
            transaction.update(deliveryId, "reason", "Falta de comparência")
            transaction.update(deliveryId, "delivered", false)

            /** }.addOnSuccessListener { (novasFaltas, aindaBeneficiario) ->
            uiState.value = uiState.value.copy(
            isLoading = false,
            operationSuccess = true
            )
            Log.d(
            "OrderDetailVM",
            "Falta marcada. Total: $novasFaltas. Beneficiário: $aindaBeneficiario"
            )

            }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(
            isLoading = false,
            error = "Erro ao marcar falta: ${e.message}"
            )
            Log.e("OrderDetailVM", "Erro na transação", e)*/
        }
    }
}