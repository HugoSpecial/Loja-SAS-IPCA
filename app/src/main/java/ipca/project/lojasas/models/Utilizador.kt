package ipca.project.lojasas.models

import java.util.Date

data class Utilizador (
    var docId : String? = null,
    var name : String? = null,
    var preferences : String? = null,
    var isBeneficiary : Boolean = false,
    var phone : String? = null,
    var email : String? = null,
    var falta : Int = 0,
    var candidatureId : String? = null
)
