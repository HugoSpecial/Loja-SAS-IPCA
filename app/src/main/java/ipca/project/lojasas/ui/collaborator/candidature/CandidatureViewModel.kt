package ipca.project.lojasas.ui.collaborator.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.Type // Certifica-te que importas o Enum Type

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

                        // Campos de Texto
                        c.course = doc.getString("course") ?: doc.getString("curso")
                        c.email = doc.getString("email") ?: ""
                        c.mobilePhone = doc.getString("mobilePhone") ?: doc.getString("telemovel") ?: ""
                        c.academicYear = doc.getString("academicYear") ?: doc.getString("anoLetivo") ?: ""
                        c.birthDate = doc.getString("birthDate") ?: doc.getString("dataNascimento") ?: ""

                        // --- DATAS (Adicionado evaluationDate) ---
                        c.creationDate = doc.getDate("creationDate") ?: doc.getDate("dataCriacao")
                        c.updateDate = doc.getDate("updateDate") ?: doc.getDate("dataAtualizacao")

                        // IMPORTANTE: Ler a data de avaliação para a View mostrar corretamente
                        c.evaluationDate = doc.getDate("evaluationDate") ?: doc.getDate("dataAvaliacao")

                        // --- TIPO (LICENCIATURA, MESTRADO, ETC) ---
                        val typeStr = doc.getString("type") ?: doc.getString("tipo")
                        if (typeStr != null) {
                            try {
                                c.type = Type.valueOf(typeStr)
                            } catch (e: Exception) {
                                c.type = null // Se der erro, fica null e a View mostra "GERAL"
                            }
                        }

                        // --- ESTADO ---
                        val estadoStr = doc.getString("state") ?: doc.getString("estado")
                        if (estadoStr != null) {
                            try {
                                c.state = CandidatureState.valueOf(estadoStr)
                            } catch (e: Exception) {
                                c.state = CandidatureState.PENDENTE
                            }
                        }

                        list.add(c)

                    } catch (e: Exception) {
                        Log.e("CandidatureVM", "Erro ao ler documento: ${doc.id}", e)
                    }
                }

                // Filtrar contagem de pendentes
                val pendentes = list.count { it.state == CandidatureState.PENDENTE }

                // Ordenar: Pendentes primeiro, depois pela data de criação (mais recentes primeiro)
                val sortedList = list.sortedWith(
                    compareBy<Candidature> { it.state != CandidatureState.PENDENTE } // False (Pendentes) vem antes de True
                        .thenByDescending { it.creationDate } // Do mais recente para o mais antigo
                )

                uiState.value = uiState.value.copy(
                    candidaturas = sortedList,
                    pendingCount = pendentes,
                    isLoading = false,
                    error = null
                )
            }
    }
}