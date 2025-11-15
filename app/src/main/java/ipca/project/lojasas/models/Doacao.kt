package ipca.example.lojasas.models

import java.util.Date

data class Doacao (
    var docId : String? = null,
    var anonimo : Boolean = false,
    var data_doacao : Date? = null,
    var quantidade : Int = 0
)
// --- Tabela Produto ---