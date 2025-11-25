package ipca.project.lojasas.ui.colaborator.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidature
import ipca.example.lojasas.models.CandidatureState

data class CandidatureListState(
    val candidaturas: List<Candidature> = emptyList(),
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CandidatureViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(CandidatureListState())
        private set

    init {
        fetchCandidatures()
    }

    private fun fetchCandidatures() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("candidatures")
            .addSnapshotListener { value, error ->

                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = mutableListOf<Candidature>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val c = Candidature()
                        c.docId = doc.id

                        // --- CORREÇÃO AQUI ---
                        // Tenta ler os nomes em Inglês primeiro. Se for null, lê os antigos em Português.

                        c.course = doc.getString("course") ?: doc.getString("curso")
                        c.email = doc.getString("email") ?: ""
                        c.mobilePhone = doc.getString("mobilePhone") ?: doc.getString("telemovel") ?: ""
                        c.academicYear = doc.getString("academicYear") ?: doc.getString("anoLetivo") ?: ""
                        c.birthDate = doc.getString("birthDate") ?: doc.getString("dataNascimento") ?: ""

                        // Datas
                        c.creationDate = doc.getDate("creationDate") ?: doc.getDate("dataCriacao")
                        c.updateDate = doc.getDate("updateDate") ?: doc.getDate("dataAtualizacao")

                        // ESTADO: Prioridade ao 'state' (novo), fallback para 'estado' (antigo)
                        val estadoStr = doc.getString("state") ?: doc.getString("estado")

                        if (estadoStr != null) {
                            try {
                                c.state = CandidatureState.valueOf(estadoStr)
                            } catch (e: Exception) {
                                // Se o nome no enum mudou (ex: PENDING vs PENDENTE), isto protege o crash
                                c.state = CandidatureState.PENDENTE
                            }
                        }
                        list.add(c)

                    } catch (e: Exception) {
                        Log.e("CandidatureVM", "Erro ao ler documento: ${doc.id}", e)
                    }
                }

                // Filtrar contagem pelo estado correto
                val pendentes = list.count { it.state == CandidatureState.PENDENTE }

                uiState.value = uiState.value.copy(
                    candidaturas = list,
                    pendingCount = pendentes,
                    isLoading = false,
                    error = null
                )
            }
    }
}