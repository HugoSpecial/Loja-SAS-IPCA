package ipca.project.lojasas.models

import java.util.Date

data class Donation (
    var docId : String? = null,
    var anonymous : Boolean = false,
    var donationDate : Date? = null,
    var quantity : Int = 0
)
// --- Tabela Produto ---