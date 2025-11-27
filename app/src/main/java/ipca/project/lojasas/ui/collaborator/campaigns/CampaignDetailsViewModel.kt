package ipca.project.lojasas.ui.collaborator.campaigns

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.Donation

data class CampaignDetailsState(
    val campaign: Campaign? = null,
    val donations: List<Donation> = emptyList(), // Lista de doações desta campanha
    val totalCollected: Int = 0,                 // Total de itens angariados
    val isLoading: Boolean = false,
    val error: String? = null
)

class CampaignDetailsViewModel : ViewModel() {

    var uiState = mutableStateOf(CampaignDetailsState())
        private set

    private val db = FirebaseFirestore.getInstance()

    // Chama esta função no LaunchedEffect da View
    fun initialize(campaignId: String) {
        uiState.value = uiState.value.copy(isLoading = true)
        fetchCampaignDetails(campaignId)
        fetchCampaignDonations(campaignId)
    }

    // 1. Busca os detalhes da Campanha (Nome, Datas, Tipo)
    private fun fetchCampaignDetails(campaignId: String) {
        db.collection("campaigns").document(campaignId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val campaign = document.toObject(Campaign::class.java)
                    campaign?.docId = document.id

                    // Atualiza o estado mantendo o loading true até as doações carregarem também,
                    // ou false se preferires mostrar logo a campanha
                    uiState.value = uiState.value.copy(campaign = campaign)
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Campanha não encontrada")
                }
            }
            .addOnFailureListener { exception ->
                uiState.value = uiState.value.copy(isLoading = false, error = exception.message)
            }
    }

    // 2. Busca as doações associadas a esta campanha em Tempo Real
    private fun fetchCampaignDonations(campaignId: String) {
        db.collection("donations")
            .whereEqualTo("campaignId", campaignId) // Filtra pelo ID da campanha
            .orderBy("donationDate", Query.Direction.DESCENDING) // Ordena por data (mais recente primeiro)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("CampaignDetailsVM", "Erro ao ler doações", error)
                    uiState.value = uiState.value.copy(isLoading = false) // Para o loading mesmo com erro
                    return@addSnapshotListener
                }

                val donationList = mutableListOf<Donation>()
                var totalItems = 0

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val donation = doc.toObject(Donation::class.java)
                        if (donation != null) {
                            donation.docId = doc.id
                            donationList.add(donation)
                            totalItems += donation.quantity // Soma a quantidade desta doação
                        }
                    } catch (e: Exception) {
                        Log.e("CampaignDetailsVM", "Erro ao converter doação", e)
                    }
                }

                // Atualiza o estado com a lista e o total
                uiState.value = uiState.value.copy(
                    donations = donationList,
                    totalCollected = totalItems,
                    isLoading = false, // Dados carregados
                    error = null
                )
            }
    }
}