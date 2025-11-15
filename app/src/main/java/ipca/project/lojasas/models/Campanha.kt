package ipca.example.lojasas.models

import java.util.Date

data class Campanha (
    var docId : String? = null,
    var nome : String? = null,
    var data_inicio : Date? = null,
    var data_fim : Date? = null,
    var tipo_campanha : String? = null,
)
// --- Tabela Doacao ---