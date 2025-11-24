package ipca.project.lojasas.ui.colaborator.candidature

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.EstadoCandidatura
import java.util.Date

data class CandidatureDetailsState(
    val candidatura: Candidatura? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val operationSuccess: Boolean = false
)

class CandidatureDetailsViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(CandidatureDetailsState())
        private set

    fun fetchCandidature(docId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("candidatures").document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val item = snapshot.toObject(Candidatura::class.java)
                        item?.docId = snapshot.id
                        uiState.value = uiState.value.copy(
                            candidatura = item,
                            isLoading = false,
                            error = null
                        )
                    } catch (e: Exception) {
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao converter dados.")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Candidatura não encontrada.")
                }
            }
    }

    fun approveCandidature(candidatureId: String) {
        val currentCandidature = uiState.value.candidatura

        if (currentCandidature?.estado == EstadoCandidatura.ACEITE ||
            currentCandidature?.estado == EstadoCandidatura.REJEITADA) {
            uiState.value = uiState.value.copy(error = "Candidatura já finalizada. Não é possível alterar.")
            return
        }

        if (currentCandidature?.userId.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(error = "Erro: Candidatura sem User ID associado.")
            return
        }

        val studentId = currentCandidature!!.userId!!
        val colaboradorId = Firebase.auth.currentUser?.uid ?: ""

        uiState.value = uiState.value.copy(isLoading = true)

        val batch = db.batch()

        val candidatureRef = db.collection("candidatures").document(candidatureId)
        batch.update(candidatureRef, mapOf(
            "estado" to EstadoCandidatura.ACEITE,
            "dataAvaliacao" to Date(),
            "avaliadoPor" to colaboradorId
        ))

        // Adiciona a flag de beneficiário ao utilizador
        val userRef = db.collection("users").document(studentId)
        batch.update(userRef, "isBeneficiary", true)

        batch.commit()
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isLoading = false, operationSuccess = true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = "Falha ao aprovar: ${e.message}")
            }
    }

    fun rejectCandidature(docId: String, reason: String) {
        val currentCandidature = uiState.value.candidatura

        if (currentCandidature?.estado == EstadoCandidatura.ACEITE ||
            currentCandidature?.estado == EstadoCandidatura.REJEITADA) {
            uiState.value = uiState.value.copy(error = "Candidatura já finalizada. Não é possível alterar.")
            return
        }

        val colaboradorId = Firebase.auth.currentUser?.uid ?: ""

        val updates = hashMapOf<String, Any>(
            "estado" to EstadoCandidatura.REJEITADA,
            "motivoAlteracaoEstado" to reason,
            "dataAvaliacao" to Date(),
            "avaliadoPor" to colaboradorId
        )

        db.collection("candidatures").document(docId)
            .update(updates)
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isLoading = false, operationSuccess = true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }
}