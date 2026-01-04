package ipca.project.lojasas.ui.collaborator.notifications

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
import androidx.compose.runtime.*
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

val BackgroundGray = Color(0xFFF9F9F9)
val PrimaryGreen = Color(0xFF00864F)
val TextGray = Color(0xFF8C8C8C)
val LightGreenBg = Color(0xFFE8F5E9)

@Composable
fun CollaboratorNotificationView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: NotificationsCollaboratorViewModel = viewModel()
) {
    val state by remember { viewModel.uiState }
    var notificationForDialog by remember { mutableStateOf<Notification?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications()
    }

    Column(
        modifier = modifier.fillMaxSize().background(BackgroundGray).padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(painter = painterResource(id = R.drawable.logo_sas), contentDescription = "Logo", modifier = Modifier.fillMaxWidth().height(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Notificações", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            if (state.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = Color.Red, contentColor = Color.White) { Text("${state.unreadCount}", Modifier.padding(2.dp)) }
            }
        }
        Text("Fique a par das novidades.", fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        FilterSection(state.selectedFilter) { viewModel.filterByType(it) }
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.notifications.isEmpty() -> CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error ?: "Erro", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                state.notifications.isEmpty() -> EmptyState(message = "Sem notificações.", icon = Icons.Outlined.Notifications)
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(state.notifications) { notification ->
                            NotificationCard(notification = notification, onClick = {

                                // --- ALTERAÇÃO IMPORTANTE AQUI ---
                                // Só marca como lida IMEDIATAMENTE se NÃO for uma justificação.
                                // Se for justificação, esperamos que o utilizador decida no popup.
                                if (notification.type != "resposta_entrega_rejeitada" && !notification.read) {
                                    viewModel.markAsRead(notification.docId)
                                }

                                if (notification.type == "resposta_entrega_rejeitada") {
                                    notificationForDialog = notification // Abre o popup (ainda não lida)
                                } else if (notification.type == "resposta_entrega") {
                                    navController.navigate("delivery_details/${notification.relatedId}")
                                } else if (notification.type == "pedido_novo") {
                                    navController.navigate("order_details/${notification.relatedId}")
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    // --- POPUP: JUSTIFICAR FALTA ---
    if (notificationForDialog != null) {
        val notif = notificationForDialog!!
        val dateFormat = SimpleDateFormat("dd/MM/yyyy às HH:mm", Locale.getDefault())
        val dateString = notif.date?.let { dateFormat.format(it) } ?: "Data desconhecida"

        var beneficiaryName by remember { mutableStateOf("A carregar...") }
        var beneficiaryPhone by remember { mutableStateOf("") }

        LaunchedEffect(notif) {
            val idToSearch = notif.senderId
            if (idToSearch.isNotEmpty()) {
                viewModel.fetchBeneficiaryDetails(idToSearch) { name, phone ->
                    beneficiaryName = name
                    beneficiaryPhone = phone
                }
            } else {
                beneficiaryName = "Desconhecido (ID vazio)"
            }
        }

        AlertDialog(
            onDismissRequest = { notificationForDialog = null },
            containerColor = Color.White,
            title = {
                Column {
                    Text(text = "Justificação de Falta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.LightGray)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Beneficiário:", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Text(text = beneficiaryName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)

                    if (beneficiaryPhone.isNotEmpty() && beneficiaryPhone != "--") {
                        Text(text = "Contacto: $beneficiaryPhone", fontSize = 14.sp, color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = dateString, fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = BackgroundGray,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notif.body,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    // ACEITAR = O ViewModel trata de marcar como lida e atualizar BD
                    onClick = {
                        viewModel.handleJustificationDecision(notif, accepted = true) {
                            notificationForDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aceitar justificação")
                }
            },
            dismissButton = {
                Button(
                    // RECUSAR = O ViewModel aplica falta, cancela entrega e marca como lida
                    onClick = {
                        viewModel.handleJustificationDecision(notif, accepted = false) {
                            notificationForDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recusar / Aplicar falta")
                }
            }
        )
    }
}

// ... Resto das funções auxiliares ...
@Composable
fun getIconForType(type: String): ImageVector {
    return when (type) {
        "candidatura_nova" -> Icons.Default.AccountBox
        "pedido_novo" -> ImageVector.vectorResource(id = R.drawable.shopping_cart)
        "pedido_agendado" -> Icons.Default.DateRange
        "validade_alerta" -> Icons.Default.Warning
        "resposta_entrega" -> Icons.Default.Email
        "resposta_entrega_rejeitada" -> Icons.Default.Cancel
        else -> Icons.Default.Notifications
    }
}

@Composable
fun FilterSection(selectedFilter: String?, onFilterSelected: (String?) -> Unit) {
    val filters = listOf(null to "Tudo", "resposta_entrega_rejeitada" to "Justificações", "candidatura_nova" to "Candidaturas", "pedido_novo" to "Pedidos")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(filters) { (type, label) ->
            val isSelected = selectedFilter == type
            Button(
                onClick = { onFilterSelected(type) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PrimaryGreen else Color.White, contentColor = if (isSelected) Color.White else TextGray),
                border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) { Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
        }
    }
}

@Composable
fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = notification.date?.let { dateFormat.format(it) } ?: "--"
    val isUnread = !notification.read
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(LightGreenBg).align(Alignment.Center), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getIconForType(notification.type), contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                }
                if (isUnread) Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Red).align(Alignment.TopEnd))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = notification.title, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = dateString, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = notification.body, fontSize = 14.sp, color = if (isUnread) Color.DarkGray else TextGray, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
            }
        }
    }
}