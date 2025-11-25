package ipca.example.lojasas.models

import java.util.Date

data class Order (
    var docId : String? = null,
    var orderDate : Date? = null,
    var surveyDate : Date? = null,
    var accept : Boolean = false
)