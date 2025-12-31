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
import java.text.SimpleDateFormat
import java.util.Locale

// Cores (Mantidas)
val IpcaGreen = Color(0xFF00864F)
val BgLight = Color(0xFFF2F4F3)
val FaultRed = Color(0xFFD32F2F)
val TitleBlack = Color(0xFF1A1A1A)

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
            .background(BgLight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.beneficiaries.isEmpty()) {
                // Loading inicial apenas se não tiver dados
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = IpcaGreen)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Cabeçalho
                    item { HeaderContent() }

                    // 2. BARRA DE PESQUISA (NOVA)
                    item {
                        SearchBar(
                            searchText = state.searchText,
                            onSearchChange = { viewModel.onSearchTextChange(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 3. Conteúdo da Lista
                    if (state.beneficiaries.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (state.searchText.isEmpty()) "Não existem beneficiários." else "Nenhum resultado encontrado.",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

    // Pop-up de Detalhes
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

    // Pop-up de Histórico
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
            color = TitleBlack,
            modifier = Modifier.padding(bottom = 16.dp) // Reduzi ligeiramente para caber a pesquisa
        )
    }
}

// --- NOVO COMPONENTE: BARRA DE PESQUISA ---
@Composable
fun SearchBar(searchText: String, onSearchChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChange,
        placeholder = { Text("Pesquisar por nome...", color = Color.Gray) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = IpcaGreen)
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IpcaGreen,
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

// ... Resto dos componentes (BeneficiaryCard, Dialogs, etc.) mantêm-se iguais ao anterior ...
// Vou incluir apenas os componentes que já existiam abaixo para garantir que o código compila

@Composable
fun BeneficiaryCard(user: User, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = TitleBlack,
                    modifier = Modifier.weight(1f)
                )
                if (user.fault > 0) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                        Text("${user.fault} Falta(s)", color = FaultRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                } else {
                    Text("0 Faltas", color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (!user.preferences.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Possui restrições/preferências", fontSize = 12.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun BeneficiaryDetailsDialog(user: User, onDismiss: () -> Unit, onViewHistory: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(text = user.name ?: "Detalhes", fontWeight = FontWeight.Bold, color = TitleBlack) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!user.email.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Email, null, tint = IpcaGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.email!!, fontSize = 14.sp)
                    }
                }
                if (!user.phone.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = IpcaGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.phone!!, fontSize = 14.sp)
                    }
                }
                Divider(color = BgLight)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = if (user.fault > 0) FaultRed else IpcaGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Número de Faltas: ${user.fault}", fontSize = 14.sp, fontWeight = if (user.fault > 0) FontWeight.Bold else FontWeight.Normal, color = if (user.fault > 0) FaultRed else TitleBlack)
                }
                Divider(color = BgLight)
                if (!user.preferences.isNullOrBlank()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFE65100)); Spacer(modifier = Modifier.width(8.dp)); Text("Preferências", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        Text(user.preferences!!, fontSize = 14.sp)
                    }
                } else { Text("Sem preferências.", color = Color.Gray) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onViewHistory, colors = ButtonDefaults.buttonColors(containerColor = IpcaGreen), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Ver Histórico")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar", color = Color.Gray) } }
    )
}

@Composable
fun HistoryDialog(user: User, orders: List<Order>, isLoading: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Column { Text("Histórico", fontWeight = FontWeight.Bold, color = TitleBlack); Text("Beneficiário: ${user.name}", fontSize = 12.sp, color = Color.Gray) } },
        text = {
            if (isLoading) Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = IpcaGreen) }
            else if (orders.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("Nenhum registo.", color = Color.Gray) }
            else LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(orders) { HistoryItemCard(it) } }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Voltar", color = IpcaGreen, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun HistoryItemCard(order: Order) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("pt", "PT"))
    Card(colors = CardDefaults.cardColors(containerColor = BgLight), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(order.orderDate?.let { dateFormat.format(it) } ?: "-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total: ${order.items.sumOf { it.quantity ?: 0 }} produtos", fontSize = 12.sp, color = TitleBlack)
        }
    }
}