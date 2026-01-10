package ipca.project.lojasas.ui.collaborator.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Report
import java.io.File
import java.io.FileOutputStream

data class ReportListState(
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReportsViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(ReportListState())
        private set

    init {
        fetchReports()
    }

    private fun fetchReports() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("reports")
            .orderBy("generatedAt", Query.Direction.DESCENDING) // Mais recentes primeiro
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = value?.documents?.mapNotNull { doc ->
                    try {
                        val r = Report()
                        r.docId = doc.id
                        r.title = doc.getString("title")
                        // Conversão segura de Long para Int (Firestore guarda números como Long)
                        r.month = doc.getLong("month")?.toInt()
                        r.year = doc.getLong("year")?.toInt()
                        r.totalOrders = doc.getLong("totalOrders")?.toInt()

                        r.generatedAt = doc.getTimestamp("generatedAt")?.toDate()
                        r.generatedBy = doc.getString("generatedBy")
                        r.type = doc.getString("type")

                        // Campos de Ficheiro (Híbrido: URL ou Base64)
                        r.fileUrl = doc.getString("fileUrl")
                        r.fileBase64 = doc.getString("fileBase64")

                        r
                    } catch (e: Exception) {
                        Log.e("ReportVM", "Erro ao converter documento ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                uiState.value = uiState.value.copy(reports = list, isLoading = false)
            }
    }

    // --- LÓGICA INTELIGENTE PARA ABRIR PDF ---
    fun openReport(context: Context, report: Report) {
        try {
            // CASO 1: É um Backup Automático ou URL externo
            if (!report.fileUrl.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(report.fileUrl))
                // Esta flag ajuda a garantir que abre noutra app sem crashar
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }

            // CASO 2: Foi gerado na App (tem Base64)
            if (!report.fileBase64.isNullOrEmpty()) {
                openBase64Pdf(context, report)
                return
            }

            Toast.makeText(context, "Ficheiro PDF não encontrado neste registo.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("ReportVM", "Erro ao abrir relatório", e)
            Toast.makeText(context, "Erro ao abrir o ficheiro. Verifique se tem um leitor de PDF.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openBase64Pdf(context: Context, report: Report) {
        try {
            // 1. Converter a String Base64 de volta para Bytes
            val pdfAsBytes = Base64.decode(report.fileBase64, Base64.DEFAULT)

            // 2. Criar ficheiro temporário na pasta Documents da App
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val fileName = "temp_view_${report.month}_${report.year}.pdf"
            val file = File(directory, fileName)

            // 3. Escrever os bytes no ficheiro
            val os = FileOutputStream(file)
            os.write(pdfAsBytes)
            os.close()

            // 4. Gerar URI seguro com FileProvider (igual ao que fizemos no gerador)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION

            val chooser = Intent.createChooser(intent, "Ver Relatório")
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e("ReportVM", "Erro converter Base64", e)
            Toast.makeText(context, "Erro ao processar ficheiro interno.", Toast.LENGTH_SHORT).show()
        }
    }
}