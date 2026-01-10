package ipca.project.lojasas.ui.collaborator.delivery

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.utils.PdfGenerator
import java.io.File
import java.util.Calendar

data class DeliveryWithUser (
    val delivery: Delivery,
    val userName: String? = null,
    val userId: String? = null
)

data class DeliveryListState(
    val deliveries: List<DeliveryWithUser> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val error: String? = null
)

class DeliveryViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(DeliveryListState())
        private set

    init {
        fetchDeliveries()
    }

    // ====================================================================================
    //  GERAR PDF + GRAVAR
    // ====================================================================================

    fun generateCurrentMonthReport(context: Context) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        uiState.value = uiState.value.copy(isGeneratingReport = true)

        Thread {
            try {
                // 1. Filtrar Entregas
                val deliveriesWithUserDoMes = uiState.value.deliveries.filter { item ->
                    if (item.delivery.surveyDate != null) {
                        val cal = Calendar.getInstance()
                        cal.time = item.delivery.surveyDate!!
                        (cal.get(Calendar.MONTH) + 1 == currentMonth &&
                                cal.get(Calendar.YEAR) == currentYear)
                    } else {
                        false
                    }
                }

                val deliveriesParaPdf = deliveriesWithUserDoMes.map { it.delivery }

                val beneficiaryNames = deliveriesWithUserDoMes.associate {
                    (it.delivery.docId ?: "") to (it.userName ?: "Anónimo")
                }

                val collaboratorNames = mutableMapOf<String, String>()
                val uniqueIds = deliveriesParaPdf.mapNotNull { it.evaluatedBy }.distinct()

                for (id in uniqueIds) {
                    if (id.isNotEmpty()) {
                        try {
                            val docSnapshot = Tasks.await(db.collection("users").document(id).get())
                            val name = docSnapshot.getString("name") ?: "Sem Nome"
                            collaboratorNames[id] = name
                        } catch (e: Exception) {
                            collaboratorNames[id] = "Desconhecido"
                        }
                    }
                }

                val pdfFile = PdfGenerator.generateDeliveryReport(
                    context,
                    deliveriesParaPdf,
                    currentMonth,
                    currentYear,
                    collaboratorNames,
                    beneficiaryNames
                )

                Handler(Looper.getMainLooper()).post {
                    uiState.value = uiState.value.copy(isGeneratingReport = false)

                    if (pdfFile != null && pdfFile.exists()) {
                        abrirPdf(context, pdfFile)
                        saveReportToFirestore(currentMonth, currentYear, deliveriesParaPdf.size, pdfFile)
                    } else {
                        Toast.makeText(context, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    uiState.value = uiState.value.copy(isGeneratingReport = false)
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun saveReportToFirestore(month: Int, year: Int, totalItems: Int, pdfFile: File) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "unknown"

        val fileBytes = pdfFile.readBytes()
        val base64String = Base64.encodeToString(fileBytes, Base64.DEFAULT)

        // --- AQUI ESTÁ A GARANTIA PARA ENTREGAS ---
        db.collection("reports")
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .whereEqualTo("type", "delivery_report") // <--- FILTRO DE TIPO (Só apaga entregas)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                val newReportRef = db.collection("reports").document()
                val reportData = hashMapOf(
                    "title" to "Relatório de Entregas - $month/$year",
                    "month" to month,
                    "year" to year,
                    "totalOrders" to totalItems,
                    "generatedAt" to com.google.firebase.Timestamp.now(),
                    "generatedBy" to userId,
                    "type" to "delivery_report", // <--- TIPO ESPECÍFICO
                    "status" to "created",
                    "fileBase64" to base64String
                )

                batch.set(newReportRef, reportData)

                batch.commit()
                    .addOnSuccessListener { Log.d("Firestore", "Relatório de Entregas atualizado.") }
                    .addOnFailureListener { e -> Log.e("Firestore", "Erro ao gravar relatório", e) }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao verificar relatórios existentes", e)
            }
    }

    private fun abrirPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(Intent.createChooser(intent, "Abrir Relatório"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sem app de PDF instalada.", Toast.LENGTH_LONG).show()
        }
    }

    // ====================================================================================
    // FETCH (Igual)
    // ====================================================================================
    private fun fetchDeliveries() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("delivery")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val documents = value?.documents ?: emptyList()
                if (documents.isEmpty()) {
                    uiState.value = uiState.value.copy(isLoading = false, deliveries = emptyList())
                    return@addSnapshotListener
                }

                val resultList = mutableListOf<DeliveryWithUser>()
                var processedCount = 0

                for (doc in documents) {
                    val rawState = doc.getString("state") ?: "PENDENTE"
                    val safeState = try { DeliveryState.valueOf(rawState) } catch (e: Exception) { DeliveryState.CANCELADO }

                    val delivery = Delivery(
                        docId = doc.id,
                        orderId = doc.getString("orderId"),
                        delivered = doc.getBoolean("delivered") ?: false,
                        reason = doc.getString("reason"),
                        state = safeState,
                        surveyDate = doc.getTimestamp("surveyDate")?.toDate(),
                        evaluationDate = doc.getTimestamp("evaluationDate")?.toDate(),
                        evaluatedBy = doc.getString("evaluatedBy"),
                        beneficiaryNote = doc.getString("beneficiaryNote")
                    )

                    val orderId = delivery.orderId

                    if (orderId != null) {
                        db.collection("orders").document(orderId).get()
                            .addOnSuccessListener { orderDoc ->
                                val userName = orderDoc.getString("userName")
                                val userId = orderDoc.getString("userId")
                                val item = DeliveryWithUser(delivery, userName, userId)
                                synchronized(resultList) {
                                    val index = resultList.indexOfFirst { it.delivery.docId == delivery.docId }
                                    if (index != -1) resultList[index] = item else resultList.add(item)
                                }
                                processedCount++
                                updateUiIfReady(resultList, documents.size, processedCount)
                            }
                            .addOnFailureListener {
                                val item = DeliveryWithUser(delivery, "Desconhecido", null)
                                synchronized(resultList) { resultList.add(item) }
                                processedCount++
                                updateUiIfReady(resultList, documents.size, processedCount)
                            }
                    } else {
                        val item = DeliveryWithUser(delivery, "--", null)
                        synchronized(resultList) { resultList.add(item) }
                        processedCount++
                        updateUiIfReady(resultList, documents.size, processedCount)
                    }
                }
            }
    }

    private fun updateUiIfReady(list: MutableList<DeliveryWithUser>, totalDocs: Int, currentCount: Int) {
        list.sortByDescending { it.delivery.surveyDate }
        uiState.value = uiState.value.copy(
            deliveries = ArrayList(list),
            pendingCount = list.count { it.delivery.state == DeliveryState.PENDENTE },
            isLoading = currentCount < totalDocs
        )
    }
}