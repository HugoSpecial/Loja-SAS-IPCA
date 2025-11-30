package ipca.project.lojasas.ui.beneficiary.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.ProductTest
import java.util.Date

data class BeneficiaryHomeState(
    val products: List<ProductTest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BeneficiaryHomeViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(BeneficiaryHomeState())
        private set

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val today = Date()
                val list = mutableListOf<ProductTest>()
                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val product = doc.toObject(ProductTest::class.java) ?: continue
                        product.docId = doc.id

                        // Só adiciona se existir algum batch com stock > 0 e validade >= hoje
                        if (product.batches.any { it.quantity > 0 && it.validity >= today }) {
                            list.add(product)
                        }

                    } catch (e: Exception) {
                        Log.e("BeneficiaryHomeVM", "Erro ao ler produto: ${doc.id}", e)
                    }
                }

                uiState.value = uiState.value.copy(
                    products = list,
                    isLoading = false,
                    error = null
                )
            }
    }
}
