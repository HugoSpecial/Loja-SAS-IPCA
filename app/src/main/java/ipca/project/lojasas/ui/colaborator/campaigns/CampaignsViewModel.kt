package ipca.project.lojasas.ui.colaborator.campaigns

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ipca.project.lojasas.models.Campaign

data class CampaignsState(
    val campaigns: List<Campaign> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CampaignsViewModel : ViewModel() {

    var uiState = mutableStateOf(CampaignsState())
        private set

    private var listener: ListenerRegistration? = null

    init {
        fetchCampaigns()
    }

    private fun fetchCampaigns() {
        uiState.value = uiState.value.copy(isLoading = true)
        val db = FirebaseFirestore.getInstance()

        listener = db.collection("campaigns") // <--- CONFIRMA SE O NOME É ESTE
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("CampaignsViewModel", "Erro de conexão: ${error.message}")
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (value == null || value.isEmpty) {
                    Log.d("CampaignsViewModel", "A coleção 'campaigns' está vazia ou não existe.")
                    uiState.value = uiState.value.copy(campaigns = emptyList(), isLoading = false)
                    return@addSnapshotListener
                }

                Log.d("CampaignsViewModel", "Encontrei ${value.size()} documentos.")

                val campaignsList = mutableListOf<Campaign>()

                for (doc in value) {
                    try {
                        Log.d("CampaignsViewModel", "A tentar converter documento: ${doc.id}")
                        Log.d("CampaignsViewModel", "Dados brutos: ${doc.data}")

                        val campaign = doc.toObject(Campaign::class.java)
                        campaign.docId = doc.id
                        campaignsList.add(campaign)

                        Log.d("CampaignsViewModel", "Sucesso: ${campaign.name}")

                    } catch (e: Exception) {
                        Log.e("CampaignsViewModel", "FALHA ao converter documento ${doc.id}", e)
                        // Dica: Vê a mensagem de erro aqui no Logcat
                    }
                }

                uiState.value = uiState.value.copy(
                    campaigns = campaignsList,
                    isLoading = false
                )
            }
    }

    // Limpar o listener quando sairmos do ecrã para poupar recursos
    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}