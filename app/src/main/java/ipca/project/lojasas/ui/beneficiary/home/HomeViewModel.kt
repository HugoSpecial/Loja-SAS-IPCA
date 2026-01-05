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
    val searchText: String = "",         // Novo
    val selectedCategory: String = "Todos", // Novo
    val userName: String = "",           // Novo
    val isLoading: Boolean = false,
    val error: String? = null
)

class BeneficiaryHomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(BeneficiaryHomeState())
        private set

    init {
        fetchUserName()
        fetchCandidatureAndProducts()
    }

    // --- MÉTODOS DE UI ---
    fun onSearchTextChange(text: String) {
        uiState.value = uiState.value.copy(searchText = text)
    }

    fun onCategoryChange(category: String) {
        uiState.value = uiState.value.copy(selectedCategory = category)
    }

    // Filtra a lista plana de produtos
    fun getFilteredProducts(): List<Product> {
        val state = uiState.value
        return state.products.filter { product ->
            val matchesSearch = product.name.contains(state.searchText, ignoreCase = true)

            val matchesCategory = if (state.selectedCategory == "Todos") true
            else product.category.equals(state.selectedCategory, ignoreCase = true)

            // Verifica se a categoria é permitida (caso a seleção seja "Todos")
            val isAllowed = state.allowedCategories.any { it.equals(product.category, ignoreCase = true) }

            matchesSearch && matchesCategory && isAllowed
        }
    }

    // --- DADOS ---
    private fun fetchUserName() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fullName = document.getString("name") ?: "Beneficiário"
                    val firstName = fullName.split(" ").firstOrNull() ?: fullName
                    uiState.value = uiState.value.copy(userName = firstName)
                }
            }
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