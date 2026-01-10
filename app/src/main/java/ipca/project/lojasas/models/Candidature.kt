package ipca.project.lojasas.models

import java.util.Date

enum class CandidatureState {
    PENDENTE,
    ACEITE,
    REJEITADA,
}

enum class Type {
    LICENCIATURA,
    MESTRADO,
    CTESP,
    PROFESSOR,
    FUNCIONARIO,
    OUTRO,
}

data class DocumentAttachment(
    var name: String = "",
    var base64: String = ""
)

data class Candidature(
    var docId: String = "",
    var creationDate: Date? = null,
    var updateDate: Date? = null,

    // --- DADOS PESSOAIS ---
    var academicYear: String = "",    // "anoLetivo"
    var birthDate: String = "",       // "dataNascimento"
    var mobilePhone: String = "",     // "telemovel"
    var email: String = "",

    // --- DADOS ACADÉMICOS ---
    var type: Type? = null,           // "tipo"
    var course: String? = null,       // "curso"
    var cardNumber: String = "",      // "numeroCartao"

    // --- PRODUTOS ---
    var foodProducts: Boolean = false,    // "produtosAlimentares"
    var hygieneProducts: Boolean = false, // "produtosHigiene"
    var cleaningProducts: Boolean = false,// "produtosLimpeza"

    // --- APOIOS ---
    var faesSupport: Boolean? = null,         // "faesApoiado"
    var scholarshipSupport: Boolean? = null,  // "bolsaApoio"
    var scholarshipDetails: String = "",      // "detalhesBolsa"

    // --- DOCUMENTOS ---
    var attachments: MutableList<DocumentAttachment> = mutableListOf(), // "anexos"

    // --- FINALIZAÇÃO ---
    var truthfulnessDeclaration: Boolean = false, // "declaracaoVeracidade"
    var dataAuthorization: Boolean = false,       // "autorizacaoDados"
    var signatureDate: String = "",               // "dataAssinatura"
    var signature: String = "",                   // "assinatura"

    // --- SISTEMA / ESTADO ---
    var state: CandidatureState = CandidatureState.PENDENTE, // Corrigido de "estado"
    var statusChangeReason: String? = null,   // "motivoAlteracaoEstado"
    var evaluationDate: Date? = null,         // "dataAvaliacao"
    var evaluatedBy: String? = null,          // "avaliadoPor"
    var userId: String? = null
)