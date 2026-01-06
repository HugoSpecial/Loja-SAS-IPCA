package ipca.project.lojasas.ui.beneficiary.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.ProposalDelivery
import java.util.Date

// Estado da UI
data class BeneficiaryOrderState(
    val isLoading: Boolean = false,
    val selectedOrder: Order? = null,
    val error: String? = null,
    val proposals: List<ProposalDelivery> = emptyList(),
    val currentUserName: String? = null // Nome do Beneficiário logado
)

class BeneficiaryOrderViewModel : ViewModel() {

    var uiState by mutableStateOf(BeneficiaryOrderState())
        private set

    private val db = Firebase.firestore
    private var proposalsListener: ListenerRegistration? = null

    fun fetchOrder(orderId: String) {
        uiState = uiState.copy(isLoading = true)

        fetchCurrentUserName()

        db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    uiState = uiState.copy(isLoading = false, error = e.message)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val order = snapshot.toObject(Order::class.java)
                    order?.docId = snapshot.id

                    uiState = uiState.copy(selectedOrder = order, isLoading = false)

                    listenToProposals(orderId)
                } else {
                    uiState = uiState.copy(isLoading = false, error = "Pedido não encontrado")
                }
            }
    }

    private fun listenToProposals(orderId: String) {
        proposalsListener?.remove() // Limpa anterior se existir
        proposalsListener = db.collection("orders").document(orderId)
            .collection("proposals")
            .orderBy("proposalDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    val proposals = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ProposalDelivery::class.java)?.apply {
                            docId = doc.id
                        }
                    }
                    uiState = uiState.copy(proposals = proposals)
                }
            }
    }

    private fun fetchCurrentUserName() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener {
            uiState = uiState.copy(currentUserName = it.getString("name"))
        }
    }

    // Propor nova data (Contra-proposta)
    fun proposeNewDate(orderId: String, newDate: Date) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val proposal = ProposalDelivery(
            newDate = newDate,
            proposedBy = uid,
            proposalDate = Date(),
            confirmed = false
        )

        db.collection("orders").document(orderId)
            .collection("proposals")
            .add(proposal)
    }

    // Aceitar proposta do Colaborador
    fun acceptProposal(orderId: String, proposalId: String) {
        val proposal = uiState.proposals.find { it.docId == proposalId } ?: return
        val newDate = proposal.newDate ?: return

        val batch = db.batch()
        val orderRef = db.collection("orders").document(orderId)
        val proposalRef = orderRef.collection("proposals").document(proposalId)

        // Atualiza a data oficial e marca proposta como confirmada
        batch.update(orderRef, "surveyDate", newDate)
        batch.update(proposalRef, "confirmed", true)

        batch.commit()
            .addOnFailureListener { e ->
                uiState = uiState.copy(error = e.message)
            }
    }

    override fun onCleared() {
        super.onCleared()
        proposalsListener?.remove()
    }
}