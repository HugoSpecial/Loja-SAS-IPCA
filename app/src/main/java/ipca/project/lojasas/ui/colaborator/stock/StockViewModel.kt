package ipca.project.lojasas.ui.colaborator.stock

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.StockItem

data class StockState(
    val items: List<StockItem> = emptyList(),
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

                val stockList = mutableListOf<StockItem>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        // Converte o documento diretamente para o objeto StockItem
                        // Isto funciona automaticamente se os nomes na BD baterem certo (name, imageUrl, batches)
                        val item = doc.toObject(StockItem::class.java)
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

    fun getFilteredItems(): List<StockItem> {
        val query = uiState.value.searchText.lowercase()
        return if (query.isEmpty()) {
            uiState.value.items
        } else {
            uiState.value.items.filter {
                it.name.lowercase().contains(query)
            }
        }
    }

    fun deleteProduct(docId: String, onSuccess: () -> Unit) {
        if (docId.isEmpty()) return

        db.collection("products")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("StockViewModel", "Erro ao apagar", e)
            }
    }
}