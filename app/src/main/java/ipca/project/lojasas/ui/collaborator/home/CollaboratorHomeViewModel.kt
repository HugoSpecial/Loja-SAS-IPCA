package ipca.project.lojasas.ui.collaborator.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import java.util.Calendar
import java.util.Date

data class CollaboratorHomeState(
    val candidatures: List<Candidature> = emptyList(),
    val pendingCount: Int = 0,              // Candidaturas pendentes
    val pendingSolicitationsCount: Int = 0, // Pedidos pendentes
    val deliveriesTodayCount: Int = 0,      // Entregas pendentes para HOJE
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
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

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

                // Atualiza estado e remove loading
                uiState.value = uiState.value.copy(
                    candidatures = list,
                    pendingCount = count,
                    isLoading = false
                )
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

                var countPendenteHoje = 0
                val hoje = Date() // Data atual do telemóvel

                for (doc in value?.documents ?: emptyList()) {
                    try {
                        // 1. Ler o Estado
                        val estadoStr = doc.getString("state")
                        val state = if (estadoStr != null) {
                            try { DeliveryState.valueOf(estadoStr) } catch (e: Exception) { DeliveryState.PENDENTE }
                        } else DeliveryState.PENDENTE

                        // 2. Ler a Data do campo "surveyDate"
                        val timestamp = doc.getTimestamp("surveyDate")
                        val surveyDate = timestamp?.toDate()

                        // 3. Verificação: É Pendente E é no dia de Hoje (ignora horas)?
                        if (state == DeliveryState.PENDENTE && surveyDate != null) {
                            if (isSameDay(surveyDate, hoje)) {
                                countPendenteHoje++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Erro ao ler delivery", e)
                    }
                }

                uiState.value = uiState.value.copy(deliveriesTodayCount = countPendenteHoje)
            }

        // --- 4. CAMPANHAS (CAMPAIGNS) ---
        db.collection("campaigns")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val now = Date() // Data e hora atual
                var activeCount = 0

                for (doc in value?.documents ?: emptyList()) {
                    try {
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
    }

    /**
     * Função auxiliar para comparar duas datas ignorando as horas/minutos.
     * Verifica apenas se o Ano e o Dia do Ano são iguais.
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()

        cal1.time = date1
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}