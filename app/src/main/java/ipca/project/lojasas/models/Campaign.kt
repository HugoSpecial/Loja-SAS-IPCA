package ipca.project.lojasas.models

import java.util.Date

enum class CampaignType {
    INTERNO,
    EXTERNO
}

data class Campaign(
    var docId: String = "",
    var collaboradorId: String = "",
    var name: String = "",
    var startDate: Date = Date(),
    var endDate: Date = Date(),
    var campaignType: CampaignType = CampaignType.INTERNO,
    var donations: List<String> = emptyList()
)
