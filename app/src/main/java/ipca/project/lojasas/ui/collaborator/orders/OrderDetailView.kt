package ipca.project.lojasas.ui.collaborator.orders

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
import ipca.project.lojasas.models.ProductTest
import ipca.project.lojasas.ui.collaborator.candidature.IpcaGreen
import ipca.project.lojasas.ui.collaborator.candidature.RejectDialog
import java.text.SimpleDateFormat
import java.util.Locale

val BackgroundColor = Color(0xFFF5F6F8)
val TextDark = Color(0xFF2D2D2D)

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

                        StatusBadgeDetails(state = order.accept)

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
                            ProductCategoryList(order.items, state.products)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Submissão ---
                        SectionTitle("Submissão")
                        InfoRow(
                            "Data do pedido",
                            order.orderDate?.let {
                                SimpleDateFormat("d 'de' MMM 'de' yyyy, HH:mm", Locale("pt", "PT"))
                                    .format(it)
                            } ?: "-"
                        )
                        InfoRow(
                            "Data da entrega",
                            order.surveyDate?.let {
                                SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("pt", "PT"))
                                    .format(it)
                            } ?: "-"
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Avaliação ---
                        SectionTitle("Avaliação")
                        InfoRow("Avaliado por:", state.evaluatorName ?: "-")
                        InfoRow(
                            "Data da avaliação:",
                            order.evaluationDate?.let {
                                SimpleDateFormat("d 'de' MMM 'de' yyyy, HH:mm", Locale("pt", "PT"))
                                    .format(it)
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
        RejectDialog(
            reason = rejectReason,
            text = "Rejeitar pedido",
            onReasonChange = {
                rejectReason = it
                if (it.isNotBlank()) showReasonError = false
            },
            onDismiss = {
                showRejectDialog = false
                showReasonError = false
            },
            onConfirm = {
                if (rejectReason.isNotBlank()) {
                    viewModel.rejectOrder(orderId, rejectReason)
                    showRejectDialog = false
                    showReasonError = false
                } else {
                    showReasonError = true
                }
            }
        )

        if (showReasonError) {
            Text(
                text = "É obrigatório inserir um motivo para rejeição",
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

// --- Componentes visuais ---
@Composable
fun SectionTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IpcaGreen)
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
        Text(value.ifEmpty { "-" }, color = TextDark)
    }
}

@Composable
private fun StatusBadgeDetails(state: OrderState) {
    val (backgroundColor, contentColor) = when (state) {
        OrderState.PENDENTE -> Pair(Color(0xFFFFE0B2), Color(0xFFEF6C00))
        OrderState.ACEITE -> Pair(Color(0xFFC8E6C9), Color(0xFF2E7D32))
        OrderState.REJEITADA -> Pair(Color(0xFFFFCDD2), Color(0xFFC62828))
    }
    Surface(color = backgroundColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = state.name,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductCategoryList(orderItems: List<OrderItem>, allProducts: List<ProductTest>) {
    val itemsByCategory = allProducts.groupBy { it.category }

    itemsByCategory.forEach { (category, productsInCategory) ->
        val requested = orderItems.filter { orderItem ->
            productsInCategory.any { it.name == orderItem.name }
        }
        if (requested.isEmpty()) return@forEach

        val categoryIcon = if (requested.isNotEmpty()) Icons.Default.Check else null
        val categoryIconColor = Color(0xFF2E7D32)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (categoryIcon != null) {
                Icon(categoryIcon, contentDescription = null, tint = categoryIconColor)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(category, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        requested.forEach { orderItem ->
            val product = productsInCategory.find { it.name == orderItem.name }
            if (product != null) ProductStockRow(orderItem, product)
        }
    }
}

@Composable
fun ProductStockRow(orderItem: OrderItem, product: ProductTest) {
    val totalStock = product.batches.sumOf { it.quantity }
    val newStock = totalStock - (orderItem.quantity ?: 0)

    val icon = if (newStock >= 0) Icons.Default.Check else Icons.Default.Close
    val iconColor = if (newStock >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${orderItem.quantity}x ${orderItem.name} (Stock: $totalStock)",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stock atualizado: $newStock",
                fontSize = 13.sp,
                color = Color.DarkGray
            )
        }

        Icon(icon, contentDescription = null, tint = iconColor)
    }
}
