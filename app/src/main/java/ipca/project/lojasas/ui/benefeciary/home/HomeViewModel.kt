package ipca.project.lojasas.ui.benefeciary.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState

data class BeneficiaryHomeState(
    val orders: List<Order> = emptyList(),
    val upcomingCount: Int = 0,
    val userName: String = "",
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
        fetchOrders()
    }

    private fun fetchUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("name") ?: ""
                        uiState.value = uiState.value.copy(userName = name)
                    }
                }
                .addOnFailureListener {
                    Log.e("BeneficiaryHomeVM", "Erro ao obter nome", it)
                }
        }
    }

    private fun fetchOrders() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("orders")
            .whereEqualTo("userId", auth.currentUser?.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                var pendingCount = 0
                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val acceptStr = doc.getString("accept") ?: "PENDENTE"
                        val acceptState = OrderState.valueOf(acceptStr)
                        if (acceptState == OrderState.PENDENTE) {
                            pendingCount++
                        }
                    } catch (e: Exception) {
                        Log.e("BeneficiaryHomeVM", "Erro ao ler order", e)
                    }
                }

                uiState.value = uiState.value.copy(
                    upcomingCount = pendingCount,
                    isLoading = false,
                    error = null
                )
            }
    }
}
