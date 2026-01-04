package ipca.project.lojasas.ui.beneficiary.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Notification
import ipca.project.lojasas.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: NotificationsBeneficiaryViewModel = viewModel()
) {
    val state by remember { viewModel.uiState }

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fundo adaptável
            .padding(horizontal = 24.dp)
    ) {

        // --- 1. CABEÇALHO ---
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo SAS",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. TÍTULO ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Notificações",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary // GreenPrimary
            )

            if (state.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.error, // RedPrimary
                    contentColor = Color.White
                ) {
                    Text(text = "${state.unreadCount}", modifier = Modifier.padding(2.dp))
                }
            }
        }

        Text(
            text = "Fique a par do estado dos seus pedidos.",
            fontSize = 14.sp,
            // Texto cinza adaptável (Branco transparente no escuro)
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // --- 3. FILTROS ---
        BeneficiaryFilterSection(
            selectedFilter = state.selectedFilter,
            onFilterSelected = { category -> viewModel.filterByType(category) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. LISTA ---
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.notifications.isEmpty() -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Ocorreu um erro.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.notifications.isEmpty() -> {
                    val msg = when(state.selectedFilter) {
                        "cat_pedidos" -> "Sem notificações de pedidos."
                        "cat_candidaturas" -> "Sem notificações de candidatura."
                        "cat_levantamentos" -> "Sem levantamentos agendados."
                        "cat_Entrega" -> "Sem notificações de entregas."
                        else -> "Sem notificações novas."
                    }

                    EmptyState(
                        message = msg,
                        icon = Icons.Outlined.Notifications
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(state.notifications) { notification ->
                            BeneficiaryNotificationCard(
                                notification = notification,
                                onClick = {
                                    if (!notification.read) {
                                        viewModel.markAsRead(notification.docId)
                                    }

                                    // --- NAVEGAÇÃO ---
                                    if (notification.type.startsWith("candidatura", ignoreCase = true)) {
                                        navController.navigate("await-candidature")
                                    }
                                    else if (notification.type.equals("entrega", ignoreCase = true) ||
                                        notification.type.equals("entrega_rejeitada", ignoreCase = true)) {
                                        navController.navigate("beneficiary_delivery_detail/${notification.relatedId}/${notification.docId}")
                                    }
                                    else {
                                        navController.navigate("beneficiary_order_details/${notification.relatedId}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ÍCONES ---
@Composable
fun getBeneficiaryIconForType(notification: Notification): ImageVector {
    return when {
        notification.type.startsWith("candidatura") -> Icons.Default.AccountBox
        notification.title.contains("Levantamento", ignoreCase = true) -> Icons.Default.DateRange
        notification.type.equals("entrega", ignoreCase = true) -> Icons.Default.LocalShipping
        notification.type.startsWith("pedido") -> ImageVector.vectorResource(id = R.drawable.shopping_cart)
        else -> Icons.Default.Notifications
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun BeneficiaryFilterSection(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        null to "Tudo",
        "cat_pedidos" to "Pedidos",
        "cat_candidaturas" to "Candidatura",
        "cat_levantamentos" to "Levantamentos",
        "cat_Entrega" to "Entregas"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { (key, label) ->
            val isSelected = selectedFilter == key

            Button(
                onClick = { onFilterSelected(key) },
                colors = ButtonDefaults.buttonColors(
                    // Se selecionado: Verde. Se não: Surface (Branco/Cinza Escuro)
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    // Se selecionado: Branco. Se não: Texto cinza
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                // Borda suave se não estiver selecionado
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun BeneficiaryNotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = notification.date?.let { dateFormat.format(it) } ?: "--"
    val isUnread = !notification.read

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Branco (Light) ou DarkSurface (Dark)
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                // Fundo do Ícone: Verde com 10% de opacidade
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBeneficiaryIconForType(notification),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, // Verde
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error) // Vermelho
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface, // Preto/Branco
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Data discreta
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    // Texto do corpo ligeiramente mais claro se lido
                    color = if (isUnread)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}