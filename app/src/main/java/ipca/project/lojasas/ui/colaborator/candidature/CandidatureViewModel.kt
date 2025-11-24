package ipca.project.lojasas.ui.colaborator.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.EstadoCandidatura

data class CandidatureState(
    val candidaturas: List<Candidatura> = emptyList(),
    val pendingCount: Int = 0, // <--- NOVO CAMPO
    val isLoading: Boolean = false,
    val error: String? = null
)

class CandidatureViewModel : ViewModel() {

    private val db = Firebase.firestore

    var uiState = mutableStateOf(CandidatureState())
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

                val list = mutableListOf<Candidatura>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val c = Candidatura()
                        c.docId = doc.id
                        c.curso = doc.getString("curso")
                        c.email = doc.getString("email") ?: ""
                        c.telemovel = doc.getString("telemovel") ?: ""
                        c.anoLetivo = doc.getString("anoLetivo") ?: ""
                        c.dataNascimento = doc.getString("dataNascimento") ?: ""
                        c.dataCriacao = doc.getDate("dataCriacao")
                        c.dataAtualizacao = doc.getDate("dataAtualizacao")

                        val estadoStr = doc.getString("estado")
                        if (estadoStr != null) {
                            try {
                                c.estado = EstadoCandidatura.valueOf(estadoStr)
                            } catch (e: Exception) {
                                c.estado = EstadoCandidatura.PENDENTE
                            }
                        }
                        list.add(c)

                    } catch (e: Exception) {
                        Log.e("DEBUG_FIREBASE", "Erro ao ler documento: ${doc.id}", e)
                    }
                }

                val pendentes = list.count { it.estado == EstadoCandidatura.PENDENTE }

                uiState.value = uiState.value.copy(
                    candidaturas = list,
                    pendingCount = pendentes,
                    isLoading = false,
                    error = null
                )
            }
    }
}