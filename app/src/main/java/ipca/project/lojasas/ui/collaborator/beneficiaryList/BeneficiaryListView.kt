package ipca.project.lojasas.ui.collaborator.beneficiaryList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning // Icon para as faltas
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.* import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.models.User
import ipca.project.lojasas.ui.collaborator.beneficiaries.BeneficiaryListViewModel

val IpcaGreen = Color(0xFF00864F)
val BgLight = Color(0xFFF2F4F3)
val FaultRed = Color(0xFFD32F2F) // Cor para destacar as faltas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryListView(
    navController: NavController,
    viewModel: BeneficiaryListViewModel = viewModel()
) {
    val state = viewModel.uiState.value

    // Estado para controlar qual beneficiário está selecionado para o popup
    var selectedUser by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Beneficiários", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IpcaGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(BgLight)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else if (state.beneficiaries.isEmpty()) {
                Text(
                    text = "Não existem beneficiários registados.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.beneficiaries) { user ->
                        BeneficiaryCard(user = user, onClick = { selectedUser = user })
                    }
                }
            }
        }
    }

    // --- POP-UP DE DETALHES ---
    if (selectedUser != null) {
        BeneficiaryDetailsDialog(
            user = selectedUser!!,
            onDismiss = { selectedUser = null }
        )
    }
}

@Composable
fun BeneficiaryCard(user: User, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // LINHA 1: NOME e FALTAS (Lado a Lado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name ?: "Sem Nome",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f) // O nome ocupa o espaço que sobrar
                )

                // Indicador de Faltas
                if (user.fault > 0) {
                    Surface(
                        color = Color(0xFFFFEBEE), // Fundo vermelho claro
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${user.fault} Falta(s)",
                            color = FaultRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "0 Faltas",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Se tiver preferências, mostra um pequeno aviso visual no cartão
            if (!user.preferences.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Possui restrições/preferências",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100), // Laranja
                )
            }
        }
    }
}

@Composable
fun BeneficiaryDetailsDialog(user: User, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = user.name ?: "Detalhes do Beneficiário", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Contactos
                if (!user.email.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Email, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.email!!, fontSize = 14.sp)
                    }
                }
                if (!user.phone.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.phone!!, fontSize = 14.sp)
                    }
                }

                // NOVO: FALTAS NO DETALHE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Faltas",
                        tint = if (user.fault > 0) FaultRed else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Número de Faltas: ${user.fault}",
                        fontSize = 14.sp,
                        fontWeight = if (user.fault > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (user.fault > 0) FaultRed else Color.Black
                    )
                }

                Divider()

                // PREFERÊNCIAS / ALERGIAS
                if (!user.preferences.isNullOrBlank()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preferências / Restrições", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(user.preferences!!, fontSize = 14.sp)
                    }
                } else {
                    Text("Sem preferências registadas.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}