package ipca.example.lojasas.models

import java.util.Date

data class PropostaEntrega (
    var docId : String? = null,
    var confirmado : Boolean = false,
    var nova_data : Date? = null
)
// --- Tabela Pedido --->
// --- Tabela Entrega --->