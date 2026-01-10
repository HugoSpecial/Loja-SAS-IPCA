package ipca.project.lojasas.ui.beneficiary.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.ui.components.InfoRow
import ipca.project.lojasas.ui.components.SectionTitle
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DeliveryDetailView(
    navController: NavController,
    orderId: String,
    notificationId: String,
    viewModel: DeliveryDetailViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.fetchNotificationData(notificationId)
        viewModel.checkExistingNote(orderId)
        viewModel.fetchDeliveryData(orderId) // Busca dados da entrega
    }

    DeliveryDetailContent(
        state = state,
        onBackClick = { navController.popBackStack() },
        onNoteChange = { viewModel.onUserNoteChange(it) },
        onSaveClick = { note -> viewModel.saveDeliveryNote(orderId, note) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailContent(
    state: DeliveryDetailState,
    onBackClick: () -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onSaveClick: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Detalhes do Levantamento", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- NOTIFICAÇÃO ---
                if (state.notificationTitle.isNotEmpty() || state.notificationBody.isNotEmpty()) {
                    SectionTitle("Notificação")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.notificationTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.notificationBody, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // --- ESTADO DA ENTREGA (Badge no topo, igual pedido) ---
                state.delivery?.let { delivery ->
                    DeliveryStatusBadge(delivery.state)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- INFORMAÇÕES DA ENTREGA (estilo Submissão) ---
                    SectionTitle("Informações do Levantamento")
                    InfoRow("Entregue", if (delivery.delivered) "Sim" else "Não")
                    if (!delivery.reason.isNullOrEmpty()) InfoRow("Motivo", delivery.reason!!)
                    delivery.surveyDate?.let { InfoRow("Data do Levantamento", SimpleDateFormat("d MMM yyyy", Locale("pt")).format(it)) }
                    if (!delivery.evaluatedBy.isNullOrEmpty()) InfoRow("Avaliado por", delivery.evaluatedBy!!)
                    delivery.evaluationDate?.let { InfoRow("Data de Avaliação", SimpleDateFormat("d MMM yyyy", Locale("pt")).format(it)) }
                    if (!delivery.justificationStatus.isNullOrEmpty()) InfoRow("Status da Justificação", delivery.justificationStatus!!)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- OBSERVAÇÃO / RESPOSTA ---
                SectionTitle("Observação/Resposta")
                OutlinedTextField(
                    value = state.userNote,
                    onValueChange = onNoteChange,
                    enabled = !state.isSaved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- BOTÃO ENVIAR ---
                if (!state.isSaved) {
                    Button(
                        onClick = { onSaveClick(state.userNote) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Enviar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (state.isSaved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enviado com sucesso!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(state.error ?: "Erro", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
private fun DeliveryStatusBadge(state: DeliveryState) {
    val mainColor = when (state) {
        DeliveryState.PENDENTE -> Color(0xFFFFA000)
        DeliveryState.ENTREGUE -> MaterialTheme.colorScheme.primary
        DeliveryState.CANCELADO -> MaterialTheme.colorScheme.error
        DeliveryState.EM_ANALISE -> Color(0xFF0288D1)
    }

    StatusBadge(
        label = state.name,
        backgroundColor = mainColor.copy(alpha = 0.1f),
        contentColor = mainColor
    )
}

