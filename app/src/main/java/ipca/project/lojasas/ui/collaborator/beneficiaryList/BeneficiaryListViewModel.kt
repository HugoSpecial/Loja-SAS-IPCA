package ipca.project.lojasas.ui.collaborator.beneficiaries

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.User

data class BeneficiaryListState(
    val beneficiaries: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BeneficiaryListViewModel : ViewModel() {

    var uiState = mutableStateOf(BeneficiaryListState())
        private set

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchBeneficiaries()
    }

    private fun fetchBeneficiaries() {
        uiState.value = uiState.value.copy(isLoading = true)

        // Filtra utilizadores onde isBeneficiary == true
        db.collection("users")
            .whereEqualTo("isBeneficiary", true)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<User>()
                for (doc in result) {
                    try {
                        val user = doc.toObject(User::class.java)
                        user.docId = doc.id
                        list.add(user)
                    } catch (e: Exception) {
                        Log.e("BeneficiaryVM", "Erro ao converter: ${e.message}")
                    }
                }

                val sortedList = list.sortedBy { it.name ?: "" }

                uiState.value = uiState.value.copy(
                    beneficiaries = sortedList,
                    isLoading = false
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
    }
}