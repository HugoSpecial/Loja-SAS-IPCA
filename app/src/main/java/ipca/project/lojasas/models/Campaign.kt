package ipca.project.lojasas.models

import java.util.Date

enum class CampaignType {
    INTERNO,
    EXTERNO
}

data class Campaign(
    var docId: String = "",              // Adicionado = ""
    var collaboradorId: String = "",     // Adicionado = ""
    var name: String = "",               // Adicionado = ""
    var startDate: Date = Date(),        // Adicionado = Date()
    var endDate: Date = Date(),          // Adicionado = Date()
    var campaignType: CampaignType = CampaignType.INTERNO, // Adicionado valor padrão
    var donations: MutableList<Donation> = mutableListOf()
)
