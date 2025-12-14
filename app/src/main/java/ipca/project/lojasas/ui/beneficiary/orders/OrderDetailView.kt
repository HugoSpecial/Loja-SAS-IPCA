package ipca.project.lojasas.ui.beneficiary.orders

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.ProposalDelivery
import ipca.project.lojasas.ui.beneficiary.newBasket.DynamicCalendarView
import ipca.project.lojasas.ui.components.InfoRow
import ipca.project.lojasas.ui.components.SectionTitle
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

val BackgroundColor = Color(0xFFF5F6F8)
val TextGray = Color(0xFF8C8C8C)
val IpcaGreen = Color(0xFF2E7D32)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryOrderDetailView(
    navController: NavController,
    orderId: String,
    viewModel: BeneficiaryOrderViewModel = viewModel()
) {
    val state = viewModel.uiState

    // Busca o pedido ao iniciar
    LaunchedEffect(orderId) {
        viewModel.fetchOrder(orderId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Detalhes do Pedido",
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "Erro",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (state.selectedOrder != null) {
                val order = state.selectedOrder

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Estado do Pedido ---
                    OrderStatusBadge(order.accept)

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Produtos Solicitados ---
                    SectionTitle("Produtos solicitados")
                    if (order.items.isEmpty()) {
                        Text("Nenhum produto solicitado.", color = Color.Gray, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    } else {
                        ProductList(order.items)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Datas ---
                    SectionTitle("Submissão")
                    InfoRow(
                        "Data do pedido",
                        order.orderDate?.let {
                            SimpleDateFormat("d 'de' MMM 'de' yyyy, HH:mm", Locale("pt", "PT")).format(it)
                        } ?: "-"
                    )
                    InfoRow(
                        "Data da entrega",
                        order.surveyDate?.let {
                            SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("pt", "PT")).format(it)
                        } ?: "-"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Observações do Colaborador ---
                    if (!order.rejectReason.isNullOrBlank() && order.accept == OrderState.REJEITADA) {
                        SectionTitle("Observações")
                        InfoRow("Motivo da rejeição:", order.rejectReason!!)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // --- Propostas de Data ---
                    if (state.proposals.isNotEmpty()) {
                        SectionTitle("Propostas de data")
                        state.proposals.forEach { proposal ->
                            ProposalCard(proposal = proposal, viewModel = viewModel, orderId = orderId)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProposalCard(
    proposal: ProposalDelivery,
    viewModel: BeneficiaryOrderViewModel,
    orderId: String
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser?.uid

    val latestPendingProposal = viewModel.uiState.proposals
        .filter { it.confirmed == false }
        .maxByOrNull { it.proposalDate?.time ?: 0L }

    val showButtons = proposal.confirmed == false &&
            proposal.proposedBy != currentUser &&
            proposal == latestPendingProposal

    var showDatePicker by remember { mutableStateOf(false) }
    var displayedYearMonth by remember {
        mutableStateOf(
            proposal.newDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.let {
                YearMonth.of(it.year, it.monthValue)
            } ?: YearMonth.now()
        )
    }
    var selectedDate by remember {
        mutableStateOf(proposal.newDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now())
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val proposalDateStr = proposal.proposalDate?.let {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("pt", "PT")).format(it)
            } ?: "--"

            val newDateStr = proposal.newDate?.let {
                SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")).format(it)
            } ?: "--"

            Text("Proposta enviada em: $proposalDateStr", fontSize = 14.sp, color = TextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Nova data: $newDateStr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            if (showButtons) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.acceptProposal(orderId, proposal.docId ?: "") },
                        colors = ButtonDefaults.buttonColors(containerColor = IpcaGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Aceitar", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aceitar", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Recusar", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recusar / Nova Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.proposeNewDate(orderId, Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                        showDatePicker = false
                    }
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
            text = {
                Column {
                    DynamicCalendarView(
                        displayedYearMonth = displayedYearMonth,
                        selectedDate = selectedDate,
                        onMonthChange = { displayedYearMonth = it },
                        onDateSelected = { date -> selectedDate = date }
                    )
                }
            }
        )
    }
}

@Composable
fun OrderStatusBadge(state: OrderState) {
    val (bg, color) = when (state) {
        OrderState.PENDENTE -> Color(0xFFFFE0B2) to Color(0xFFEF6C00)
        OrderState.ACEITE -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        OrderState.REJEITADA -> Color(0xFFFFCDD2) to Color(0xFFC62828)
    }
    StatusBadge(label = state.name, backgroundColor = bg, contentColor = color)
}

@Composable
fun ProductList(items: List<OrderItem>) {
    items.forEach { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = item.name ?: "-", fontWeight = FontWeight.Bold)
                Text(text = "x${item.quantity}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
