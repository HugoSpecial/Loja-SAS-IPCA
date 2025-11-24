package ipca.project.lojasas.ui.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ipca.example.lojasas.models.Candidatura

// Estado da UI
data class AwaitCandidatureState(
    val candidatura: Candidatura? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AwaitCandidatureViewModel : ViewModel() {

    var uiState = mutableStateOf(AwaitCandidatureState())
        private set

    // Variável para guardar a "escuta" em tempo real
    private var listenerRegistration: ListenerRegistration? = null

    init {
        startRealtimeUpdates()
    }

    fun startRealtimeUpdates() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Buscar o ID da candidatura no User (Isto pode ser uma leitura única)
                val userDoc = db.collection("users").document(uid).get().await()
                val candId = userDoc.getString("candidatureId")

                if (!candId.isNullOrEmpty()) {

                    // 2. LIGAR O MODO TEMPO REAL NA CANDIDATURA
                    // Se já houver um listener ativo, removemos para não duplicar
                    listenerRegistration?.remove()

                    listenerRegistration = db.collection("candidatures")
                        .document(candId)
                        .addSnapshotListener { snapshot, e ->

                            // Se der erro na conexão
                            if (e != null) {
                                Log.e("AwaitCandVM", "Erro no realtime", e)
                                uiState.value = uiState.value.copy(error = e.message, isLoading = false)
                                return@addSnapshotListener
                            }

                            // Se recebermos dados
                            if (snapshot != null && snapshot.exists()) {
                                val candObj = snapshot.toObject(Candidatura::class.java)
                                candObj?.docId = snapshot.id

                                // ATUALIZA O ESTADO AUTOMATICAMENTE
                                uiState.value = uiState.value.copy(
                                    isLoading = false,
                                    candidatura = candObj,
                                    error = null
                                )
                            } else {
                                uiState.value = uiState.value.copy(
                                    isLoading = false,
                                    error = "Candidatura não encontrada."
                                )
                            }
                        }

                } else {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Nenhuma candidatura associada."
                    )
                }
            } catch (e: Exception) {
                Log.e("AwaitCandVM", "Erro ao buscar User", e)
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${e.message}")
            }
        }
    }



    // Função para ativar o aluno como beneficiário
    fun ativarBeneficiario(onSuccess: () -> Unit) {
        val user = Firebase.auth.currentUser
        if (user == null) return

        val db = Firebase.firestore

        // Atualiza o campo isBeneficiary no documento do user
        db.collection("users").document(user.uid)
            .update("isBeneficiary", true)
            .addOnSuccessListener {
                onSuccess() // Chama o callback para navegar
            }
            .addOnFailureListener { e ->
                // Opcional: Tratar erro
                uiState.value = uiState.value.copy(error = "Erro ao ativar conta: ${e.message}")
            }
    }


    // Importante: Quando o utilizador sai deste ecrã, paramos de escutar o Firebase
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }

    fun retry() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)
        startRealtimeUpdates()
    }
}