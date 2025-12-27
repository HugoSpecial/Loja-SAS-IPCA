package ipca.project.lojasas.ui.collaborator.delivery

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import java.util.Date

data class DeliveryWithUser (
    val delivery: Delivery,
    val userName: String? = null,
    val surveyDate: Date? = null
)

data class DeliveryListState(
    val deliveries: List<DeliveryWithUser> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DeliveryViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(DeliveryListState())
        private set

    init {
        fetchDeliveries()
    }

    private fun fetchDeliveries() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("delivery")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val resultList = mutableListOf<DeliveryWithUser>()

                for (doc in value?.documents ?: emptyList()) {

                    val delivery = Delivery(
                        docId = doc.id,
                        orderId = doc.getString("orderId"),
                        delivered = doc.getBoolean("delivered") ?: false,
                        reason = doc.getString("reason"),
                        state = DeliveryState.valueOf(
                            doc.getString("state") ?: "PENDENTE"
                        )
                    )

                    val orderId = delivery.orderId ?: continue

                    db.collection("orders")
                        .document(orderId)
                        .get()
                        .addOnSuccessListener { orderDoc ->

                            val userName = orderDoc.getString("userName")
                            val surveyDate = orderDoc.getDate("surveyDate");

                            resultList.add(
                                DeliveryWithUser(
                                    delivery = delivery,
                                    userName = userName,
                                    surveyDate = surveyDate
                                )
                            )

                            uiState.value = uiState.value.copy(
                                deliveries = resultList,
                                pendingCount = resultList.count {
                                    it.delivery.state == DeliveryState.PENDENTE
                                },
                                isLoading = false
                            )
                        }
                }
            }
    }
}
