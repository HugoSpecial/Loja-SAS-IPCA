package ipca.project.lojasas.ui.beneficiary.history

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.Order

data class BeneficiaryHistoryState(
    val orders: List<Order> = emptyList(),
    val deliveries: List<Delivery> = emptyList(),
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
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.apply {
                        docId = doc.id
                    }
                } ?: emptyList()

                uiState.value = uiState.value.copy(
                    orders = orders.sortedByDescending { it.orderDate },
                    isLoading = false
                )

                // 🔥 AQUI: depois de ter orders, buscar deliveries
                fetchDeliveriesForOrders(orders.mapNotNull { it.docId })
            }
    }

    private fun fetchDeliveriesForOrders(orderIds: List<String>) {
        if (orderIds.isEmpty()) {
            uiState.value = uiState.value.copy(deliveries = emptyList())
            return
        }

        db.collection("delivery")
            .whereIn("orderId", orderIds.take(10))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(error = error.message)
                    return@addSnapshotListener
                }

                val deliveries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Delivery::class.java)?.apply {
                        docId = doc.id
                    }
                } ?: emptyList()

                uiState.value = uiState.value.copy(
                    deliveries = deliveries.sortedByDescending { it.surveyDate }
                )
            }
    }
}
