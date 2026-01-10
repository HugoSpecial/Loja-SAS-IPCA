package ipca.project.lojasas.ui.collaborator.stock

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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.utils.PdfGenerator
import java.io.File
import java.util.Calendar
import java.util.Date

data class StockState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingReport: Boolean = false, // Novo estado
    val error: String? = null,
    val searchText: String = "",
    val selectedCategory: String = "Todos"
)

class StockViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(StockState())
        private set

    init {
        fetchStock()
    }

    // ====================================================================================
    // GERAR PDF E GRAVAR (TIPO: STOCK_REPORT)
    // ====================================================================================

    fun generateCurrentMonthReport(context: Context) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        uiState.value = uiState.value.copy(isGeneratingReport = true)

        Thread {
            try {
                // Para relatório de stock, geralmente queremos ver TUDO, ou a lista filtrada?
                // O padrão é tirar uma "foto" ao stock completo atual.
                val stockList = uiState.value.items

                // Gerar PDF
                val pdfFile = PdfGenerator.generateStockReport(
                    context,
                    stockList,
                    currentMonth,
                    currentYear
                )

                Handler(Looper.getMainLooper()).post {
                    uiState.value = uiState.value.copy(isGeneratingReport = false)

                    if (pdfFile != null && pdfFile.exists()) {
                        abrirPdf(context, pdfFile)
                        saveReportToFirestore(currentMonth, currentYear, stockList.size, pdfFile)
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

        db.collection("reports")
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .whereEqualTo("type", "stock_report") // <--- TIPO ESPECÍFICO PARA STOCK
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                // Apagar antigo se existir
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                // Criar novo
                val newReportRef = db.collection("reports").document()
                val reportData = hashMapOf(
                    "title" to "Inventário de Stock - $month/$year",
                    "month" to month,
                    "year" to year,
                    "totalOrders" to totalItems, // Reutilizando campo 'totalOrders' para qtd de produtos
                    "generatedAt" to com.google.firebase.Timestamp.now(),
                    "generatedBy" to userId,
                    "type" to "stock_report",
                    "status" to "created",
                    "fileBase64" to base64String
                )

                batch.set(newReportRef, reportData)

                batch.commit()
                    .addOnSuccessListener { Log.d("Firestore", "Relatório de Stock gravado.") }
                    .addOnFailureListener { e -> Log.e("Firestore", "Erro ao gravar relatório", e) }
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
    // LÓGICA EXISTENTE
    // ====================================================================================

    private fun fetchStock() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("products")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val stockList = mutableListOf<Product>()
                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val item = doc.toObject(Product::class.java)
                        if (item != null) {
                            item.docId = doc.id
                            stockList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e("StockVM", "Erro ao ler produto: ${doc.id}", e)
                    }
                }

                uiState.value = uiState.value.copy(items = stockList, isLoading = false, error = null)
            }
    }

    fun onSearchTextChange(text: String) {
        uiState.value = uiState.value.copy(searchText = text)
    }

    fun onCategoryChange(category: String) {
        uiState.value = uiState.value.copy(selectedCategory = category)
    }

    fun getFilteredItems(): List<Product> {
        val query = uiState.value.searchText.lowercase()
        val currentCategory = uiState.value.selectedCategory

        return uiState.value.items.filter { product ->
            val matchesName = product.name.lowercase().contains(query)
            val hasValidBatches = product.batches.any { it.quantity > 0 && isDateValid(it.validity) }
            val hasExpiredBatches = product.batches.any { it.quantity > 0 && !isDateValid(it.validity) }

            val matchesCategory = when (currentCategory) {
                "Fora de validade" -> hasExpiredBatches
                "Todos" -> hasValidBatches
                else -> product.category.equals(currentCategory, ignoreCase = true) && hasValidBatches
            }
            matchesName && matchesCategory
        }
    }

    fun deleteProduct(docId: String, onSuccess: () -> Unit) {
        if (docId.isEmpty()) return
        db.collection("products").document(docId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("StockViewModel", "Erro ao apagar", e) }
    }

    private fun isDateValid(date: Date?): Boolean {
        if (date == null) return false
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return !date.before(today.time)
    }
}