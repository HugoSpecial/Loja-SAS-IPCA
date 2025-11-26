package ipca.project.lojasas.ui.colaborator.donation

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Donation

data class DonationListState(
    val donations: List<Donation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DonationListViewModel : ViewModel() {

    var uiState = mutableStateOf(DonationListState())
        private set

    private val db = Firebase.firestore

    init {
        fetchDonations()
    }

    private fun fetchDonations() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("donations")
            .orderBy("donationDate", Query.Direction.DESCENDING) // Ordena por mais recente
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = mutableListOf<Donation>()
                for (doc in value?.documents ?: emptyList()) {
                    try {
                        val donation = doc.toObject(Donation::class.java)
                        if (donation != null) {
                            donation.docId = doc.id
                            list.add(donation)
                        }
                    } catch (e: Exception) {
                        Log.e("DonationListVM", "Erro ao ler doação", e)
                    }
                }

                uiState.value = uiState.value.copy(
                    donations = list,
                    isLoading = false,
                    error = null
                )
            }
    }
}