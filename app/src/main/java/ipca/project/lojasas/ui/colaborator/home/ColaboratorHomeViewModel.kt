package ipca.project.lojasas.ui.colaborator.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidature
import ipca.example.lojasas.models.CandidatureState

data class ColaboratorHomeState(
    val candidatures: List<Candidature> = emptyList(),
    val pendingCount: Int = 0,
    val userName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ColaboratorHomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(ColaboratorHomeState())
        private set

    init {
        fetchData()
        fetchUserName()
    }

    private fun fetchUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        uiState.value = uiState.value.copy(userName = name)
                    }
                }
                .addOnFailureListener {
                    Log.e("HomeViewModel", "Erro ao obter nome", it)
                }
        }
    }

    private fun fetchData() {
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
                        Log.e("HomeViewModel", "Erro ao ler doc", e)
                    }
                }

                val count = list.count { it.state == CandidatureState.PENDENTE }

                uiState.value = uiState.value.copy(
                    candidatures = list,
                    pendingCount = count,
                    isLoading = false,
                    error = null
                )
            }
    }
}