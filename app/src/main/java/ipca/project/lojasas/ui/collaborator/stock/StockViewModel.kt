package ipca.project.lojasas.ui.collaborator.stock

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Product
import java.util.Calendar
import java.util.Date

data class StockState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val selectedCategory: String = "Todos"
)

class StockViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(StockState())
        private set

    init {
        fetchStock()
    }

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

                uiState.value = uiState.value.copy(
                    items = stockList,
                    isLoading = false,
                    error = null
                )
            }
    }

    fun onSearchTextChange(text: String) {
        uiState.value = uiState.value.copy(searchText = text)
    }

    fun onCategoryChange(category: String) {
        uiState.value = uiState.value.copy(selectedCategory = category)
    }

    // --- LÓGICA DE FILTRO ATUALIZADA ---
    fun getFilteredItems(): List<Product> {
        val query = uiState.value.searchText.lowercase()
        val currentCategory = uiState.value.selectedCategory

        return uiState.value.items.filter { product ->
            // 1. Filtra pelo texto de pesquisa
            val matchesName = product.name.lowercase().contains(query)

            // 2. Verifica Validade dos lotes
            val hasValidBatches = product.batches.any { it.quantity > 0 && isDateValid(it.validity) }
            val hasExpiredBatches = product.batches.any { it.quantity > 0 && !isDateValid(it.validity) }

            // 3. Lógica da Categoria
            val matchesCategory = when (currentCategory) {
                "Fora de validade" -> hasExpiredBatches // Mostra se tiver algum expirado
                "Todos" -> hasValidBatches // Mostra apenas se tiver stock válido
                else -> {
                    // Se for uma categoria específica (Alimentos, etc), tem de bater certo a categoria E ter stock válido
                    product.category.equals(currentCategory, ignoreCase = true) && hasValidBatches
                }
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

    // Função auxiliar para verificar se a data é válida (Hoje ou Futuro)
    private fun isDateValid(date: Date?): Boolean {
        if (date == null) return false
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val checkDate = Calendar.getInstance()
        checkDate.time = date
        checkDate.set(Calendar.HOUR_OF_DAY, 0)
        checkDate.set(Calendar.MINUTE, 0)
        checkDate.set(Calendar.SECOND, 0)
        checkDate.set(Calendar.MILLISECOND, 0)

        return !checkDate.before(today)
    }
}