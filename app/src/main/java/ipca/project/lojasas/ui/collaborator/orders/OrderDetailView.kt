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
// Certifica-te que estes imports correspondem à localização correta no teu projeto
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

    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    color = MaterialTheme.colorScheme.primary
                )

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
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
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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

                        // --- Propostas de Data (NEGOCIAÇÃO) ---
                        if (state.proposals.isNotEmpty()) {
                            SectionTitle("Histórico de Negociação")
                            state.proposals.forEach { proposal ->
                                CollaboratorProposalCard(
                                    proposal = proposal,
                                    viewModel = viewModel,
                                    orderId = orderId
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

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

                        // Botões Principais
                        if (!isFinalState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showRejectDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rejeitar", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.approveOrder(order.docId ?: "") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Aprovar", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Este pedido encontra-se ${order.accept.name}",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

// --- Componentes auxiliares ---

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CollaboratorProposalCard(
    proposal: ProposalDelivery,
    viewModel: OrderDetailViewModel,
    orderId: String
) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser?.uid

    val proposals = viewModel.uiState.value.proposals
    val latestPendingProposal = proposals
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
        mutableStateOf(
            proposal.newDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?: LocalDate.now()
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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

            val beneficiaryName = viewModel.uiState.value.userName ?: "Beneficiário"
            val collaboratorName = viewModel.uiState.value.currentCollaboratorName ?: "Colaborador"

            val proposedByLabel = if (proposal.proposedBy == currentUser) {
                collaboratorName
            } else {
                beneficiaryName
            }

            Text(
                "Proposta enviada por $proposedByLabel em: $proposalDateStr",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Nova data sugerida: $newDateStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (showButtons) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.acceptProposal(orderId, proposal.docId ?: "") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Aceitar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aceitar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Contra-propor")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mudar", fontWeight = FontWeight.Bold)
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
                        val dateAsDate = Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        viewModel.rejectOrProposeDate(orderId, reason = "", proposedDate = dateAsDate)
                        showDatePicker = false
                    }
                ) { Text("Enviar Contra-proposta", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column {
                    Text("Selecione uma nova data para sugerir ao beneficiário:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
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
private fun OrderStatusBadge(state: OrderState) {
    val mainColor = when (state) {
        OrderState.PENDENTE -> Color(0xFFEF6C00)
        OrderState.ACEITE -> MaterialTheme.colorScheme.primary
        OrderState.REJEITADA -> MaterialTheme.colorScheme.error
    }

    StatusBadge(
        label = state.name,
        backgroundColor = mainColor.copy(alpha = 0.1f),
        contentColor = mainColor
    )
}

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
            Text(
                category,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        requested.forEach { orderItem ->
            val product = productsInCategory.find { it.name == orderItem.name }
            if (product != null) ProductStockRow(orderItem, product, isFinal)
        }
    }
}

// --- FUNÇÃO CORRIGIDA COM IMAGEM ---
@Composable
private fun ProductStockRow(orderItem: OrderItem, product: Product, isFinal: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isFinal) 16.dp else 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. IMAGEM DO PRODUTO ---
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Tenta carregar imagem
            val imageBitmap = remember(product.imageUrl) {
                try {
                    if (!product.imageUrl.isNullOrBlank()) {
                        val decodedBytes = Base64.decode(product.imageUrl, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Ícone de fallback
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- 2. INFO TEXTO ---
        Column(modifier = Modifier.weight(1f)) {
            if (isFinal) {
                Text(
                    "${orderItem.quantity}x ${orderItem.name}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                // --- CÁLCULO DE STOCK VÁLIDO ---
                val validStock = product.batches
                    .filter { it.quantity > 0 && isDateValid(it.validity) }
                    .sumOf { it.quantity }

                val newStock = validStock - (orderItem.quantity ?: 0)

                Text(
                    "${orderItem.quantity}x ${orderItem.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    "Stock Atual: $validStock | Após: $newStock",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        if (!isFinal) {
            val validStock = product.batches
                .filter { it.quantity > 0 && isDateValid(it.validity) }
                .sumOf { it.quantity }
            val newStock = validStock - (orderItem.quantity ?: 0)
            val icon = if (newStock >= 0) Icons.Default.Check else Icons.Default.Close
            val iconColor = if (newStock >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            Spacer(modifier = Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = iconColor)
        }
    }
}

private fun isDateValid(date: Date?): Boolean {
    if (date == null) return false
    val today = java.util.Calendar.getInstance()
    today.set(java.util.Calendar.HOUR_OF_DAY, 0)
    today.set(java.util.Calendar.MINUTE, 0)
    today.set(java.util.Calendar.SECOND, 0)
    today.set(java.util.Calendar.MILLISECOND, 0)

    val checkDate = java.util.Calendar.getInstance()
    checkDate.time = date
    checkDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
    checkDate.set(java.util.Calendar.MINUTE, 0)
    checkDate.set(java.util.Calendar.SECOND, 0)
    checkDate.set(java.util.Calendar.MILLISECOND, 0)

    return !checkDate.before(today)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectDialogWithDate(
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
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = {
                val millis = if (isDateChange) {
                    Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).time
                } else null
                onDateSelected(millis)
                onConfirm()
            }) {
                Text("Confirmar", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        text = {
            Column {
                if (!isDateChange) {
                    Text(
                        "Motivo da rejeição",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        placeholder = { Text("Escreva o motivo...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDateChange,
                        onCheckedChange = onDateChangeToggle,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("Propor nova data?", color = MaterialTheme.colorScheme.onSurface)
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