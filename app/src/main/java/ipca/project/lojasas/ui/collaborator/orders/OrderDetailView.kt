package ipca.project.lojasas.ui.collaborator.orders


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.ui.beneficiary.newBasket.DynamicCalendarView
import ipca.project.lojasas.ui.collaborator.candidature.IpcaGreen
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

    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // --- HEADER ---
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
                    modifier = Modifier
                        .heightIn(max = 55.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = IpcaGreen
                )

                state.error != null -> Text(
                    text = state.error,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )

                state.order != null -> {
                    val order = state.order

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

                        // --- Dados do beneficiário ---
                        SectionTitle("Dados do beneficiário")
                        InfoRow("Nome:", state.userName ?: "N/A")
                        InfoRow("Telemóvel:", state.userPhone ?: "N/A")
                        InfoRow("Observações:", state.userNotes ?: "N/A")
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Produtos solicitados ---
                        SectionTitle("Produtos solicitados")
                        if (order.items.isEmpty()) {
                            Text(
                                "Nenhum produto solicitado.",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            ProductCategoryList(order.items, state.products, order.accept != OrderState.PENDENTE)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Submissão ---
                        SectionTitle("Submissão")
                        InfoRow(
                            "Data do pedido",
                            order.orderDate?.let {
                                SimpleDateFormat(
                                    "d 'de' MMM 'de' yyyy, HH:mm",
                                    Locale("pt", "PT")
                                ).format(it)
                            } ?: "-"
                        )
                        InfoRow(
                            "Data da entrega",
                            order.surveyDate?.let {
                                SimpleDateFormat(
                                    "d 'de' MMM 'de' yyyy",
                                    Locale("pt", "PT")
                                ).format(it)
                            } ?: "-"
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Avaliação ---
                        SectionTitle("Avaliação")
                        InfoRow("Avaliado por:", state.evaluatorName ?: "-")
                        InfoRow(
                            "Data da avaliação:",
                            order.evaluationDate?.let {
                                SimpleDateFormat(
                                    "d 'de' MMM 'de' yyyy, HH:mm",
                                    Locale("pt", "PT")
                                ).format(it)
                            } ?: "-"
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val isFinalState = order.accept != OrderState.PENDENTE
                        if (!isFinalState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showRejectDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rejeitar", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.approveOrder(order.docId ?: "") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Aprovar", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Este pedido encontra-se ${order.accept.name}",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        }
    }

    if (showRejectDialog) {
        RejectDialogWithDate(
            reason = rejectReason,
            isDateChange = isDateChange,
            selectedDate = selectedDate,
            onReasonChange = {
                rejectReason = it
                showReasonError = false
            },
            onDateChangeToggle = {
                isDateChange = it
                if (it) rejectReason = ""
            },
            onDateSelected = { selectedDate = it },
            onDismiss = {
                showRejectDialog = false
                showReasonError = false
            },
            onConfirm = {
                if (!isDateChange && rejectReason.isBlank()) {
                    showReasonError = true
                    return@RejectDialogWithDate
                }

                viewModel.rejectOrProposeDate(
                    orderId = orderId,
                    reason = if (isDateChange) "" else rejectReason,
                    proposedDate = selectedDate?.let { Date(it) }
                )

                showRejectDialog = false
            }
        )
    }

    if (showReasonError) {
        Text(
            text = "É obrigatório inserir um motivo para rejeição",
            color = Color.Red,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

// --- Componentes auxiliares ---

@Composable
private fun OrderStatusBadge(state: OrderState) {
    val (bg, color) = when (state) {
        OrderState.PENDENTE -> Color(0xFFFFE0B2) to Color(0xFFEF6C00)
        OrderState.ACEITE -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        OrderState.REJEITADA -> Color(0xFFFFCDD2) to Color(0xFFC62828)
    }
    StatusBadge(label = state.name, backgroundColor = bg, contentColor = color)
}

// --- Produtos ---
@Composable
private fun ProductCategoryList(orderItems: List<OrderItem>, allProducts: List<Product>, isFinal: Boolean) {
    val itemsByCategory = allProducts.groupBy { it.category }

    itemsByCategory.forEach { (category, productsInCategory) ->
        val requested = orderItems.filter { orderItem ->
            productsInCategory.any { it.name == orderItem.name }
        }
        if (requested.isEmpty()) return@forEach

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        requested.forEach { orderItem ->
            val product = productsInCategory.find { it.name == orderItem.name }
            if (product != null) ProductStockRow(orderItem, product, isFinal)
        }
    }
}

@Composable
fun ProductStockRow(orderItem: OrderItem, product: Product, isFinal: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isFinal) 16.dp else 32.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isFinal) {
                Text("${orderItem.quantity}x ${orderItem.name}", fontWeight = FontWeight.Bold)
            } else {
                val totalStock = product.batches.sumOf { it.quantity }
                val newStock = totalStock - (orderItem.quantity ?: 0)
                val icon = if (newStock >= 0) Icons.Default.Check else Icons.Default.Close
                val iconColor = if (newStock >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

                Text("${orderItem.quantity}x ${orderItem.name} (Stock: $totalStock)", fontWeight = FontWeight.Bold)
                Text("Stock atualizado: $newStock", fontSize = 13.sp, color = Color.DarkGray)
                Icon(icon, contentDescription = null, tint = iconColor)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectDialogWithDate(
    reason: String,
    isDateChange: Boolean,
    selectedDate: Long?,
    onReasonChange: (String) -> Unit,
    onDateChangeToggle: (Boolean) -> Unit,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var displayedYearMonth by remember {
        mutableStateOf(
            selectedDate?.let {
                Date(it).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .let { ld -> YearMonth.of(ld.year, ld.monthValue) }
            } ?: YearMonth.now()
        )
    }

    var selectedLocalDate by remember {
        mutableStateOf(
            selectedDate?.let {
                Date(it).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } ?: LocalDate.now()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = if (isDateChange) {
                    Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).time
                } else null
                onDateSelected(millis)
                onConfirm()
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Column {
                if (!isDateChange) {
                    Text("Motivo da rejeição", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        placeholder = { Text("Escreva o motivo...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDateChange,
                        onCheckedChange = onDateChangeToggle
                    )
                    Text("É para propor nova data?")
                }

                if (isDateChange) {
                    Spacer(Modifier.height(12.dp))
                    DynamicCalendarView(
                        displayedYearMonth = displayedYearMonth,
                        selectedDate = selectedLocalDate,
                        onMonthChange = { displayedYearMonth = it },
                        onDateSelected = { date -> selectedLocalDate = date }
                    )
                }
            }
        }
    )
}

