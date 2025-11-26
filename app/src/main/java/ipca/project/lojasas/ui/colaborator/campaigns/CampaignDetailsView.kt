package ipca.project.lojasas.ui.colaborator.campaigns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.models.Donation
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsView(
    navController: NavController,
    campaignId: String,
    viewModel: CampaignDetailsViewModel = viewModel()
) {
    val uiState = viewModel.uiState.value
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ESTADO PARA CONTROLAR O DIÁLOGO
    var selectedDonation by remember { mutableStateOf<Donation?>(null) }

    LaunchedEffect(campaignId) {
        viewModel.initialize(campaignId)
    }

    // --- MOSTRAR DIÁLOGO SE UMA DOAÇÃO FOR SELECIONADA ---
    if (selectedDonation != null) {
        DonationProductsDialog(
            donation = selectedDonation!!,
            onDismiss = { selectedDonation = null }
        )
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else if (uiState.campaign != null) {
                val campaign = uiState.campaign

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    // --- CABEÇALHO DA CAMPANHA ---
                    Text(
                        text = campaign.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Tipo: ${campaign.campaignType.name}")

                    val startStr = campaign.startDate?.let { dateFormat.format(it) } ?: "N/A"
                    val endStr = campaign.endDate?.let { dateFormat.format(it) } ?: "N/A"

                    Text("Início: $startStr")
                    Text("Fim: $endStr")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // --- CABEÇALHO DA LISTA ---
                    Text(
                        text = "Doações Recebidas (${uiState.donations.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Total angariado: ${uiState.totalCollected} un",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- LISTA DE DOAÇÕES ---
                    if (uiState.donations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ainda não existem doações nesta campanha.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.donations) { donation ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    // CLIQUE PARA ABRIR DETALHES
                                    modifier = Modifier.clickable { selectedDonation = donation }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (donation.anonymous) "Anónimo" else (donation.name ?: "Sem Nome"),
                                                fontWeight = FontWeight.Bold
                                            )
                                            donation.donationDate?.let { date ->
                                                Text(
                                                    text = dateFormat.format(date),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                            // Pequena indicação para clicar
                                            Text(
                                                text = "Ver produtos...",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "+${donation.quantity}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Text(
                    text = "Erro: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// --- NOVO COMPONENTE: DIÁLOGO DE PRODUTOS DA DOAÇÃO ---
@Composable
fun DonationProductsDialog(
    donation: Donation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Produtos Doados",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = "Doador: ${if (donation.anonymous) "Anónimo" else donation.name}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // LISTA DE PRODUTOS DENTRO DA DOAÇÃO
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp), // Altura máxima para não ocupar o ecrã todo
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(donation.products) { product ->
                        // Calcula o total deste produto específico (soma dos lotes)
                        val prodQty = product.batches.sumOf { it.quantity }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${product.name}", fontWeight = FontWeight.SemiBold)
                            Text("$prodQty un", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Total geral da doação
                Text(
                    text = "Total da Doação: ${donation.quantity} itens",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}