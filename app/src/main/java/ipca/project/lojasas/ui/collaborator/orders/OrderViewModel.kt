package ipca.project.lojasas.ui.collaborator.orders

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64 // Importante para o Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks // Necessário para o await
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.utils.PdfGenerator
import java.io.File
import java.util.Calendar

data class OrderListState(
    val orders: List<Order> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val error: String? = null
)

class OrderViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(OrderListState())
        private set

    init {
        fetchOrders()
    }

    // ====================================================================================
    //  GERAR PDF + FETCH NOMES + BASE64
    // ====================================================================================

    fun generateCurrentMonthReport(context: Context) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        uiState.value = uiState.value.copy(isGeneratingReport = true)

        Thread {
            try {
                // 1. Filtrar Orders
                val ordersDoMes = uiState.value.orders.filter { order ->
                    if (order.orderDate != null) {
                        val calOrder = Calendar.getInstance()
                        calOrder.time = order.orderDate!!
                        (calOrder.get(Calendar.MONTH) + 1 == currentMonth &&
                                calOrder.get(Calendar.YEAR) == currentYear)
                    } else {
                        false
                    }
                }

                // 2. BUSCAR NOMES DOS COLABORADORES (NOVA PARTE)
                val collaboratorNames = mutableMapOf<String, String>()

                // Obter lista de IDs únicos que avaliaram encomendas
                val uniqueIds = ordersDoMes.mapNotNull { it.evaluatedBy }.distinct()

                // Ir ao Firestore buscar o nome de cada um (Users Collection)
                for (id in uniqueIds) {
                    if (id.isNotEmpty()) {
                        try {
                            // Tasks.await bloqueia esta thread até termos o resultado (seguro aqui porque estamos numa Thread secundária)
                            val docSnapshot = Tasks.await(db.collection("users").document(id).get())
                            val name = docSnapshot.getString("name") ?: "Sem Nome"
                            collaboratorNames[id] = name
                        } catch (e: Exception) {
                            Log.e("PDF", "Erro ao buscar nome do user $id", e)
                            collaboratorNames[id] = "Desconhecido"
                        }
                    }
                }

                // 3. Gerar PDF passando os nomes
                val pdfFile = PdfGenerator.generateOrderReport(
                    context,
                    ordersDoMes,
                    currentMonth,
                    currentYear,
                    collaboratorNames
                )

                // 4. Voltar à UI
                Handler(Looper.getMainLooper()).post {
                    uiState.value = uiState.value.copy(isGeneratingReport = false)

                    if (pdfFile != null && pdfFile.exists()) {
                        abrirPdf(context, pdfFile)
                        // Passamos o ficheiro para o converter em Base64
                        saveReportToFirestore(currentMonth, currentYear, ordersDoMes.size, pdfFile)
                    } else {
                        Toast.makeText(context, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    uiState.value = uiState.value.copy(isGeneratingReport = false)
                    Toast.makeText(context, "Erro interno: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun saveReportToFirestore(month: Int, year: Int, totalOrders: Int, pdfFile: File) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "unknown"

        // Converter PDF para Base64
        val fileBytes = pdfFile.readBytes()
        val base64String = Base64.encodeToString(fileBytes, Base64.DEFAULT)

        db.collection("reports")
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                // Apagar antigos
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                // Gravar Novo com Base64
                val newReportRef = db.collection("reports").document()
                val reportData = hashMapOf(
                    "title" to "Relatório Mensal - $month/$year",
                    "month" to month,
                    "year" to year,
                    "totalOrders" to totalOrders,
                    "generatedAt" to com.google.firebase.Timestamp.now(),
                    "generatedBy" to userId,
                    "type" to "local_pdf_app",
                    "status" to "created",
                    "fileBase64" to base64String // <--- AQUI ESTÁ O PDF
                )

                batch.set(newReportRef, reportData)

                batch.commit()
                    .addOnSuccessListener { Log.d("Firestore", "Relatório e Base64 gravados.") }
                    .addOnFailureListener { e -> Log.e("Firestore", "Erro ao gravar", e) }
            }
            .addOnFailureListener { e -> Log.e("Firestore", "Erro verificação", e) }
    }

    private fun abrirPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(Intent.createChooser(intent, "Abrir Relatório"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sem app de PDF instalada.", Toast.LENGTH_LONG).show()
        }
    }

    // --- FETCH ORDERS (Mantém-se igual) ---
    private fun fetchOrders() {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("orders").addSnapshotListener { value, error ->
            if (error != null) {
                uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                return@addSnapshotListener
            }
            val list = mutableListOf<Order>()
            for (doc in value?.documents ?: emptyList()) {
                try {
                    val o = Order()
                    o.docId = doc.id
                    o.userId = doc.getString("userId") ?: "--"
                    o.userName = doc.getString("userName") ?: "--"
                    o.orderDate = doc.getTimestamp("orderDate")?.toDate()
                    o.surveyDate = doc.getTimestamp("surveyDate")?.toDate()

                    // Campos Avaliação
                    o.evaluatedBy = doc.getString("evaluatedBy")
                    o.evaluationDate = doc.getTimestamp("evaluationDate")?.toDate()

                    val stateStr = doc.getString("accept") ?: "PENDENTE"
                    try { o.accept = OrderState.valueOf(stateStr) } catch (e: Exception) { o.accept = OrderState.PENDENTE }

                    val itemsRaw = doc.get("items") as? List<Map<String, Any>>
                    if (itemsRaw != null) {
                        for (itemMap in itemsRaw) {
                            o.items.add(OrderItem(
                                name = itemMap["name"] as? String,
                                quantity = (itemMap["quantity"] as? Long)?.toInt() ?: 0
                            ))
                        }
                    }
                    list.add(o)
                } catch (e: Exception) { Log.e("OrderVM", "Erro leitura", e) }
            }
            val sortedList = list.sortedWith(compareBy<Order> { it.accept != OrderState.PENDENTE }.thenByDescending { it.orderDate })
            uiState.value = uiState.value.copy(orders = sortedList, pendingCount = list.count { it.accept == OrderState.PENDENTE }, isLoading = false)
        }
    }
}