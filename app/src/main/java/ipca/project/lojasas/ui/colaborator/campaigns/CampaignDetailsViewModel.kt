package ipca.project.lojasas.ui.colaborator.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Campaign

data class CampaignDetailsState(
    val campaign: Campaign? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CampaignDetailsViewModel : ViewModel() {

    var uiState = mutableStateOf(CampaignDetailsState())
        private set

    // Esta função deve ser chamada quando a View abre
    fun getCampaignDetails(campaignId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        val db = FirebaseFirestore.getInstance()

        db.collection("campaigns").document(campaignId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val campaign = document.toObject(Campaign::class.java)
                    campaign?.docId = document.id
                    uiState.value = uiState.value.copy(
                        campaign = campaign,
                        isLoading = false
                    )
                } else {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Campanha não encontrada"
                    )
                }
            }
            .addOnFailureListener { exception ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = exception.message
                )
            }
    }
}