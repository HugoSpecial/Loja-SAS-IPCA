package ipca.project.lojasas.models

import java.util.Date

data class ProposalDelivery (
    var docId : String? = null,
    var confirmed : Boolean = false,
    var newDate : Date? = null,
    var proposedBy: String? = null,
    var proposalDate: Date? = null
)