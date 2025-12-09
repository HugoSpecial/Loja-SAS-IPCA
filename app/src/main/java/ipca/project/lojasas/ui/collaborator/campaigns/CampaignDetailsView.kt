package ipca.project.lojasas.ui.collaborator.campaigns

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Donation
import ipca.project.lojasas.ui.components.InfoRow
import ipca.project.lojasas.ui.components.SectionTitle
import java.text.SimpleDateFormat
import java.util.Locale

val IpcaGreen = Color(0xFF438C58)
val BackgroundColor = Color(0xFFF5F6F8)
val TextDark = Color(0xFF2D2D2D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsView(
    navController: NavController,
    campaignId: String,
    viewModel: CampaignDetailsViewModel = viewModel()
) {
    val uiState = viewModel.uiState.value
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var selectedDonation by remember { mutableStateOf<Donation?>(null) }

    LaunchedEffect(campaignId) {
        viewModel.initialize(campaignId)
    }

    if (selectedDonation != null) {
        DonationProductsDialog(
            donation = selectedDonation!!,
            onDismiss = { selectedDonation = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = IpcaGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Cabeçalho IPCA SAS",
                    modifier = Modifier.heightIn(max = 55.dp).align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = IpcaGreen)
            }
            else if (uiState.campaign != null) {
                val campaign = uiState.campaign

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionTitle("Detalhes da Campanha")
                    InfoRow("Nome:", campaign.name)
                    InfoRow("Tipo:", campaign.campaignType.name)

                    val startStr = campaign.startDate?.let { dateFormat.format(it) } ?: "N/A"
                    val endStr = campaign.endDate?.let { dateFormat.format(it) } ?: "N/A"

                    InfoRow("Início:", startStr)
                    InfoRow("Fim:", endStr)

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Doações Recebidas (${uiState.donations.size})")
                    InfoRow("Total angariado:", "${uiState.totalCollected} un")

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.donations.isEmpty()) {
                        Text(
                            text = "Ainda não existem doações nesta campanha.",
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.donations) { donation ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    modifier = Modifier.clickable { selectedDonation = donation }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (donation.anonymous) "Anónimo" else (donation.name ?: "Sem Nome"),
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                            donation.donationDate?.let { date ->
                                                Text(
                                                    text = dateFormat.format(date),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = "Ver produtos...",
                                                fontSize = 10.sp,
                                                color = IpcaGreen
                                            )
                                        }

                                        Surface(
                                            color = IpcaGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "+${donation.quantity}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = IpcaGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
            else if (uiState.error != null) {
                Text(
                    text = "Erro: ${uiState.error}",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun DonationProductsDialog(
    donation: Donation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Produtos Doados", fontWeight = FontWeight.Bold, color = IpcaGreen) },
        text = {
            Column {
                Text("Doador: ${if (donation.anonymous) "Anónimo" else donation.name}", fontSize = 14.sp, color = Color.Gray)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(donation.products) { product ->
                        val prodQty = product.batches.sumOf { it.quantity }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${product.name}", fontWeight = FontWeight.SemiBold)
                            Text("$prodQty un", fontWeight = FontWeight.Bold, color = IpcaGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Total da Doação: ${donation.quantity} itens", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}