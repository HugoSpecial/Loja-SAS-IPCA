package ipca.project.lojasas.ui.beneficiary.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.Product
import java.util.Date

data class BeneficiaryHomeState(
    val products: List<Product> = emptyList(),
    val allowedCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BeneficiaryHomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(BeneficiaryHomeState())
        private set

    init {
        fetchCandidatureAndProducts()
    }

    private fun fetchCandidatureAndProducts() {
        uiState.value = uiState.value.copy(isLoading = true)
        val userId = auth.currentUser?.uid

        if (userId == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado")
            return
        }

        db.collection("candidatures")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val candidature = documents.documents[0].toObject(Candidature::class.java)
                    if (candidature != null) {
                        val categoriesToShow = mutableListOf<String>()
                        // Garante que estas strings são iguais às do Firebase (campo category)
                        if (candidature.foodProducts) categoriesToShow.add("Alimentos")
                        if (candidature.hygieneProducts) categoriesToShow.add("Higiene")
                        if (candidature.cleaningProducts) categoriesToShow.add("Limpeza")

                        uiState.value = uiState.value.copy(allowedCategories = categoriesToShow)
                        fetchProducts()
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Candidatura não encontrada.")
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    private fun fetchProducts() {
        db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val today = Date()
                val list = mutableListOf<Product>()

                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val product = doc.toObject(Product::class.java) ?: continue
                        product.docId = doc.id
                        // Filtra produtos com stock e validade
                        if (product.batches.any { it.quantity > 0 && it.validity >= today }) {
                            list.add(product)
                        }
                    } catch (e: Exception) {
                        Log.e("BeneficiaryHomeVM", "Erro ao ler produto", e)
                    }
                }

                uiState.value = uiState.value.copy(products = list, isLoading = false, error = null)
            }
    }
}