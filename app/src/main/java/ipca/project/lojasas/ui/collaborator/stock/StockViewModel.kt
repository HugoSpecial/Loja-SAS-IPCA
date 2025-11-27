package ipca.project.lojasas.ui.collaborator.stock

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Product

data class StockState(
    val items: List<Product> = emptyList(), // Lista de Produtos
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = ""
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
                        // Converte diretamente para o objeto Product
                        val item = doc.toObject(Product::class.java)
                        if (item != null) {
                            item.docId = doc.id // Guarda o ID do documento
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

    fun getFilteredItems(): List<Product> {
        val query = uiState.value.searchText.lowercase()
        return if (query.isEmpty()) {
            uiState.value.items
        } else {
            uiState.value.items.filter {
                it.name.lowercase().contains(query)
            }
        }
    }

    // --- APAGAR PRODUTO ---
    fun deleteProduct(docId: String, onSuccess: () -> Unit) {
        if (docId.isEmpty()) return

        db.collection("products").document(docId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> Log.e("StockViewModel", "Erro ao apagar", e) }
    }
}