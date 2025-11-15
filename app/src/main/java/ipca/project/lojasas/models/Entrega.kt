package ipca.example.lojasas.models

data class Entrega (
    var docId : String? = null,
    var entregue : Boolean = false,
    var estado : String? = null,
    var motivo : String? = null
)
// --- Tabela PropostaEntrega ---