package ipca.example.lojasas.models

data class Doacao (
    var docId : String? = null,
    var anonimo : Bool = false,
    var data_doacao : Date? = null,
    var quantidade : Int = 0
)
// --- Tabela Produto ---