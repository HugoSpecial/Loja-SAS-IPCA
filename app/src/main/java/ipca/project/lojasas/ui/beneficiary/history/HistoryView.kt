package ipca.project.lojasas.ui.beneficiary.history

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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.ui.components.EmptyState
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BeneficiaryHistoryView(
    navController: NavController,
    viewModel: BeneficiaryHistoryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Pedidos", "Levantamentos")

    val filteredHistory = remember(state.orders, state.deliveries, selectedFilter) {
        when (selectedFilter) {
            "Todos" -> state.orders + state.deliveries
            "Pedidos" -> state.orders
            "Levantamentos" -> state.deliveries
            else -> state.orders + state.deliveries
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {

        // --- CABEÇALHO ---
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo",
            modifier = Modifier
                .padding(top = 16.dp)
                .height(80.dp)
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // --- TÍTULO E DESCRIÇÃO ---
        Text(
            text = "Histórico",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Aqui consegue ver o histórico de pedidos realizados, histórico de levantamentos e justificações de faltas.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            lineHeight = 20.sp
        )

        // --- FILTROS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChipButton(
                    text = filter,
                    isSelected = selectedFilter == filter,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LISTA DE HISTÓRICO ---
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
                state.error != null -> Text(
                    text = state.error ?: "Erro desconhecido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                filteredHistory.isEmpty() -> {
                    EmptyState(
                        message = "Sem histórico disponível para este filtro.",
                        icon = Icons.Outlined.DateRange
                    )
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredHistory) { item ->
                        when (item) {
                            is Order -> HistoryCardOrder(item) {
                                navController.navigate("beneficiary_order_details/${item.docId}")
                            }
                            is Delivery -> HistoryCardDelivery(item) {
                                navController.navigate("beneficiary_delivery_detail/${item.docId}/sem_notificacao")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---
@Composable
fun FilterChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
fun HistoryCardOrder(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido de Cabaz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OrderStatusBadge(order.accept)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
            Text(
                text = order.orderDate?.let { dateFormat.format(it) } ?: "--/--/----",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun HistoryCardDelivery(delivery: Delivery, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Levantamento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                DeliveryStatusBadge(delivery.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
            Text(
                text = delivery.surveyDate?.let { dateFormat.format(it) } ?: "--/--/----",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
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
