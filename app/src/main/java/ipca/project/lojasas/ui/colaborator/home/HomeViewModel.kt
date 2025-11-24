package ipca.project.lojasas.ui.colaborator.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth // Necessário para saber quem está logado
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.EstadoCandidatura

data class HomeState(
    val candidatures: List<Candidatura> = emptyList(),
    val pendingCount: Int = 0,
    val userName: String = "", // <--- NOVO CAMPO: Nome do utilizador
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth // <--- Instância da Auth

    var uiState = mutableStateOf(HomeState())
        private set

    init {
        fetchData()
        fetchUserName() // <--- Chamamos a função para buscar o nome
    }

    // --- NOVA FUNÇÃO: Busca o nome do utilizador ---
    private fun fetchUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        // Atualizamos o estado com o nome obtido
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

                val list = mutableListOf<Candidatura>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val c = Candidatura()
                        c.docId = doc.id
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
                        Log.e("HomeViewModel", "Erro ao ler doc", e)
                    }
                }

                val count = list.count { it.estado == EstadoCandidatura.PENDENTE }

                uiState.value = uiState.value.copy(
                    candidatures = list,
                    pendingCount = count,
                    isLoading = false,
                    error = null
                )
            }
    }
}