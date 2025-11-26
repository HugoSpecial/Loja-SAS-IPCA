package ipca.project.lojasas.ui.colaborator.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth // <--- Importante
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.CampaignType
import java.util.Date

data class NewCampaignState(
    var name: String = "",
    var startDate: Date = Date(),
    var endDate: Date = Date(),
    var campaignType: CampaignType = CampaignType.INTERNO,
    var isLoading: Boolean = false,
    var error: String? = null
)

class NewCampaignViewModel : ViewModel() {

    var uiState = mutableStateOf(NewCampaignState())
        private set

    fun onNameChange(newValue: String) {
        uiState.value = uiState.value.copy(name = newValue)
    }

    fun onStartDateChange(newValue: Date) {
        uiState.value = uiState.value.copy(startDate = newValue)
    }

    fun onEndDateChange(newValue: Date) {
        uiState.value = uiState.value.copy(endDate = newValue)
    }

    fun onTypeChange(newValue: CampaignType) {
        uiState.value = uiState.value.copy(campaignType = newValue)
    }

    fun addCampaign(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.name.isBlank()) {
            uiState.value = state.copy(error = "O nome da campanha é obrigatório.")
            return
        }
        if (state.endDate.before(state.startDate)) {
            uiState.value = state.copy(error = "A data de fim não pode ser anterior à data de início.")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            uiState.value = state.copy(error = "Erro: Utilizador não autenticado.")
            return
        }

        val currentUserId = currentUser.uid

        uiState.value = state.copy(isLoading = true, error = null)

        val db = FirebaseFirestore.getInstance()

        val campaignData = hashMapOf(
            "name" to state.name,
            "startDate" to state.startDate,
            "endDate" to state.endDate,
            "campaignType" to state.campaignType.name,
            "collaboratorId" to currentUserId
        )

        db.collection("campaigns")
            .add(campaignData)
            .addOnSuccessListener {
                uiState.value = state.copy(isLoading = false)
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = e.message ?: "Erro ao criar campanha.")
            }
    }
}