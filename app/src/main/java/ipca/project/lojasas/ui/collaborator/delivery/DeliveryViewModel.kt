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

                    // --- CORREÇÃO IMPORTANTE ---
                    // Lê o estado com segurança. Se der erro (ex: estado "JUSTIFICADO" antigo),
                    // assume CANCELADO para a app não ir abaixo.
                    val rawState = doc.getString("state") ?: "PENDENTE"
                    val safeState = try {
                        DeliveryState.valueOf(rawState)
                    } catch (e: Exception) {
                        DeliveryState.CANCELADO
                    }

                    val delivery = Delivery(
                        docId = doc.id,
                        orderId = doc.getString("orderId"),
                        delivered = doc.getBoolean("delivered") ?: false,
                        reason = doc.getString("reason"),
                        state = safeState, // Usamos o estado seguro aqui
                        surveyDate = doc.getDate("surveyDate"),
                        evaluationDate = doc.getDate("evaluationDate"),
                        evaluatedBy = doc.getString("evaluatedBy")
                    )

                    val orderId = delivery.orderId ?: continue

                    // Busca o utilizador para mostrar o nome na lista
                    db.collection("orders")
                        .document(orderId)
                        .get()
                        .addOnSuccessListener { orderDoc ->
                            val userName = orderDoc.getString("userName")
                            val userId = orderDoc.getString("userId")

                            val item = DeliveryWithUser(
                                delivery = delivery,
                                userName = userName,
                                userId = userId
                            )

                            // Atualiza a lista evitando duplicados visuais
                            val index = resultList.indexOfFirst { it.delivery.docId == delivery.docId }
                            if (index != -1) {
                                resultList[index] = item
                            } else {
                                resultList.add(item)
                            }

                            // Ordena por data (mais recentes primeiro)
                            resultList.sortByDescending { it.delivery.surveyDate }

                            uiState.value = uiState.value.copy(
                                deliveries = ArrayList(resultList),
                                pendingCount = resultList.count { it.delivery.state == DeliveryState.PENDENTE },
                                isLoading = false
                            )
                        }
                }

                if (value?.isEmpty == true) {
                    uiState.value = uiState.value.copy(isLoading = false, deliveries = emptyList())
                }
            }
    }

    // Mantive a tua função original, caso precises dela noutros ecrãs
    fun FaultDelivery(userId: String, deliveryId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        val userRef = db.collection("users").document(userId)
        val deliveryRef = db.collection("delivery").document(deliveryId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)

            val currentFaults = (userSnapshot.getLong("fault") ?: 0).toInt()
            val newFaults = currentFaults + 1
            val isBeneficiary = newFaults < 2

            // Atualiza User na BD
            transaction.update(userRef, "fault", newFaults)
            transaction.update(userRef, "isBeneficiary", isBeneficiary)

            // Atualiza Delivery para CANCELADO
            transaction.update(deliveryRef, "state", DeliveryState.CANCELADO.name)
            transaction.update(deliveryRef, "reason", "Falta de comparência")
            transaction.update(deliveryRef, "delivered", false)

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