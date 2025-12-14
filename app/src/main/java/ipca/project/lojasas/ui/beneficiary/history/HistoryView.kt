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
import androidx.compose.material.icons.filled.ArrowBack
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
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Locale

val BackgroundGray = Color(0xFFF5F6F8)
val TextGray = Color(0xFF8C8C8C)

@Composable
fun BeneficiaryHistoryView(
    navController: NavController,
    viewModel: BeneficiaryHistoryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Pedidos", "Levantamentos", "Justificações de faltas")

    val filteredHistory = remember(state.orders, selectedFilter) {
        when (selectedFilter) {
            "Todos" -> state.orders
            "Pedidos" -> state.orders
            "Levantamentos" -> emptyList() // ainda não implementado
            "Justificações de faltas" -> emptyList() // ainda não implementado
            else -> state.orders
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 20.dp)
    ) {

        // --- CABEÇALHO COM LOGÓTIPO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
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
        }


        // --- TÍTULO ---
        Text(
            text = "Histórico do Beneficiário",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Aqui consegue ver o histórico de pedidos realizados, histórico de levantamentos, histórico de justificações de faltas.",
            fontSize = 14.sp,
            color = TextGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
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
                    modifier = Modifier.align(Alignment.Center)
                )
                state.error != null -> Text(
                    text = state.error ?: "Erro",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
                filteredHistory.isEmpty() -> Text(
                    text = "Sem histórico disponível.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredHistory) { order ->
                        HistoryCard(order) {
                            navController.navigate("beneficiary_order_details/${order.docId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (isSelected) Color.White else TextGray
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun HistoryCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pedido",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            Text(
                text = order.orderDate?.let { dateFormat.format(it) } ?: "--",
                fontSize = 12.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(6.dp))
            OrderStatusBadge(order.accept)
        }
    }
}

@Composable
private fun OrderStatusBadge(state: OrderState) {
    val (bg, color) = when (state) {
        OrderState.PENDENTE -> Color(0xFFFFE0B2) to Color(0xFFEF6C00)
        OrderState.ACEITE -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        OrderState.REJEITADA -> Color(0xFFFFCDD2) to Color(0xFFC62828)
    }
    StatusBadge(label = state.name, backgroundColor = bg, contentColor = color)
}
