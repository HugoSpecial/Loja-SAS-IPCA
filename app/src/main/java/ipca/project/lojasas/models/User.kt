package ipca.project.lojasas.models

data class User (
    var docId : String? = null,
    var name : String? = null,
    var preferences : String? = null,
    var isBeneficiary : Boolean = false,
    var isCollaborator : Boolean = false,
    var phone : String? = null,
    var email : String? = null,
    var fault : Int = 0,
    var candidatureId : String? = null,
    var fcmToken : String? = null
)
