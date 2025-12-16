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
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Locale

// Cores
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

    // Lógica de filtragem simples
    val filteredHistory = remember(state.orders, selectedFilter) {
        when (selectedFilter) {
            "Todos" -> state.orders
            "Pedidos" -> state.orders
            "Levantamentos" -> emptyList()
            "Justificações de faltas" -> emptyList()
            else -> state.orders
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(horizontal = 20.dp) // Nota: Aqui não há padding vertical (top/bottom)
    ) {

        // --- CABEÇALHO ---
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo",
            modifier = Modifier
                // 1. Adicionamos padding TOP de 16dp para simular o padding da Column do Home
                .padding(top = 16.dp)
                // 2. Definimos a altura exata igual ao Home
                .height(80.dp)
                // 3. Adicionamos padding BOTTOM de 16dp igual ao Home
                .padding(bottom = 16.dp)
                // 4. Centramos manualmente porque esta Column não centra itens por defeito
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
            color = TextGray,
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
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (isSelected) Color.White else TextGray
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
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
            // Linha superior: Tipo e Badge
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

            // Data
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))
            Text(
                text = order.orderDate?.let { dateFormat.format(it) } ?: "--/--/----",
                fontSize = 13.sp,
                color = TextGray
            )
        }
    }
}

@Composable
private fun OrderStatusBadge(state: OrderState) {
    val (backgroundColor, contentColor) = when (state) {
        OrderState.PENDENTE -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        OrderState.ACEITE -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        OrderState.REJEITADA -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    StatusBadge(
        label = state.name,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )
}