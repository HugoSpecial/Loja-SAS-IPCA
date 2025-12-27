package ipca.project.lojasas.ui.colaborator.history

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import java.util.Date

// --- MODELO UI ATUALIZADO ---
data class HistoryUiItem(
    val id: String,
    val typeLabel: String,
    val typeColor: Color,
    val beneficiaryName: String,
    val infoText: String,
    val date: Date,
    val statusLabel: String,
    val statusBgColor: Color,
    val statusTextColor: Color,
    // NOVO CAMPO: Lista de strings descritivas (ex: "2x Arroz")
    val productsList: List<String> = emptyList()
)

data class HistoryState(
    val items: List<HistoryUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CollatorHistoryViewModel : ViewModel() {

    var uiState = mutableStateOf(HistoryState())
        private set

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchHistoryData()
    }

    private fun fetchHistoryData() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders").get()
            .addOnSuccessListener { ordersResult ->
                val ordersList = mutableListOf<Order>()
                // Leitura segura com try-catch para evitar crash se houver lixo na BD
                for (doc in ordersResult) {
                    try {
                        val order = doc.toObject(Order::class.java)
                        order.docId = doc.id
                        ordersList.add(order)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                val ordersMap = ordersList.associateBy { it.docId }

                db.collection("deliveries").get()
                    .addOnSuccessListener { deliveriesResult ->
                        val deliveriesList = deliveriesResult.toObjects(Delivery::class.java)
                        deliveriesList.forEachIndexed { i, d -> d.docId = deliveriesResult.documents[i].id }

                        val combinedHistory = mutableListOf<HistoryUiItem>()

                        // A) Pedidos
                        ordersList.forEach { order ->
                            combinedHistory.add(mapOrderToUi(order))
                        }

                        // B) Levantamentos
                        deliveriesList.forEach { delivery ->
                            val linkedOrder = ordersMap[delivery.orderId]
                            val beneficiaryName = linkedOrder?.userName ?: "Desconhecido"
                            val date = linkedOrder?.orderDate ?: Date()
                            // Passamos também os items do pedido original
                            val items = linkedOrder?.items ?: mutableListOf()

                            combinedHistory.add(mapDeliveryToUi(delivery, beneficiaryName, date, items))
                        }

                        uiState.value = uiState.value.copy(
                            items = combinedHistory.sortedByDescending { it.date },
                            isLoading = false,
                            error = null
                        )
                    }
                    .addOnFailureListener {
                        uiState.value = uiState.value.copy(isLoading = false, error = it.message)
                    }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
    }

    private fun mapOrderToUi(order: Order): HistoryUiItem {
        val totalQty = order.items.sumOf { it.quantity ?: 0 }

        // Converter lista de objetos OrderItem em lista de Strings "Qtd x Nome"
        val productsDesc = order.items.map { "${it.quantity}x ${it.name}" }

        val (label, bg, text) = when (order.accept) {
            OrderState.ACEITE -> Triple("ACEITE", Color(0xFFB9F6CA), Color(0xFF00C853))
            OrderState.REJEITADA -> Triple("REJEITADA", Color(0xFFFFCDD2), Color(0xFFD32F2F))
            OrderState.PENDENTE -> Triple("PENDENTE", Color(0xFFFFF9C4), Color(0xFFFBC02D))
        }

        return HistoryUiItem(
            id = order.docId ?: "",
            typeLabel = "Pedido",
            typeColor = Color(0xFF00864F),
            beneficiaryName = order.userName ?: "Sem Nome",
            infoText = "$totalQty produtos pedidos",
            date = order.orderDate ?: Date(),
            statusLabel = label,
            statusBgColor = bg,
            statusTextColor = text,
            productsList = productsDesc // <--- Preenchido aqui
        )
    }

    private fun mapDeliveryToUi(delivery: Delivery, name: String, date: Date, items: List<OrderItem>): HistoryUiItem {
        val (label, bg, text) = when (delivery.state) {
            DeliveryState.ENTREGUE -> Triple("ENTREGUE", Color(0xFFB9F6CA), Color(0xFF00C853))
            DeliveryState.CANCELADO -> Triple("NÃO ENTREGUE", Color(0xFFFFCDD2), Color(0xFFD32F2F))
            DeliveryState.PENDENTE -> Triple("PENDENTE", Color(0xFFFFF9C4), Color(0xFFFBC02D))
            DeliveryState.EM_ANALISE -> Triple("EM ANÁLISE", Color(0xFFCDDC39), Color(0xFFFFC107))
        }

        val totalQty = items.sumOf { it.quantity ?: 0 }
        val productsDesc = items.map { "${it.quantity}x ${it.name}" }

        return HistoryUiItem(
            id = delivery.docId ?: "",
            typeLabel = "Levantamento",
            typeColor = Color(0xFF2E7D32),
            beneficiaryName = name,
            infoText = "$totalQty produtos para levantar",
            date = date,
            statusLabel = label,
            statusBgColor = bg,
            statusTextColor = text,
            productsList = productsDesc // <--- Preenchido aqui
        )
    }
}