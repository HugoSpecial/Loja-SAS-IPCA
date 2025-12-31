package ipca.project.lojasas.ui.collaborator.beneficiaries

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.User

data class BeneficiaryListState(
    val beneficiaries: List<User> = emptyList(),
    val searchText: String = "", // Novo campo
    val isLoading: Boolean = false,
    val error: String? = null
)

// Estado para o histórico (mantém-se igual)
data class HistoryState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false
)

class BeneficiaryListViewModel : ViewModel() {

    var uiState = mutableStateOf(BeneficiaryListState())
        private set

    var historyState = mutableStateOf(HistoryState())
        private set

    private val db = FirebaseFirestore.getInstance()

    // Lista auxiliar para guardar TODOS os dados originais
    private var originalList: List<User> = emptyList()

    init {
        fetchBeneficiaries()
    }

    private fun fetchBeneficiaries() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("users")
            .whereEqualTo("isBeneficiary", true)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<User>()
                for (doc in result) {
                    try {
                        val user = doc.toObject(User::class.java)
                        user.docId = doc.id
                        list.add(user)
                    } catch (e: Exception) {
                        Log.e("BeneficiaryVM", "Erro ao converter: ${e.message}")
                    }
                }

                // Guarda a lista completa original e ordenada
                originalList = list.sortedBy { it.name ?: "" }

                // Atualiza a UI com a lista completa inicial
                uiState.value = uiState.value.copy(
                    beneficiaries = originalList,
                    isLoading = false
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    // --- NOVA FUNÇÃO: Filtrar a lista ---
    fun onSearchTextChange(text: String) {
        val filteredList = if (text.isBlank()) {
            originalList
        } else {
            originalList.filter { user ->
                (user.name ?: "").contains(text, ignoreCase = true) ||
                        (user.email ?: "").contains(text, ignoreCase = true)
            }
        }

        uiState.value = uiState.value.copy(
            searchText = text,
            beneficiaries = filteredList
        )
    }

    // --- Histórico (Mantém-se igual) ---
    fun fetchUserHistory(userId: String) {
        historyState.value = historyState.value.copy(isLoading = true, orders = emptyList())
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val orders = result.toObjects(Order::class.java)
                historyState.value = historyState.value.copy(orders = orders, isLoading = false)
            }
            .addOnFailureListener { fetchUserHistoryNoSort(userId) }
    }

    private fun fetchUserHistoryNoSort(userId: String) {
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val orders = result.toObjects(Order::class.java)
                val sorted = orders.sortedByDescending { it.orderDate }
                historyState.value = historyState.value.copy(orders = sorted, isLoading = false)
            }
            .addOnFailureListener { historyState.value = historyState.value.copy(isLoading = false) }
    }

    fun clearHistory() { historyState.value = HistoryState() }
}