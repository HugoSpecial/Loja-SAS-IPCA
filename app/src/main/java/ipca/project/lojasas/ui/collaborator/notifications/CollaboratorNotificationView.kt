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

@Composable
fun CollaboratorNotificationView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: NotificationsCollaboratorViewModel = viewModel()
) {
    val state by remember { viewModel.uiState }
    var notificationForDialog by remember { mutableStateOf<Notification?>(null) }
    var existingReason by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Adaptável
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo",
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Notificações",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary // Verde
            )
            if (state.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) { Text("${state.unreadCount}", Modifier.padding(2.dp)) }
            }
        }

        Text(
            "Fique a par das novidades.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        FilterSection(state.selectedFilter) { viewModel.filterByType(it) }
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.notifications.isEmpty() -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.error != null -> Text(
                    state.error ?: "Erro",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.notifications.isEmpty() -> EmptyState(
                    message = "Sem notificações.",
                    icon = Icons.Outlined.Notifications
                )
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(state.notifications) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (notification.type != "resposta_entrega_rejeitada" && !notification.read) {
                                        viewModel.markAsRead(notification.docId)
                                    }

                                    if (notification.type == "resposta_entrega_rejeitada") {
                                        existingReason = null
                                        notificationForDialog = notification
                                    } else if (notification.type == "resposta_entrega") {
                                        navController.navigate("delivery_details/${notification.relatedId}")
                                    } else if (notification.type == "pedido_novo") {
                                        navController.navigate("order_details/${notification.relatedId}")
                                    }
                                }
                            )
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
                beneficiaryName = "Desconhecido"
            }

            viewModel.checkDeliveryStatus(notif.relatedId) { reasonFromDb ->
                existingReason = reasonFromDb
            }
        }

        val isDecided = existingReason != null &&
                !existingReason!!.contains("urgente", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { notificationForDialog = null },
            containerColor = MaterialTheme.colorScheme.surface, // Branco/Cinza Escuro
            title = {
                Column {
                    Text(
                        text = "Justificação de Falta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Beneficiário:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = beneficiaryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (beneficiaryPhone.isNotEmpty() && beneficiaryPhone != "--") {
                        Text(
                            text = "Contacto: $beneficiaryPhone",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = notif.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notif.body,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // SÓ MOSTRA A CAIXA DE ESTADO SE JÁ ESTIVER DECIDIDO
                    if (isDecided) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Estado Atual:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val isAccepted = existingReason!!.contains("Justificado", ignoreCase = true)

                        // Cores dinâmicas para a caixa de status
                        val statusBg = if (isAccepted)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)

                        val statusText = if (isAccepted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(statusBg, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = existingReason!!,
                                color = statusText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDecided) {
                    Button(
                        onClick = {
                            viewModel.handleJustificationDecision(notif, accepted = true) {
                                existingReason = "Justificado (Falta não aplicada)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aceitar justificação")
                    }
                } else {
                    TextButton(onClick = { notificationForDialog = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Fechar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isDecided) {
                    Button(
                        onClick = {
                            viewModel.handleJustificationDecision(notif, accepted = false) {
                                existingReason = "Justificação Rejeitada (Falta Aplicada)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recusar / Aplicar falta")
                    }
                }
            }
        )
    }
}

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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = notification.date?.let { dateFormat.format(it) } ?: "--"
    val isUnread = !notification.read

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) // Fundo verde transparente
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForType(notification.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = if (isUnread)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}