package ipca.project.lojasas.ui.collaborator.delivery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.PictureAsPdf // Ícone PDF
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.DeliveryState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DeliveryListView(
    navController: NavController,
    viewModel: DeliveryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val context = LocalContext.current // Necessário para gerar ficheiro
    var selectedFilter by remember { mutableStateOf<DeliveryState?>(null) }

    val filteredList = remember(state.deliveries, selectedFilter) {
        if (selectedFilter == null) state.deliveries
        else state.deliveries.filter { it.delivery.state == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- CABEÇALHO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão Voltar
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

            // Logótipo (Centrado com weight)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Cabeçalho IPCA SAS",
                    modifier = Modifier
                        .heightIn(max = 55.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }

            // --- BOTÃO GERAR PDF ---
            IconButton(
                onClick = {
                    if (!state.isGeneratingReport) {
                        viewModel.generateCurrentMonthReport(context)
                    }
                },
                enabled = !state.isGeneratingReport
            ) {
                if (state.isGeneratingReport) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = "Gerar Relatório Entregas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Título
            Text(
                text = "Gestão de Entregas",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            // Subtítulo
            Text(
                text = "Aqui pode consultar todas as entregas.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contador de pendentes
            val pendingColor = Color(0xFFEF6C00)
            Text(
                text = if (state.pendingCount == 1) "1 pendente" else "${state.pendingCount} pendentes",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (state.pendingCount > 0) pendingColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipButton(
                    text = "Todas",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterChipButton(
                    text = "Pendentes",
                    isSelected = selectedFilter == DeliveryState.PENDENTE,
                    onClick = { selectedFilter = DeliveryState.PENDENTE }
                )
                FilterChipButton(
                    text = "Entregues",
                    isSelected = selectedFilter == DeliveryState.ENTREGUE,
                    onClick = { selectedFilter = DeliveryState.ENTREGUE }
                )
                FilterChipButton(
                    text = "Não entregues",
                    isSelected = selectedFilter == DeliveryState.CANCELADO,
                    onClick = { selectedFilter = DeliveryState.CANCELADO }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de pedidos
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.deliveries.isEmpty() -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                    state.error != null -> Text(
                        text = state.error ?: "Erro",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    filteredList.isEmpty() -> Text(
                        text = "Não existem pedidos neste filtro.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredList) { delivery ->
                            DeliveryCard(delivery = delivery, onClick = {
                                navController.navigate("delivery_details/${delivery.delivery.docId}")
                            })
                        }
                    }
                }
            }
        }
    }
}

// ... (FilterChipButton, DeliveryCard e StatusBadgeOrder mantêm-se iguais ao que tinhas) ...
// Vou incluir aqui para garantir que tens o ficheiro completo funcional

@Composable
fun FilterChipButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DeliveryCard(delivery: DeliveryWithUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entrega",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadgeOrder(delivery.delivery.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Beneficiário: ${delivery.userName ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            Text(
                text = "Data: ${delivery.delivery.surveyDate?.let { dateFormat.format(it) } ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatusBadgeOrder(state: DeliveryState) {
    val mainColor = when(state) {
        DeliveryState.PENDENTE -> Color(0xFFEF6C00)
        DeliveryState.ENTREGUE -> MaterialTheme.colorScheme.primary
        DeliveryState.CANCELADO -> MaterialTheme.colorScheme.error
        DeliveryState.EM_ANALISE -> Color(0xFF00BCD4)
    }

    Surface(
        color = mainColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = state.name.replace("_", " "),
            color = mainColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold
        )
    }
}