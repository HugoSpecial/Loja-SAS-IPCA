package ipca.project.lojasas.models

import java.util.Date

data class Report(
    var docId: String? = null,

    // Dados do Relatório
    var title: String? = null,
    var month: Int? = 0,
    var year: Int? = 0,
    var totalOrders: Int? = 0,

    // Metadados
    var generatedAt: Date? = null,
    var generatedBy: String? = null, // ID do user ou "SISTEMA"
    var type: String? = null,        // ex: "local_pdf_app" ou "auto_backup"
    var status: String? = null,      // ex: "created"

    // Ficheiro (Compatibilidade Híbrida)
    var fileBase64: String? = null,  // Usado quando a App gera o PDF
    var fileUrl: String? = null,     // Usado quando o Cloud Functions gera o PDF
    var storagePath: String? = null  // Caminho no Storage (se aplicável)
)