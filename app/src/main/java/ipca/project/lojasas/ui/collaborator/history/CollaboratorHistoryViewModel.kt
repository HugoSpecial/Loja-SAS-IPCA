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
import ipca.project.lojasas.models.Product // Certifica-te que tens este import
import java.util.Date

// --- NOVO MODELO PARA O PRODUTO NO HISTÓRICO ---
data class HistoryProductItem(
    val name: String,
    val quantity: Int,
    val imageUrl: String?
)

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
    // AGORA É UMA LISTA DE OBJETOS, NÃO DE STRINGS
    val productsList: List<HistoryProductItem> = emptyList()
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

    // Cache local dos produtos para ir buscar as imagens
    private var allProductsCache: List<Product> = emptyList()

    init {
        fetchAllProductsAndHistory()
    }

    // 1. Primeiro buscamos os produtos para ter as imagens
    private fun fetchAllProductsAndHistory() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("products").get()
            .addOnSuccessListener { result ->
                allProductsCache = result.toObjects(Product::class.java)
                // 2. Só depois buscamos o histórico
                fetchHistoryData()
            }
            .addOnFailureListener {
                // Se falhar produtos, tenta buscar histórico na mesma (sem imagens)
                fetchHistoryData()
            }
    }

    private fun fetchHistoryData() {
        db.collection("orders").get()
            .addOnSuccessListener { ordersResult ->
                val ordersList = mutableListOf<Order>()
                for (doc in ordersResult) {
                    try {
                        val order = doc.toObject(Order::class.java)
                        order.docId = doc.id
                        ordersList.add(order)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                val ordersMap = ordersList.associateBy { it.docId }

                db.collection("delivery").get() // Atenção: Confirma se a coleção é "delivery" ou "deliveries"
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
                            val date = linkedOrder?.orderDate ?: delivery.surveyDate ?: Date()
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

    // Função auxiliar para mapear items e encontrar a imagem
    private fun mapItemsToHistoryProducts(items: List<OrderItem>): List<HistoryProductItem> {
        return items.map { item ->
            // Procura o produto na cache para obter a imagem
            val productDetails = allProductsCache.find { it.name == item.name }
            HistoryProductItem(
                name = item.name ?: "?",
                quantity = item.quantity ?: 0,
                imageUrl = productDetails?.imageUrl
            )
        }
    }

    private fun mapOrderToUi(order: Order): HistoryUiItem {
        val totalQty = order.items.sumOf { it.quantity ?: 0 }

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
            productsList = mapItemsToHistoryProducts(order.items) // Usa a nova função
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
            productsList = mapItemsToHistoryProducts(items) // Usa a nova função
        )
    }
}