package ipca.project.lojasas.ui.collaborator.delivery

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
import ipca.project.lojasas.models.*
import ipca.project.lojasas.ui.components.InfoRow
import ipca.project.lojasas.ui.components.SectionTitle
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailView(
    navController: NavController,
    deliveryId: String,
    viewModel: DeliveryDetailViewModel = viewModel()
) {
    val state = viewModel.uiState.value

    LaunchedEffect(deliveryId) { viewModel.fetchDelivery(deliveryId) }

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
            IconButton(onClick = { navController.popBackStack() }) {
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
                    contentDescription = "Cabeçalho IPCA SAS",
                    modifier = Modifier.heightIn(max = 55.dp).align(Alignment.Center),
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
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.delivery != null -> {
                    val delivery = state.delivery

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Status da entrega
                        DeliveryStatusBadge(state = delivery.state)

                        if (delivery.state == DeliveryState.CANCELADO && !delivery.reason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("Motivo da rejeição:", delivery.reason ?: "-")
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
                        state.order?.let { order ->
                            if (order.items.isEmpty()) {
                                Text(
                                    "Nenhum produto solicitado.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                ProductCategoryList(
                                    orderItems = order.items,
                                    allProducts = state.products,
                                    isFinal = order.accept != OrderState.PENDENTE
                                )
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
                                delivery.surveyDate?.let {
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
                                delivery.evaluationDate?.let {
                                    SimpleDateFormat(
                                        "d 'de' MMM 'de' yyyy, HH:mm",
                                        Locale("pt", "PT")
                                    ).format(it)
                                } ?: "-"
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // --- Botões ---
                            val now = Calendar.getInstance()
                            val deliveryDate = Calendar.getInstance().apply { delivery.surveyDate?.let { time = it } }
                            deliveryDate.add(Calendar.DAY_OF_MONTH, 1)

                            val canShowButtons = delivery.state == DeliveryState.PENDENTE && now.after(deliveryDate)

                            if (canShowButtons) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.rejectDelivery(delivery.docId ?: "") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Não levantou", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.approveDelivery(delivery.docId ?: "") },
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
                                    text = "Esta entrega encontra-se ${delivery.state.name}",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        }
    }
}

// --- Componentes auxiliares ---
@Composable
private fun DeliveryStatusBadge(state: DeliveryState) {
    // Cores adaptáveis
    val mainColor = when (state) {
        DeliveryState.PENDENTE -> Color(0xFFEF6C00) // Laranja
        DeliveryState.ENTREGUE -> MaterialTheme.colorScheme.primary // Verde
        DeliveryState.CANCELADO -> MaterialTheme.colorScheme.error // Vermelho
        DeliveryState.EM_ANALISE -> Color(0xFF00BCD4) // Azul
    }

    // Fundo com transparência
    StatusBadge(
        label = state.name,
        backgroundColor = mainColor.copy(alpha = 0.1f),
        contentColor = mainColor
    )
}

// --- Produtos ---
@Composable
private fun ProductCategoryList(
    orderItems: List<OrderItem>,
    allProducts: List<Product>,
    isFinal: Boolean
) {
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
                Text(
                    "${orderItem.quantity}x ${orderItem.name}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                val totalStock = product.batches.sumOf { it.quantity }
                val newStock = totalStock - (orderItem.quantity ?: 0)
                val icon = if (newStock >= 0) Icons.Default.Check else Icons.Default.Close
                val iconColor = if (newStock >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Text(
                    "${orderItem.quantity}x ${orderItem.name} (Stock: $totalStock)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Stock atualizado: $newStock",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Icon(icon, contentDescription = null, tint = iconColor)
            }
        }
    }
}