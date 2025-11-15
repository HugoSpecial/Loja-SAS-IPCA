package ipca.example.lojasas.models

import java.util.Date

enum class EstadoCandidatura {
    PENDENTE,
    EM_ANALISE,
    ACEITE,
    REJEITADA
}

enum class TipoCurso {
    LICENCIATURA,
    MESTRADO,
    CTESP
}

data class Candidatura (
    var docId: String? = null,
    var dataCriacao: Date? = null,
    var dataAtualizacao: Date? = null,
    var anoLetivo: String = "",
    var dataNascimento: String = "",
    var telemovel: String = "",
    var email: String = "",
    var tipoCurso: TipoCurso? = null,
    var curso: String = "",
    var numeroEstudante: String = "",
    var produtosAlimentares: Boolean = false,
    var produtosHigiene: Boolean = false,
    var produtosLimpeza: Boolean = false,
    var faesApoiado: Boolean? = null,
    var bolsaApoio: Boolean? = null,
    var detalhesBolsa: String = "",
    var declaracaoVeracidade: Boolean = false,
    var autorizacaoDados: Boolean = false,
    var dataAssinatura: String = "",
    var assinatura: String = "",
    var estado: EstadoCandidatura = EstadoCandidatura.PENDENTE,
    var motivoAlteracaoEstado: String? = null,
    var dataAvaliacao: Date? = null,
    var avaliadoPor: String? = null,
    var userId: String? = null
)
