package ipca.project.lojasas.models

import java.util.Date

enum class DeliveryState {
    PENDENTE,
    ENTREGUE,
    CANCELADO,
    EM_ANALISE,
}

data class Delivery (
    var docId : String? = null,
    var orderId: String? = null,
    var delivered : Boolean = false,
    var state : DeliveryState = DeliveryState.PENDENTE,
    var reason : String? = null,
    var surveyDate : Date? = null,
    var evaluatedBy: String? = null,
    var evaluationDate: Date? = null,
    var beneficiaryNote: String? = null,
)
