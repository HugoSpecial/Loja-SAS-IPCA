package ipca.project.lojasas.ui.collaborator.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import java.text.SimpleDateFormat
import java.util.Locale

// --- PALETA DE CORES ---
val IpcaGreen = Color(0xFF00864F)
val IpcaDarkTeal = Color(0xFF005A49)
val BgLight = Color(0xFFF5F7FA)
val TextDark = Color(0xFF2D3436)
val TextGray = Color(0xFF95A5A6)
val BorderColor = Color(0xFFE0E0E0) // Cor para a borda subtil

@Composable
fun OrderListView(
    navController: NavController,
    viewModel: OrderViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var selectedFilter by remember { mutableStateOf<OrderState?>(null) }

    val filteredList = remember(state.orders, selectedFilter) {
        if (selectedFilter == null) state.orders
        else state.orders.filter { it.accept == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // --- CABEÇALHO ---
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
                    tint = IpcaDarkTeal,
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

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            Spacer(modifier = Modifier.height(16.dp))

            // Título e Contador
            Text(
                text = "Gestão de Pedidos",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "${filteredList.size} pedidos encontrados",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Filtros ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BubbleFilter("Todos", selectedFilter == null) { selectedFilter = null }
                BubbleFilter("Pendentes", selectedFilter == OrderState.PENDENTE) { selectedFilter = OrderState.PENDENTE }
                BubbleFilter("Aceites", selectedFilter == OrderState.ACEITE) { selectedFilter = OrderState.ACEITE }
                BubbleFilter("Recusados", selectedFilter == OrderState.REJEITADA) { selectedFilter = OrderState.REJEITADA }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Lista ---
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IpcaGreen)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredList) { order ->
                        SoftTicketCard(
                            order = order,
                            onClick = { navController.navigate("order_details/${order.docId}") }
                        )
                    }
                }
            }
        }
    }
}

// --- CARD ATUALIZADO (Mais definição) ---
@Composable
fun SoftTicketCard(order: Order, onClick: () -> Unit) {

    val totalQuantity = order.items.sumOf { it.quantity ?: 0 }

    val (accentColor, statusText) = when(order.accept) {
        OrderState.PENDENTE -> Color(0xFFF39C12) to "Pendente"
        OrderState.ACEITE -> IpcaGreen to "Aceite"
        OrderState.REJEITADA -> Color(0xFFC0392B) to "Recusado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // 1. ADICIONADO: Sombra leve para profundidade
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // 2. ADICIONADO: Borda subtil para definição
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // LADO ESQUERDO
            Column(modifier = Modifier.weight(1f)) {

                // Badge de Estado
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nome
                Text(
                    text = order.userName ?: "Anónimo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Data com ícone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.calendar_outline),
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val dateFormat = SimpleDateFormat("dd 'de' MMMM yyyy", Locale("pt", "PT"))
                    val dateStr = order.orderDate?.let { dateFormat.format(it) } ?: "--"

                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. ADICIONADO: Divisória Vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color(0xFFF0F0F0)) // Cinza muito claro
            )

            // LADO DIREITO: Bolha de Quantidade + Seta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$totalQuantity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IpcaDarkTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "itens",
                        fontSize = 10.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 4. ADICIONADO: Seta para indicar ação
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BubbleFilter(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) IpcaDarkTeal else Color.White
    val txt = if (isSelected) Color.White else TextGray
    val border = if (isSelected) null else BorderStroke(1.dp, BorderColor) // Borda no filtro também

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .then(if (border != null) Modifier.padding(1.dp) else Modifier) // Ajuste fino
    ) {
        // Se quiseres borda no filtro não selecionado, usa Surface em vez de Box aqui,
        // mas para manter simples deixei assim.
        Text(
            text = text,
            color = txt,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}