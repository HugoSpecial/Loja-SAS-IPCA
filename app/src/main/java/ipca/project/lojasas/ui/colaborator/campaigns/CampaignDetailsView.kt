package ipca.project.lojasas.ui.colaborator.campaigns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsView(
    navController: NavController,
    campaignId: String, // Recebido via navegação
    viewModel: CampaignDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Carrega os dados assim que a view abre
    LaunchedEffect(campaignId) {
        viewModel.getCampaignDetails(campaignId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Campanha") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            else if (uiState.campaign != null) {
                val campaign = uiState.campaign!!

                Column(modifier = Modifier.padding(16.dp)) {

                    // --- CABEÇALHO DA CAMPANHA ---
                    Text(
                        text = campaign.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tipo: ${campaign.campaignType.name}")
                    Text("Início: ${dateFormat.format(campaign.startDate)}")
                    Text("Fim: ${dateFormat.format(campaign.endDate)}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // --- LISTA DE DOAÇÕES NESTA CAMPANHA ---
                    Text(
                        text = "Doações Recebidas (${campaign.donations.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (campaign.donations.isEmpty()) {
                        Text("Ainda não existem doações nesta campanha.", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(campaign.donations) { donation ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Text(
                                            text = if (donation.anonymous) "Anónimo" else (donation.name ?: "Sem Nome"),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Quantidade: ${donation.quantity}")
                                        // Se donation.donationDate não for nulo:
                                        donation.donationDate?.let { date ->
                                            Text(
                                                "Data: ${dateFormat.format(date)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (uiState.error != null) {
                Text("Erro: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}