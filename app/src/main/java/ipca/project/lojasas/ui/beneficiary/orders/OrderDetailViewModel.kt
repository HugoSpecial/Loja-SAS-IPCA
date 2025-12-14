package ipca.project.lojasas.ui.beneficiary.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.ProposalDelivery
import java.util.Date

data class BeneficiaryOrderState(
    val selectedOrder: Order? = null,
    val proposals: List<ProposalDelivery> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class BeneficiaryOrderViewModel : ViewModel() {

    var uiState by mutableStateOf(BeneficiaryOrderState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    fun fetchOrder(orderId: String) {
        uiState = uiState.copy(isLoading = true, error = null)

        db.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                val order = doc.toObject(Order::class.java)?.apply { docId = doc.id }

                db.collection("orders")
                    .document(orderId)
                    .collection("proposals")
                    .orderBy("proposalDate")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val proposals = snapshot.documents.mapNotNull {
                            it.toObject(ProposalDelivery::class.java)?.apply { docId = it.id }
                        }
                        uiState = uiState.copy(selectedOrder = order, proposals = proposals, isLoading = false)
                    }
                    .addOnFailureListener { e ->
                        uiState = uiState.copy(error = e.message, isLoading = false)
                    }
            }
            .addOnFailureListener { e ->
                uiState = uiState.copy(error = e.message, isLoading = false)
            }
    }

    fun acceptProposal(orderId: String, proposalDocId: String) {
        val proposalRef = db.collection("orders")
            .document(orderId)
            .collection("proposals")
            .document(proposalDocId)

        proposalRef.update("confirmed", true)
            .addOnSuccessListener {
                val newDate = uiState.proposals.find { it.docId == proposalDocId }?.newDate
                if (newDate != null) {
                    db.collection("orders").document(orderId)
                        .update(
                            mapOf(
                                "accept" to OrderState.ACEITE.name,
                                "surveyDate" to newDate
                            )
                        )
                        .addOnSuccessListener {
                            val delivery = hashMapOf(
                                "orderId" to orderId,
                                "deliveryDate" to newDate,
                                "status" to "PENDENTE",
                                "createdAt" to Date()
                            )
                            db.collection("deliveries").add(delivery)
                                .addOnSuccessListener { fetchOrder(orderId) }
                                .addOnFailureListener { e ->
                                    uiState = uiState.copy(error = e.message)
                                }
                        }
                        .addOnFailureListener { e ->
                            uiState = uiState.copy(error = e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                uiState = uiState.copy(error = e.message)
            }
    }

    fun proposeNewDate(orderId: String, newDate: Date) {
        val proposal = hashMapOf(
            "newDate" to newDate,
            "proposalDate" to Date(),
            "proposedBy" to auth.currentUser?.uid,
            "confirmed" to false
        )

        db.collection("orders")
            .document(orderId)
            .collection("proposals")
            .add(proposal)
            .addOnSuccessListener { fetchOrder(orderId) }
            .addOnFailureListener { e -> uiState = uiState.copy(error = e.message) }
    }
}
