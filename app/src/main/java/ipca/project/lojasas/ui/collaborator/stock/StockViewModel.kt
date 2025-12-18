package ipca.project.lojasas.ui.collaborator.stock

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Product

data class StockState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val selectedCategory: String = "Todos" // <--- Novo Estado para a categoria
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

    // --- Nova função para mudar a categoria ---
    fun onCategoryChange(category: String) {
        uiState.value = uiState.value.copy(selectedCategory = category)
    }

    // --- Lógica de Filtro Atualizada ---
    fun getFilteredItems(): List<Product> {
        val query = uiState.value.searchText.lowercase()
        val currentCategory = uiState.value.selectedCategory

        return uiState.value.items.filter { product ->
            // 1. Filtra pelo texto de pesquisa
            val matchesName = product.name.lowercase().contains(query)

            // 2. Filtra pela categoria
            val matchesCategory = if (currentCategory == "Todos") {
                true // Se for "Todos", aceita tudo
            } else {
                // Compara ignorando maiúsculas/minúsculas (ex: "Alimentos" == "alimentos")
                product.category.equals(currentCategory, ignoreCase = true)
            }

            // Retorna apenas se corresponder a ambos
            matchesName && matchesCategory
        }
    }

    fun deleteProduct(docId: String, onSuccess: () -> Unit) {
        if (docId.isEmpty()) return

        db.collection("products").document(docId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("StockViewModel", "Erro ao apagar", e) }
    }
}