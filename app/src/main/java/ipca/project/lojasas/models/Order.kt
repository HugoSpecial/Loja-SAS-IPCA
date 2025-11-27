package ipca.project.lojasas.models

import java.util.Date

data class OrderItem(
    val name: String? = null,
    val quantity: Int? = 0,
)

data class Order (
    var docId : String? = null,
    var orderDate : Date? = null,
    var surveyDate : Date? = null,
    var accept : Boolean = false,
    var items: MutableList<OrderItem> = mutableListOf() // List of items and quantities
)