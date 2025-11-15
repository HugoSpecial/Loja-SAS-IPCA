package ipca.example.lojasas.models

import java.util.Date

data class Pedido (
    var docId : String? = null,
    var data_pedido : Date? = null,
    var data_levantamento : Date? = null,
    var aceite : Boolean = false
)