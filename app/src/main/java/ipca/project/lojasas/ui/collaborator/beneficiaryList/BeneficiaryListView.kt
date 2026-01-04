package ipca.project.lojasas.ui.collaborator.beneficiaryList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.User
import ipca.project.lojasas.ui.collaborator.beneficiaries.BeneficiaryListViewModel
import ipca.project.lojasas.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BeneficiaryListView(
    navController: NavController,
    viewModel: BeneficiaryListViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val historyState = viewModel.historyState.value

    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fundo Adaptável
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.beneficiaries.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { HeaderContent() }

                    item {
                        SearchBar(
                            searchText = state.searchText,
                            onSearchChange = { viewModel.onSearchTextChange(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (state.beneficiaries.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            EmptyState(
                                message = if (state.searchText.isEmpty()) "Não existem beneficiários." else "Nenhum resultado encontrado.",
                                icon = Icons.Outlined.Person
                            )
                        }
                    } else {
                        items(state.beneficiaries) { user ->
                            BeneficiaryCard(user = user, onClick = { selectedUser = user })
                        }
                    }
                }
            }
        }
    }

    if (selectedUser != null && !showHistoryDialog) {
        BeneficiaryDetailsDialog(
            user = selectedUser!!,
            onDismiss = { selectedUser = null },
            onViewHistory = {
                if (selectedUser!!.docId != null) {
                    viewModel.fetchUserHistory(selectedUser!!.docId!!)
                    showHistoryDialog = true
                }
            }
        )
    }

    if (showHistoryDialog && selectedUser != null) {
        HistoryDialog(
            user = selectedUser!!,
            orders = historyState.orders,
            isLoading = historyState.isLoading,
            onDismiss = {
                showHistoryDialog = false
                viewModel.clearHistory()
            }
        )
    }
}

@Composable
fun HeaderContent() {
    Column {
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logótipo",
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Lista de Beneficiários",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground, // Preto/Branco
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SearchBar(searchText: String, onSearchChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = {
            Text("Pesquisar por nome...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun BeneficiaryCard(user: User, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Branco ou Cinza Escuro
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name ?: "Sem Nome",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // Texto adaptável
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.fault > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), // Vermelho suave
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${user.fault} Falta(s)",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        "0 Faltas",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
            if (!user.preferences.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = Color(0xFFE65100), // Laranja (mantido fixo pois é alerta)
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Possui restrições/preferências",
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BeneficiaryDetailsDialog(user: User, onDismiss: () -> Unit, onViewHistory: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface, // Branco no Light, Cinza no Dark
        title = {
            Text(
                text = user.name ?: "Detalhes",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!user.email.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Email,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.email!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (!user.phone.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.phone!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val faultColor = if (user.fault > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = faultColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Número de Faltas: ${user.fault}",
                        fontSize = 14.sp,
                        fontWeight = if (user.fault > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (user.fault > 0) faultColor else MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                if (!user.preferences.isNullOrBlank()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFE65100)); Spacer(
                            modifier = Modifier.width(8.dp)
                        ); Text(
                            "Preferências",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        }
                        Text(user.preferences!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Text("Sem preferências.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onViewHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        null,
                        modifier = Modifier.size(18.dp)
                    ); Spacer(modifier = Modifier.width(8.dp)); Text("Ver Histórico")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
fun HistoryDialog(user: User, orders: List<Order>, isLoading: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    "Histórico",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ); Text("Beneficiário: ${user.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        text = {
            if (isLoading) Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            else if (orders.isEmpty()) Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) { Text("Nenhum registo.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            else LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { items(orders) { HistoryItemCard(it) } }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Voltar",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun HistoryItemCard(order: Order) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT"))
    Card(
        // Fundo do Card dentro do Dialog: Um pouco mais escuro/claro que o surface
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(order.orderDate?.let { dateFormat.format(it) } ?: "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Total: ${order.items.sumOf { it.quantity ?: 0 }} produtos",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}