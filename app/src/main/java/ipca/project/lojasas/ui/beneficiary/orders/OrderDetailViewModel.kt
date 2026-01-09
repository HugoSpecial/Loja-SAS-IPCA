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
import ipca.project.lojasas.models.Product // Certifica-te que tens este import
import ipca.project.lojasas.models.ProposalDelivery
import java.util.Date

// Estado da UI
data class BeneficiaryOrderState(
    val isLoading: Boolean = false,
    val selectedOrder: Order? = null,
    val error: String? = null,
    val proposals: List<ProposalDelivery> = emptyList(),
    val currentUserName: String? = null,
    val products: List<Product> = emptyList() // <--- NOVO: Lista de produtos para as imagens
)

class BeneficiaryOrderViewModel : ViewModel() {

    var uiState by mutableStateOf(BeneficiaryOrderState())
        private set

    private val db = Firebase.firestore
    private var proposalsListener: ListenerRegistration? = null

    init {
        // Buscar produtos assim que o ViewModel inicia para ter as imagens prontas
        fetchProducts()
    }

    // --- NOVA FUNÇÃO PARA BUSCAR PRODUTOS ---
    private fun fetchProducts() {
        db.collection("products").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            val list = value?.toObjects(Product::class.java) ?: emptyList()
            uiState = uiState.copy(products = list)
        }
    }

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

    fun acceptProposal(orderId: String, proposalId: String) {
        val proposal = uiState.proposals.find { it.docId == proposalId } ?: return
        val newDate = proposal.newDate ?: return

        val batch = db.batch()
        val orderRef = db.collection("orders").document(orderId)
        val proposalRef = orderRef.collection("proposals").document(proposalId)

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