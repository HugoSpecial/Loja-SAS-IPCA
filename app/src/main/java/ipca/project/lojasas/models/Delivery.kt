package ipca.project.lojasas.models

data class Delivery (
    var docId : String? = null,
    var delivered : Boolean = false,
    var state : String? = null,
    var reason : String? = null
)
// --- Tabela PropostaEntrega ---