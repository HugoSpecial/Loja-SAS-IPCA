package ipca.project.lojasas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    // Estado para controlar a navegação (Logout forçado)
    var shouldLogout = mutableStateOf(false)
        private set

    init {
        startListeningToUserStatus()
    }

    private fun startListeningToUserStatus() {
        val uid = auth.currentUser?.uid ?: return

        userListener?.remove()

        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val isBeneficiary = snapshot.getBoolean("isBeneficiary") ?: false
                    val isCollaborator = snapshot.getBoolean("isCollaborator") ?: false
                    val candidatureId = snapshot.getString("candidatureId")

                    if (isCollaborator) return@addSnapshotListener

                    if (isBeneficiary) return@addSnapshotListener

                    if (!candidatureId.isNullOrBlank()) return@addSnapshotListener

                    shouldLogout.value = true

                } else {
                    shouldLogout.value = true
                }
            }
    }

    fun onLogoutHandled() {
        shouldLogout.value = false
        auth.signOut()
        userListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}