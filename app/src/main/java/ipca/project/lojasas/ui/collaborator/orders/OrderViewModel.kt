package ipca.project.lojasas.ui.collaborator.orders

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState

data class OrderListState(
    val orders: List<Order> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(OrderListState())
        private set

    init {
        fetchOrders()
    }

    private fun fetchOrders() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = mutableListOf<Order>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val o = Order()
                        o.docId = doc.id

                        // Campos básicos
                        o.userId = doc.getString("userId") ?: "--"
                        o.userName = doc.getString("userName") ?: "--"

                        // Datas
                        o.orderDate = doc.getTimestamp("orderDate")?.toDate()
                        o.surveyDate = doc.getTimestamp("surveyDate")?.toDate()

                        // Estado
                        val stateStr = doc.getString("accept") ?: "PENDENTE"
                        try {
                            o.accept = OrderState.valueOf(stateStr)
                        } catch (e: Exception) {
                            o.accept = OrderState.PENDENTE
                        }

                        list.add(o)

                    } catch (e: Exception) {
                        Log.e("OrderVM", "Erro ao ler documento: ${doc.id}", e)
                    }
                }

                // Contagem de pendentes
                val pendentes = list.count { it.accept == OrderState.PENDENTE }

                // Ordenar: Pendentes primeiro, depois mais recentes
                val sortedList = list.sortedWith(
                    compareBy<Order> { it.accept != OrderState.PENDENTE }
                        .thenByDescending { it.orderDate }
                )

                uiState.value = uiState.value.copy(
                    orders = sortedList,
                    pendingCount = pendentes,
                    isLoading = false,
                    error = null
                )
            }
    }
}
