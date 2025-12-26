package ipca.project.lojasas.models

import java.util.Date

data class Notification(
    var docId: String = "",
    var title: String = "",
    var body: String = "",
    var date: Date? = null,
    var read: Boolean = false,
    var type: String = "",
    var relatedId: String = "",

    // Define para quem é a notificação: "COLABORADOR", "BENEFICIARIO", "INTERESSADO"
    var targetProfile: String = ""
)