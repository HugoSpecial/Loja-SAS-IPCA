package ipca.project.lojasas.models

import java.util.Date

data class Donation (
    var docId : String? = null,
    var anonymous : Boolean = false,
    var name : String? = null, // Nome do doador
    var donationDate : Date? = null,
    var quantity : Int = 0,
    var products : MutableList<Product> = mutableListOf(),
    var campaignId: String? = null // Novo: ID da campanha (opcional)
)