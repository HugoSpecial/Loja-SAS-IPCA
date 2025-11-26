package ipca.project.lojasas.ui.colaborator.campaigns

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.CampaignType
import java.util.Date

enum class TimeFilter {
    ATIVAS,
    FUTURAS,
    PASSADAS
}

data class CampaignsState(
    val allCampaigns: List<Campaign> = emptyList(),
    val filteredCampaigns: List<Campaign> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,

    val timeFilter: TimeFilter = TimeFilter.ATIVAS,
    val typeFilter: CampaignType? = null
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

        listener = db.collection("campaigns")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val campaignsList = mutableListOf<Campaign>()

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val campaign = doc.toObject(Campaign::class.java)
                        if (campaign != null) {
                            campaign.docId = doc.id

                            val start = doc.getDate("startDate")
                            val end = doc.getDate("endDate")
                            if (start != null) campaign.startDate = start
                            if (end != null) campaign.endDate = end

                            // Correção para ler INTERNO/EXTERNO
                            val typeStr = doc.getString("campaignType")
                            if (typeStr != null) {
                                try {
                                    campaign.campaignType = CampaignType.valueOf(typeStr)
                                } catch (_: Exception) {
                                    // Se falhar, assume padrão (INTERNO)
                                    campaign.campaignType = CampaignType.INTERNO
                                }
                            }

                            campaignsList.add(campaign)
                        }
                    } catch (e: Exception) {
                        Log.e("CampaignsVM", "Erro converter", e)
                    }
                }

                val currentState = uiState.value.copy(
                    allCampaigns = campaignsList,
                    isLoading = false
                )
                uiState.value = currentState
                reapplyFilters()
            }
    }

    fun setTimeFilter(filter: TimeFilter) {
        uiState.value = uiState.value.copy(timeFilter = filter)
        reapplyFilters()
    }

    fun setTypeFilter(type: CampaignType?) {
        val newType = if (uiState.value.typeFilter == type) null else type
        uiState.value = uiState.value.copy(typeFilter = newType)
        reapplyFilters()
    }

    private fun reapplyFilters() {
        val now = Date()
        val currentState = uiState.value
        val all = currentState.allCampaigns

        var filtered = when (currentState.timeFilter) {
            TimeFilter.PASSADAS -> all.filter { it.endDate.before(now) }
            TimeFilter.ATIVAS -> all.filter { !it.startDate.after(now) && !it.endDate.before(now) }
            TimeFilter.FUTURAS -> all.filter { it.startDate.after(now) }
        }

        if (currentState.typeFilter != null) {
            filtered = filtered.filter { it.campaignType == currentState.typeFilter }
        }

        uiState.value = currentState.copy(filteredCampaigns = filtered)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}