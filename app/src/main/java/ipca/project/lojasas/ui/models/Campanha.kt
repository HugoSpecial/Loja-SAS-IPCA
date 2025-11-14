package ipca.example.lojasas.models

data class Campanha (
    var docId : String? = null,
    var nome : String? = null,
    var data_inicio : Date? = null,
    var data_fim : Date? = null,
    var tipo_campanha : String? = null,
)
// --- Tabela Doacao ---