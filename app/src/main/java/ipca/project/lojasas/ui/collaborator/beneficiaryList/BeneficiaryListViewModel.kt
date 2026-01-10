package ipca.project.lojasas.ui.collaborator.beneficiaries

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.User

data class BeneficiaryListState(
    val beneficiaries: List<User> = emptyList(),
    val allBeneficiaries: List<User> = emptyList(), // Cópia para a pesquisa
    val searchText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HistoryState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false
)

class BeneficiaryListViewModel : ViewModel() {

    var uiState = mutableStateOf(BeneficiaryListState())
        private set

    var historyState = mutableStateOf(HistoryState())
        private set

    private val db = Firebase.firestore
    private var listener: ListenerRegistration? = null

    init {
        fetchBeneficiaries()
    }

    private fun fetchBeneficiaries() {
        uiState.value = uiState.value.copy(isLoading = true)

        // Usar addSnapshotListener para atualizações em TEMPO REAL
        // Assim que as faltas mudarem no outro ecrã, esta lista atualiza sozinha.
        listener = db.collection("users")
            .whereEqualTo("isBeneficiary", true) // Apenas quem é beneficiário ativo
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (value != null) {
                    val list = value.toObjects(User::class.java)

                    // Adicionar os IDs dos documentos
                    list.forEachIndexed { index, user ->
                        user.docId = value.documents[index].id
                    }

                    uiState.value = uiState.value.copy(
                        beneficiaries = list,
                        allBeneficiaries = list, // Guarda cópia para o filtro de pesquisa
                        isLoading = false
                    )

                    // Se houver texto na pesquisa, reaplica o filtro
                    if (uiState.value.searchText.isNotEmpty()) {
                        onSearchTextChange(uiState.value.searchText)
                    }
                }
            }
    }

    fun onSearchTextChange(text: String) {
        val filtered = if (text.isBlank()) {
            uiState.value.allBeneficiaries
        } else {
            uiState.value.allBeneficiaries.filter {
                it.name?.contains(text, ignoreCase = true) == true ||
                        it.email?.contains(text, ignoreCase = true) == true
            }
        }
        uiState.value = uiState.value.copy(searchText = text, beneficiaries = filtered)
    }

    // Buscar histórico (mantém-se igual)
    fun fetchUserHistory(userId: String) {
        historyState.value = historyState.value.copy(isLoading = true)

        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val orders = result.toObjects(Order::class.java)
                historyState.value = historyState.value.copy(orders = orders, isLoading = false)
            }
            .addOnFailureListener {
                historyState.value = historyState.value.copy(isLoading = false)
            }
    }

    fun clearHistory() {
        historyState.value = HistoryState()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove() // Limpar o listener para não gastar memória
    }
}