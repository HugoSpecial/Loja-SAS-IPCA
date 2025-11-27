package ipca.project.lojasas.models

import java.util.Date

enum class OrderState {
    PENDENTE,
    ACEITE,
    REJEITADA,
}
data class OrderItem(
    val name: String? = null,
    val quantity: Int? = 0,
)

data class Order (
    var docId : String? = null,
    var orderDate : Date? = null,
    var surveyDate : Date? = null,
    var accept: OrderState = OrderState.PENDENTE,
    var items: MutableList<OrderItem> = mutableListOf(), // List of items and quantities
    var userId: String? = null,
)