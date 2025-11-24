package ipca.project.lojasas.ui.colaborator.candidature

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidature
import ipca.example.lojasas.models.CandidatureState
import java.util.Date

data class CandidatureDetailsState(
    val candidature: Candidature? = null,
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
                        val item = snapshot.toObject(Candidature::class.java)
                        item?.docId = snapshot.id
                        uiState.value = uiState.value.copy(
                            candidature = item, // Nome corrigido
                            isLoading = false,
                            error = null
                        )
                    } catch (e: Exception) {
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao converter: ${e.message}")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Candidatura não encontrada.")
                }
            }
    }

    fun approveCandidature(candidatureId: String) {
        val currentCandidature = uiState.value.candidature

        // Verifica se já está finalizada (usando .state)
        if (currentCandidature?.state == CandidatureState.ACEITE ||
            currentCandidature?.state == CandidatureState.REJEITADA) {
            uiState.value = uiState.value.copy(error = "Candidatura já finalizada.")
            return
        }

        if (currentCandidature?.userId.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(error = "Erro: User ID em falta.")
            return
        }

        val studentId = currentCandidature!!.userId!!
        val colaboradorId = Firebase.auth.currentUser?.uid ?: ""

        uiState.value = uiState.value.copy(isLoading = true)
        val batch = db.batch()

        // Atualizar estado na BD (atenção: na BD o campo deve chamar-se "state" agora, ou "estado" se usaste @PropertyName)
        // Se apagaste a BD antiga e criaste nova, usa "state".
        val candidatureRef = db.collection("candidatures").document(candidatureId)

        // Estamos a gravar como 'state' e 'evaluationDate' (Inglês)
        batch.update(candidatureRef, mapOf(
            "state" to CandidatureState.ACEITE,
            "evaluationDate" to Date(),
            "evaluatedBy" to colaboradorId
        ))

        val userRef = db.collection("users").document(studentId)
        batch.update(userRef, "isBeneficiary", true)

        batch.commit()
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isLoading = false, operationSuccess = true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    fun rejectCandidature(docId: String, reason: String) {
        val currentCandidature = uiState.value.candidature

        if (currentCandidature?.state == CandidatureState.ACEITE ||
            currentCandidature?.state == CandidatureState.REJEITADA) {
            uiState.value = uiState.value.copy(error = "Candidatura já finalizada.")
            return
        }

        val colaboradorId = Firebase.auth.currentUser?.uid ?: ""

        // Campos em Inglês
        val updates = hashMapOf<String, Any>(
            "state" to CandidatureState.REJEITADA,
            "statusChangeReason" to reason,
            "evaluationDate" to Date(),
            "evaluatedBy" to colaboradorId
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