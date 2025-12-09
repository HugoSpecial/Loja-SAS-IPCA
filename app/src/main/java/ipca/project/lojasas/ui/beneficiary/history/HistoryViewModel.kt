package ipca.project.lojasas.ui.beneficiary.history

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Order

data class BeneficiaryHistoryState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BeneficiaryHistoryViewModel : ViewModel() {

    var uiState = mutableStateOf(BeneficiaryHistoryState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    init {
        fetchOrders()
    }

    private fun fetchOrders() {
        val userId = auth.currentUser?.uid ?: return
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BeneficiaryHistoryVM", "Erro ao carregar pedidos", error)
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val orderList = mutableListOf<Order>()
                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val order = doc.toObject(Order::class.java)
                        order?.docId = doc.id
                        if (order != null) orderList.add(order)
                    } catch (e: Exception) {
                        Log.e("BeneficiaryHistoryVM", "Erro ao converter pedido", e)
                    }
                }

                uiState.value = uiState.value.copy(
                    orders = orderList.sortedByDescending { it.orderDate },
                    isLoading = false,
                    error = null
                )
            }
    }
}
