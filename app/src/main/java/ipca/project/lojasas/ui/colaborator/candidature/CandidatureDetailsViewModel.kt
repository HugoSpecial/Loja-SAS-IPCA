package ipca.project.lojasas.ui.colaborator.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.DocumentAttachment
import ipca.project.lojasas.models.Type
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
                        val c = Candidature()
                        c.docId = snapshot.id
                        c.userId = snapshot.getString("userId")

                        // --- MAPEAMENTO MANUAL (INGLÊS -> fallback PORTUGUÊS) ---
                        // Isto garante que os dados aparecem mesmo que a BD tenha nomes antigos

                        // Dados Pessoais
                        c.email = snapshot.getString("email") ?: ""
                        c.mobilePhone = snapshot.getString("mobilePhone") ?: snapshot.getString("telemovel") ?: ""
                        c.birthDate = snapshot.getString("birthDate") ?: snapshot.getString("dataNascimento") ?: ""

                        // Dados Académicos
                        c.course = snapshot.getString("course") ?: snapshot.getString("curso")
                        c.academicYear = snapshot.getString("academicYear") ?: snapshot.getString("anoLetivo") ?: ""
                        c.cardNumber = snapshot.getString("cardNumber") ?: snapshot.getString("numeroCartao") ?: ""

                        // Tipo (Enum)
                        val typeStr = snapshot.getString("type") ?: snapshot.getString("tipo")
                        if (typeStr != null) {
                            try { c.type = Type.valueOf(typeStr) } catch (e: Exception) { c.type = null }
                        }

                        // Produtos (Booleans)
                        c.foodProducts = snapshot.getBoolean("foodProducts") ?: snapshot.getBoolean("produtosAlimentares") ?: false
                        c.hygieneProducts = snapshot.getBoolean("hygieneProducts") ?: snapshot.getBoolean("produtosHigiene") ?: false
                        c.cleaningProducts = snapshot.getBoolean("cleaningProducts") ?: snapshot.getBoolean("produtosLimpeza") ?: false

                        // Apoios / Socioeconómico
                        c.faesSupport = snapshot.getBoolean("faesSupport") ?: snapshot.getBoolean("faesApoiado")
                        c.scholarshipSupport = snapshot.getBoolean("scholarshipSupport") ?: snapshot.getBoolean("bolsaApoio")
                        c.scholarshipDetails = snapshot.getString("scholarshipDetails") ?: snapshot.getString("detalhesBolsa") ?: ""

                        // Declarações
                        c.truthfulnessDeclaration = snapshot.getBoolean("truthfulnessDeclaration") ?: snapshot.getBoolean("declaracaoVeracidade") ?: false
                        c.dataAuthorization = snapshot.getBoolean("dataAuthorization") ?: snapshot.getBoolean("autorizacaoDados") ?: false

                        // Finalização
                        c.signature = snapshot.getString("signature") ?: snapshot.getString("assinatura") ?: ""
                        c.signatureDate = snapshot.getString("signatureDate") ?: snapshot.getString("dataAssinatura") ?: ""

                        // Estado e Avaliação
                        val stateStr = snapshot.getString("state") ?: snapshot.getString("estado")
                        if (stateStr != null) {
                            try { c.state = CandidatureState.valueOf(stateStr) } catch (e: Exception) { c.state = CandidatureState.PENDENTE }
                        }

                        c.statusChangeReason = snapshot.getString("statusChangeReason") ?: snapshot.getString("motivoAlteracaoEstado")
                        c.evaluationDate = snapshot.getDate("evaluationDate") ?: snapshot.getDate("dataAvaliacao")
                        c.evaluatedBy = snapshot.getString("evaluatedBy") ?: snapshot.getString("avaliadoPor")

                        // --- ANEXOS (Lista de Objetos) ---
                        // Tenta ler "attachments", se falhar tenta "anexos"
                        val attachmentsList = snapshot.get("attachments") as? List<Map<String, String>>
                            ?: snapshot.get("anexos") as? List<Map<String, String>>

                        if (attachmentsList != null) {
                            val listDocs = mutableListOf<DocumentAttachment>()
                            for (map in attachmentsList) {
                                val doc = DocumentAttachment(
                                    name = map["name"] ?: map["nome"] ?: "",
                                    base64 = map["base64"] ?: ""
                                )
                                listDocs.add(doc)
                            }
                            c.attachments = listDocs
                        }

                        uiState.value = uiState.value.copy(
                            candidature = c,
                            isLoading = false,
                            error = null
                        )
                    } catch (e: Exception) {
                        Log.e("DetailsVM", "Erro no parse", e)
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao ler dados: ${e.message}")
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Candidatura não encontrada.")
                }
            }
    }

    fun approveCandidature(candidatureId: String) {
        val currentCandidature = uiState.value.candidature

        if (currentCandidature?.state == CandidatureState.ACEITE ||
            currentCandidature?.state == CandidatureState.REJEITADA) {
            uiState.value = uiState.value.copy(error = "Candidatura já finalizada.")
            return
        }

        if (currentCandidature?.userId.isNullOrEmpty()) {
            // Tenta obter, caso não tenha vindo no objeto, mas geralmente devia vir
            uiState.value = uiState.value.copy(error = "Erro: User ID em falta.")
            return
        }

        val studentId = currentCandidature!!.userId!!
        val colaboradorId = Firebase.auth.currentUser?.uid ?: ""

        uiState.value = uiState.value.copy(isLoading = true)
        val batch = db.batch()

        val candidatureRef = db.collection("candidatures").document(candidatureId)

        // Atualizar estado e quem avaliou
        batch.update(candidatureRef, mapOf(
            "state" to CandidatureState.ACEITE.name, // Guarda como String igual ao Enum
            "evaluationDate" to Date(),
            "evaluatedBy" to colaboradorId
        ))

        // Atualizar perfil do aluno para Beneficiário
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

        val updates = hashMapOf<String, Any>(
            "state" to CandidatureState.REJEITADA.name,
            "statusChangeReason" to reason,
            "evaluationDate" to Date(),
            "evaluatedBy" to colaboradorId
        )

        uiState.value = uiState.value.copy(isLoading = true)

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