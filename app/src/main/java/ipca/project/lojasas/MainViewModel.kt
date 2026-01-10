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

    // Estado para controlar a navegação
    var shouldLogout = mutableStateOf(false)
        private set

    init {
        startListeningToUserStatus()
    }

    private fun startListeningToUserStatus() {
        val uid = auth.currentUser?.uid ?: return

        // Remove listener anterior se existir
        userListener?.remove()

        // Ouve o documento do utilizador em tempo real
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val isBeneficiary = snapshot.getBoolean("isBeneficiary") ?: false

                    // Se o utilizador deixar de ser beneficiário e estiver logado
                    if (!isBeneficiary) {
                        shouldLogout.value = true
                    }
                } else {
                    // Se o documento do utilizador for apagado (caso raro)
                    shouldLogout.value = true
                }
            }
    }

    // Função para limpar o estado após o logout ser tratado na UI
    fun onLogoutHandled() {
        shouldLogout.value = false
        // Opcional: fazer o signOut do Firebase aqui também
        auth.signOut()
        userListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}