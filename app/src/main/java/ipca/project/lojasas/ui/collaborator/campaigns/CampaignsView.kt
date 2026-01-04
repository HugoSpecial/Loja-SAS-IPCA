package ipca.project.lojasas.ui.collaborator.campaigns

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.CampaignType
import ipca.project.lojasas.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CampaignsView(
    navController: NavController,
    viewModel: CampaignsViewModel = viewModel()
) {
    val uiState by viewModel.uiState

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("new-campaign") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Campanha")
            }
        },
        containerColor = MaterialTheme.colorScheme.background // Fundo Scaffold adaptável
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // --- CABEÇALHO ---
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
                        tint = MaterialTheme.colorScheme.primary,
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
                        contentDescription = "Logótipo",
                        modifier = Modifier
                            .heightIn(max = 55.dp)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gestão de campanhas",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Aqui pode consultar todas as campanhas.",
                    style = MaterialTheme.typography.bodyMedium,
                    // Texto cinza adaptável (Branco transparente no escuro)
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // FILTROS TEMPORAIS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton(
                        text = "Ativas",
                        isSelected = uiState.timeFilter == TimeFilter.ATIVAS,
                        onClick = { viewModel.setTimeFilter(TimeFilter.ATIVAS) }
                    )
                    FilterChipButton(
                        text = "Futuras",
                        isSelected = uiState.timeFilter == TimeFilter.FUTURAS,
                        onClick = { viewModel.setTimeFilter(TimeFilter.FUTURAS) }
                    )
                    FilterChipButton(
                        text = "Passadas",
                        isSelected = uiState.timeFilter == TimeFilter.PASSADAS,
                        onClick = { viewModel.setTimeFilter(TimeFilter.PASSADAS) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILTROS TIPO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton(
                        text = "Interno",
                        isSelected = uiState.typeFilter == CampaignType.INTERNO,
                        onClick = { viewModel.setTypeFilter(CampaignType.INTERNO) }
                    )

                    FilterChipButton(
                        text = "Externo",
                        isSelected = uiState.typeFilter == CampaignType.EXTERNO,
                        onClick = { viewModel.setTypeFilter(CampaignType.EXTERNO) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.filteredCampaigns.isEmpty()) {
                    EmptyState(
                        message = "Nenhuma campanha encontrada.",
                        icon = Icons.Outlined.DateRange
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.filteredCampaigns) { campaign ->
                            CampaignCard(
                                campaign = campaign,
                                onClick = { navController.navigate("campaign_details/${campaign.docId}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            // Selecionado: Verde. Não Selecionado: Surface (Branco/Cinza Escuro)
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            // Selecionado: Branco. Não Selecionado: Texto cinza adaptável
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        // Borda apenas se não estiver selecionado
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun CampaignCard(campaign: Campaign, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Branco ou Cinza Escuro
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Verde
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Data da Campanha
                Text(
                    text = "${dateFormat.format(campaign.startDate)} - ${dateFormat.format(campaign.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )

                // Chip de Tipo (Interno/Externo)
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), // Fundo cinza suave adaptável
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = campaign.campaignType.name,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}