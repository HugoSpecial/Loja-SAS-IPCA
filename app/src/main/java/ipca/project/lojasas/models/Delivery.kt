package ipca.project.lojasas.models

enum class DeliveryState {
    PENDENTE,
    ENTREGUE,
    CANCELADO
}

data class Delivery (
    var docId : String? = null,
    var orderId: String? = null,
    var delivered : Boolean = false,
    var state : DeliveryState = DeliveryState.PENDENTE,
    var reason : String? = null
)
