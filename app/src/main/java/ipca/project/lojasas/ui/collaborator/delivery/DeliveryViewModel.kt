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
    val userId: String? = null
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
                        ),
                        surveyDate = doc.getDate("surveyDate"),
                        evaluationDate = doc.getDate("evaluationDate"),
                        evaluatedBy = doc.getString("evaluatedBy")
                    )

                    val orderId = delivery.orderId ?: continue

                    db.collection("orders")
                        .document(orderId)
                        .get()
                        .addOnSuccessListener { orderDoc ->
                            val userName = orderDoc.getString("userName")
                            val userId = orderDoc.getString("userId")

                            resultList.add(
                                DeliveryWithUser(
                                    delivery = delivery,
                                    userName = userName,
                                    userId = userId
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

    //Função em Teste (Pires Lindu - 912343569)
    fun FaultDelivery(userId: String, deliveryId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        val userId = db.collection("users").document(userId)
        val deliveryId = db.collection("delivery").document(deliveryId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userId)

            val currentFaults = (userSnapshot.getLong("fault") ?: 0).toInt()

            val newFaults = currentFaults + 1

            val isBeneficiary = newFaults < 2

            // Atualizar User na BD
            transaction.update(userId, "fault", newFaults)
            transaction.update(userId, "isBeneficiary", isBeneficiary)

            // Atualizar Delivery para CANCELADO
            transaction.update(deliveryId, "state", DeliveryState.CANCELADO.name)
            transaction.update(deliveryId, "reason", "Falta de comparência")
            transaction.update(deliveryId, "delivered", false)

        }.addOnSuccessListener {
            uiState.value = uiState.value.copy(isLoading = false)
            Log.d("DeliveryVM", "Falta marcada com sucesso.")

        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Erro: ${e.message}"
            )
        }
    }
}
