package ipca.example.lojasas.models

import java.util.Date

data class Utilizador (
    var docId : String? = null,
    var nome : String? = null,
    var data_nasc : Date? = null,
    var alergia : List<String>? = emptyList(),
    var tipo : String? = null,
    var aprovado : Boolean = false, // Interessado/Beneficiário aprovado
    var falta : Int = 0,
    var imagem_perfil : String? = null
)
// --- Tabela Pedido ---