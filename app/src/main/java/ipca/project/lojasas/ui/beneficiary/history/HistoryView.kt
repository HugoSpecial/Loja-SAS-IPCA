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
    val filters = listOf("Todos", "Pedidos", "Levantamentos", "Justificações de faltas")

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
            .background(MaterialTheme.colorScheme.background) // Fundo adaptável
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
            color = MaterialTheme.colorScheme.primary // Verde
        )

        Text(
            text = "Aqui consegue ver o histórico de pedidos realizados, histórico de levantamentos e justificações de faltas.",
            fontSize = 14.sp,
            // Texto cinza que se adapta (fica branco translúcido no escuro)
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
            // Se selecionado: Verde. Se não: Branco (Light) ou Preto (Dark)
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            // Se selecionado: Branco. Se não: Cinza/Preto
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        ),
        // Borda cinza suave apenas se não estiver selecionado
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
fun HistoryCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Branco (Light) / Preto (Dark)
        )
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Cinza
            )
        }
    }
}

@Composable
private fun OrderStatusBadge(state: OrderState) {
    // Definimos a cor principal baseada no estado
    val mainColor = when (state) {
        OrderState.PENDENTE -> Color(0xFFEF6C00)        // Laranja (Fixo pois é padrão de alerta)
        OrderState.ACEITE -> MaterialTheme.colorScheme.primary // Verde do Tema
        OrderState.REJEITADA -> MaterialTheme.colorScheme.error // Vermelho do Tema
    }

    // O fundo usa a mesma cor principal, mas com apenas 10% de opacidade.
    // Isso funciona perfeitamente tanto no branco quanto no preto.
    StatusBadge(
        label = state.name,
        backgroundColor = mainColor.copy(alpha = 0.1f),
        contentColor = mainColor
    )
}