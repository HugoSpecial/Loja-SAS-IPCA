package ipca.project.lojasas.ui.collaborator.orders

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.R
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.OrderItem
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
fun OrderDetailView(
    navController: NavController,
    orderId: String,
    viewModel: OrderDetailViewModel = viewModel()
) {
    val state = viewModel.uiState.value

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var showReasonError by remember { mutableStateOf(false) }

    var isDateChange by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(orderId) { viewModel.fetchOrder(orderId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(modifier = Modifier.weight(1f).padding(end = 48.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Cabeçalho SAS",
                    modifier = Modifier.heightIn(max = 55.dp).align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(text = state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                state.order != null -> {
                    val order = state.order
                    val latestProposal = state.proposals.firstOrNull()
                    val isFinalState = order.accept != OrderState.PENDENTE
                    val canApprove = !isFinalState && (state.proposals.isEmpty() || latestProposal?.confirmed == true)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OrderStatusBadge(state = order.accept)

                        if (order.accept == OrderState.REJEITADA && !order.rejectReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("Motivo da rejeição:", order.rejectReason!!)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("Dados do beneficiário")
                        InfoRow("Nome:", state.userName ?: "N/A")
                        InfoRow("Telemóvel:", state.userPhone ?: "N/A")
                        InfoRow("Observações:", state.userNotes ?: "N/A")
                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle("Produtos solicitados")
                        if (order.items.isEmpty()) {
                            Text("Nenhum produto solicitado.", fontWeight = FontWeight.Bold)
                        } else {
                            ProductCategoryList(order.items, state.products, isFinalState)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        SectionTitle("Submissão")
                        InfoRow("Data do pedido", order.orderDate?.let { SimpleDateFormat("d 'de' MMM 'de' yyyy, HH:mm", Locale("pt", "PT")).format(it) } ?: "-")
                        InfoRow("Data da entrega", order.surveyDate?.let { SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("pt", "PT")).format(it) } ?: "-")
                        Spacer(modifier = Modifier.height(24.dp))

                        if (state.proposals.isNotEmpty()) {
                            SectionTitle("Histórico de Negociação")
                            state.proposals.forEach { proposal ->
                                CollaboratorProposalCard(proposal, viewModel, orderId)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        SectionTitle("Avaliação")
                        InfoRow("Avaliado por:", state.evaluatorName ?: "-")
                        InfoRow("Data da avaliação:", order.evaluationDate?.let { SimpleDateFormat("d 'de' MMM 'de' yyyy, HH:mm", Locale("pt", "PT")).format(it) } ?: "-")
                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isFinalState) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { showRejectDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rejeitar / Data", fontWeight = FontWeight.Bold)
                                }

                                if (canApprove) {
                                    Button(
                                        onClick = { viewModel.approveOrder(order.docId ?: "") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Aprovar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        }
    }

    if (showRejectDialog) {
        RejectDialogWithDate(
            reason = rejectReason, isDateChange = isDateChange, selectedDate = selectedDate,
            onReasonChange = { rejectReason = it; showReasonError = false },
            onDateChangeToggle = { isDateChange = it; if (it) rejectReason = "" },
            onDateSelected = { selectedDate = it }, onDismiss = { showRejectDialog = false },
            onConfirm = {
                if (!isDateChange && rejectReason.isBlank()) { showReasonError = true; return@RejectDialogWithDate }
                viewModel.rejectOrProposeDate(orderId, if (isDateChange) "" else rejectReason, selectedDate?.let { Date(it) })
                showRejectDialog = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CollaboratorProposalCard(proposal: ProposalDelivery, viewModel: OrderDetailViewModel, orderId: String) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser?.uid
    val proposals = viewModel.uiState.value.proposals

    val isNegotiationClosed = proposals.any { it.confirmed == true }
    val latestPendingProposal = proposals.filter { it.confirmed == false }.maxByOrNull { it.proposalDate?.time ?: 0L }

    val showButtons = !isNegotiationClosed && proposal.confirmed == false && proposal.proposedBy != currentUser && proposal == latestPendingProposal

    var showDatePicker by remember { mutableStateOf(false) }
    // Estado para armazenar a data selecionada no diálogo de "Mudar"
    var selectedLocalDate by remember { mutableStateOf(proposal.newDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (proposal.confirmed == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val proposalDateStr = proposal.proposalDate?.let { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("pt", "PT")).format(it) } ?: "--"
            val newDateStr = proposal.newDate?.let { SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT")).format(it) } ?: "--"
            val sender = if (proposal.proposedBy == currentUser) "Colaborador" else (viewModel.uiState.value.userName ?: "Beneficiário")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("De: $sender em: $proposalDateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                if (proposal.confirmed == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("DATA ACEITE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Nova data sugerida: $newDateStr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (proposal.confirmed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

            if (proposal.confirmed == true && proposal.proposedBy == currentUser) {
                Text("O beneficiário aceitou esta data.", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }

            if (showButtons) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { viewModel.acceptProposal(orderId, proposal.docId ?: "") }, modifier = Modifier.height(40.dp).weight(1f)) {
                        Text("Aceitar", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { showDatePicker = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.height(40.dp).weight(1f)) {
                        Text("Mudar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DO BOTÃO MUDAR ---
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateAsDate = Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    viewModel.rejectOrProposeDate(orderId, "", dateAsDate)
                    showDatePicker = false
                }) { Text("Confirmar Contra-proposta", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
            text = {
                Column {
                    Text("Selecione uma nova data para sugerir:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DynamicCalendarView(
                        displayedYearMonth = YearMonth.from(selectedLocalDate),
                        selectedDate = selectedLocalDate,
                        onMonthChange = { /* opcional */ },
                        onDateSelected = { selectedLocalDate = it }
                    )
                }
            }
        )
    }
}

@Composable
private fun ProductStockRow(orderItem: OrderItem, product: Product, isFinal: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            val imageBitmap = remember(product.imageUrl) {
                try {
                    if (!product.imageUrl.isNullOrBlank()) {
                        val decodedBytes = Base64.decode(product.imageUrl, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                    } else null
                } catch (e: Exception) { null }
            }
            if (imageBitmap != null) Image(imageBitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${orderItem.quantity}x ${orderItem.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            if (!isFinal) {
                val validStock = product.batches.filter { it.quantity > 0 && isDateValid(it.validity) }.sumOf { it.quantity }
                Text("Stock: $validStock | Após: ${validStock - (orderItem.quantity ?: 0)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun ProductCategoryList(orderItems: List<OrderItem>, allProducts: List<Product>, isFinal: Boolean) {
    allProducts.groupBy { it.category }.forEach { (category, productsInCategory) ->
        val requested = orderItems.filter { item -> productsInCategory.any { it.name == item.name } }
        if (requested.isEmpty()) return@forEach
        Text(category, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(vertical = 8.dp))
        requested.forEach { ProductStockRow(it, productsInCategory.find { p -> p.name == it.name }!!, isFinal) }
    }
}

fun isDateValid(date: Date?): Boolean {
    if (date == null) return false
    val today = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
    val check = java.util.Calendar.getInstance().apply { time = date; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
    return !check.before(today.time)
}

@Composable
fun OrderStatusBadge(state: OrderState) {
    val color = when (state) { OrderState.PENDENTE -> Color(0xFFEF6C00); OrderState.ACEITE -> MaterialTheme.colorScheme.primary; OrderState.REJEITADA -> MaterialTheme.colorScheme.error }
    StatusBadge(state.name, color.copy(alpha = 0.1f), color)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectDialogWithDate(reason: String, isDateChange: Boolean, selectedDate: Long?, onReasonChange: (String) -> Unit, onDateChangeToggle: (Boolean) -> Unit, onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var selectedLocalDate by remember { mutableStateOf(selectedDate?.let { Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDateSelected(if (isDateChange) Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).time else null); onConfirm() }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = {
            Column {
                if (!isDateChange) {
                    Text("Motivo da rejeição", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = reason, onValueChange = onReasonChange, modifier = Modifier.fillMaxWidth())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDateChange, onCheckedChange = onDateChangeToggle)
                    Text("Propor nova data?")
                }
                if (isDateChange) {
                    DynamicCalendarView(YearMonth.now(), selectedLocalDate, onMonthChange = {}, onDateSelected = { selectedLocalDate = it })
                }
            }
        }
    )
}