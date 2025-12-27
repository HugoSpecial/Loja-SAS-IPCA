package ipca.project.lojasas.ui.collaborator.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import java.util.Date

data class CollaboratorHomeState(
    val candidatures: List<Candidature> = emptyList(),
    val pendingCount: Int = 0,              // Candidaturas pendentes
    val pendingSolicitationsCount: Int = 0, // Pedidos pendentes
    val deliveriesTodayCount: Int = 0,      // Entregas pendentes (Simulando "Hoje")
    val activeCampaignsCount: Int = 0,      // Campanhas ativas
    val userName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class CollaboratorHomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(CollaboratorHomeState())
        private set

    init {
        fetchData()
        fetchUserName()
    }

    private fun fetchUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        uiState.value = uiState.value.copy(userName = name)
                    }
                }
                .addOnFailureListener {
                    Log.e("HomeViewModel", "Erro ao obter nome", it)
                }
        }
    }

    private fun fetchData() {
        uiState.value = uiState.value.copy(isLoading = true)

        // --- 1. CANDIDATURAS ---
        db.collection("candidatures")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val list = value?.documents?.mapNotNull { doc ->
                    try {
                        val c = Candidature()
                        c.docId = doc.id
                        val estadoStr = doc.getString("state")
                        c.state = if (estadoStr != null) {
                            try { CandidatureState.valueOf(estadoStr) } catch (e: Exception) { CandidatureState.PENDENTE }
                        } else CandidatureState.PENDENTE
                        c
                    } catch (e: Exception) { null }
                } ?: emptyList()

                val count = list.count { it.state == CandidatureState.PENDENTE }
                uiState.value = uiState.value.copy(candidatures = list, pendingCount = count)
            }

        // --- 2. PEDIDOS (ORDERS) ---
        db.collection("orders")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val list = value?.documents?.mapNotNull { doc ->
                    try {
                        val o = Order()
                        o.docId = doc.id
                        val estadoStr = doc.getString("accept")
                        o.accept = if (estadoStr != null) {
                            try { OrderState.valueOf(estadoStr) } catch (e: Exception) { OrderState.PENDENTE }
                        } else OrderState.PENDENTE
                        o
                    } catch (e: Exception) { null }
                } ?: emptyList()

                val count = list.count { it.accept == OrderState.PENDENTE }
                uiState.value = uiState.value.copy(pendingSolicitationsCount = count)
            }

        // --- 3. ENTREGAS (DELIVERY) ---
        db.collection("delivery")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                var countPendente = 0
                for (doc in value?.documents ?: emptyList()) {
                    try {
                        // Lendo o estado
                        val estadoStr = doc.getString("state")
                        val state = if (estadoStr != null) {
                            try { DeliveryState.valueOf(estadoStr) } catch (e: Exception) { DeliveryState.PENDENTE }
                        } else DeliveryState.PENDENTE

                        // Lógica: Como o modelo Delivery não tem data, contamos as PENDENTES
                        // Se tivesse data: verificar se doc.getDate("date") == hoje
                        if (state == DeliveryState.PENDENTE) {
                            countPendente++
                        }

                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Erro ao ler delivery", e)
                    }
                }
                uiState.value = uiState.value.copy(deliveriesTodayCount = countPendente)
            }

        // --- 4. CAMPANHAS (CAMPAIGNS) ---
        db.collection("campaigns")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val now = Date() // Data e hora atual
                var activeCount = 0

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        // Firestore armazena datas como Timestamp, precisamos converter
                        val start = doc.getTimestamp("startDate")?.toDate()
                        val end = doc.getTimestamp("endDate")?.toDate()

                        if (start != null && end != null) {
                            // Verifica se AGORA está entre o inicio e o fim
                            if (now.after(start) && now.before(end)) {
                                activeCount++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Erro ao ler campaign", e)
                    }
                }
                uiState.value = uiState.value.copy(activeCampaignsCount = activeCount)
            }

        // Finaliza loading (simplificado, idealmente espera todos, mas snapshot é realtime)
        uiState.value = uiState.value.copy(isLoading = false)
    }
}