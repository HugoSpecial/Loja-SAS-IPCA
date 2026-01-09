package ipca.project.lojasas.ui.beneficiary.orders

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.Product
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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryOrderDetailView(
    navController: NavController,
    orderId: String,
    viewModel: BeneficiaryOrderViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(orderId) {
        viewModel.fetchOrder(orderId)
    }

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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Detalhes do Pedido",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "Erro",
                    color = MaterialTheme.colorScheme.error,
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
                    OrderStatusBadge(order.accept)
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionTitle("Produtos solicitados")
                    if (order.items.isEmpty()) {
                        Text("Nenhum produto.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    } else {
                        // AGORA 'state.products' JÁ EXISTE NO VIEWMODEL
                        ProductList(order.items, state.products)
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Submissão")
                    InfoRow("Data do pedido", order.orderDate?.let { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("pt")).format(it) } ?: "-")
                    InfoRow("Data da entrega", order.surveyDate?.let { SimpleDateFormat("d MMM yyyy", Locale("pt")).format(it) } ?: "-")
                    Spacer(modifier = Modifier.height(24.dp))

                    if (!order.rejectReason.isNullOrBlank() && order.accept == OrderState.REJEITADA) {
                        SectionTitle("Observações")
                        InfoRow("Motivo:", order.rejectReason!!)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // --- PROPOSTAS ---
                    if (state.proposals.isNotEmpty()) {
                        SectionTitle("Negociação de Data")
                        state.proposals.forEach { proposal ->
                            BeneficiaryProposalCard(proposal = proposal, viewModel = viewModel, orderId = orderId)
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
fun BeneficiaryProposalCard(
    proposal: ProposalDelivery,
    viewModel: BeneficiaryOrderViewModel,
    orderId: String
) {
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid

    val myName = viewModel.uiState.currentUserName ?: "Eu"
    val otherName = "Colaborador"

    val isNegotiationClosed = viewModel.uiState.proposals.any { it.confirmed == true }

    val latestPendingProposal = viewModel.uiState.proposals
        .filter { it.confirmed == false }
        .maxByOrNull { it.proposalDate?.time ?: 0L }

    val showButtons = !isNegotiationClosed &&
            proposal.confirmed == false &&
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val proposalDateStr = proposal.proposalDate?.let { SimpleDateFormat("dd MMM, HH:mm", Locale("pt")).format(it) } ?: "--"
            val newDateStr = proposal.newDate?.let { SimpleDateFormat("dd MMM yyyy", Locale("pt")).format(it) } ?: "--"

            val proposedByLabel = if (proposal.proposedBy == currentUser) myName else otherName

            Text("Proposta de $proposedByLabel em: $proposalDateStr", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nova data: $newDateStr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (proposal.confirmed == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = "Aceite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(" (Aceite)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showButtons) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.acceptProposal(orderId, proposal.docId ?: "") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aceitar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contra-propor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.proposeNewDate(orderId, Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                        showDatePicker = false
                    }
                ) { Text("Enviar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.error) }
            },
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
    val mainColor = when (state) {
        OrderState.PENDENTE -> Color(0xFFEF6C00)
        OrderState.ACEITE -> MaterialTheme.colorScheme.primary
        OrderState.REJEITADA -> MaterialTheme.colorScheme.error
    }
    StatusBadge(label = state.name, backgroundColor = mainColor.copy(alpha = 0.1f), contentColor = mainColor)
}

@Composable
fun ProductList(items: List<OrderItem>, allProducts: List<Product>) {
    items.forEach { item ->
        val product = allProducts.find { it.name == item.name }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lado Esquerdo: Imagem + Nome
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Caixa da Imagem
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageBitmap = remember(product?.imageUrl) {
                            try {
                                if (!product?.imageUrl.isNullOrBlank()) {
                                    val decodedBytes = Base64.decode(product!!.imageUrl, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                                } else null
                            } catch (e: Exception) { null }
                        }

                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = item.name ?: "-",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Lado Direito: Quantidade
                Text(
                    text = "x${item.quantity}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}