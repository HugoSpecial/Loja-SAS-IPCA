package ipca.example.lojasas.models

import java.util.Date

enum class EstadoCandidatura {
    PENDENTE,
    EM_ANALISE,
    ACEITE,
    REJEITADA
}

enum class Tipo {
    LICENCIATURA,
    MESTRADO,
    CTESP,
    PROFESSOR,
    FUNCIONARIO,
    OUTRO
}

data class DocumentoAnexo(
    var nome: String = "",
    var base64: String = ""
)

data class Candidatura (
    var docId: String = "",
    var dataCriacao: Date? = null,
    var dataAtualizacao: Date? = null,

    // --- DADOS PESSOAIS ---
    var anoLetivo: String = "",
    var dataNascimento: String = "",
    var telemovel: String = "",

    var email: String = "",

    // --- DADOS ACADÉMICOS ---
    var tipo: Tipo? = null,
    var curso: String? = null,
    var numeroCartao: String = "",

    // --- PRODUTOS ---
    var produtosAlimentares: Boolean = false,
    var produtosHigiene: Boolean = false,
    var produtosLimpeza: Boolean = false,

    // --- APOIOS ---
    var faesApoiado: Boolean? = null,
    var bolsaApoio: Boolean? = null,
    var detalhesBolsa: String = "",

    // --- DOCUMENTOS ---
    var anexos: MutableList<DocumentoAnexo> = mutableListOf(),

    // --- FINALIZAÇÃO ---
    var declaracaoVeracidade: Boolean = false,
    var autorizacaoDados: Boolean = false,
    var dataAssinatura: String = "",
    var assinatura: String = "",

    // --- ESTADO ---
    var estado: EstadoCandidatura = EstadoCandidatura.PENDENTE,
    var motivoAlteracaoEstado: String? = null,
    var dataAvaliacao: Date? = null,
    var avaliadoPor: String? = null,
    var userId: String? = null
)