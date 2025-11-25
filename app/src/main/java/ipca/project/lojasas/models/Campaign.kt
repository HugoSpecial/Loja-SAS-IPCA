package ipca.project.lojasas.models

import java.util.Date

data class Campaign(
    var docId: String? = null,
    var name: String? = null,         // nome
    var startDate: Date? = null,      // data_inicio
    var endDate: Date? = null,        // data_fim
    var campaignType: String? = null  // tipo_campanha
)
// --- Tabela Doacao ---