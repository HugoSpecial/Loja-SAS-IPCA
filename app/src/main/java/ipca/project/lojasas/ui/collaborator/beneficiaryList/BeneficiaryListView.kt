package ipca.project.lojasas.ui.collaborator.beneficiaryList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ipca.project.lojasas.models.User
import ipca.project.lojasas.ui.collaborator.beneficiaries.BeneficiaryListViewModel

// Cores
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
    var selectedUser by remember { mutableStateOf<User?>(null) }

    // Estrutura principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // REMOVIDO: A Row com o botão de voltar foi apagada daqui.

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = IpcaGreen)
            } else if (state.beneficiaries.isEmpty()) {
                // Lista vazia
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp), // Padding igual à Home
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { HeaderContent() }
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = "Não existem beneficiários registados.",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Lista com conteúdo
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Mantém o padding lateral de 24dp para alinhar texto e logo com a Home
                    contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cabeçalho (Logo + Título)
                    item { HeaderContent() }

                    // Cartões
                    items(state.beneficiaries) { user ->
                        BeneficiaryCard(user = user, onClick = { selectedUser = user })
                    }
                }
            }
        }
    }

    // Pop-up de Detalhes
    if (selectedUser != null) {
        BeneficiaryDetailsDialog(
            user = selectedUser!!,
            onDismiss = { selectedUser = null }
        )
    }
}

/**
 * Componente auxiliar para o cabeçalho.
 * Ajustado para ter o mesmo espaçamento (40.dp) da Home.
 */
@Composable
fun HeaderContent() {
    Column {
        // MUDANÇA: Ajustado de 10.dp para 40.dp para igualar a Home
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logótipo",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lista de Beneficiários",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TitleBlack,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun BeneficiaryCard(user: User, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Nome e Faltas
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
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${user.fault} Falta(s)",
                            color = FaultRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

            // Preferências
            if (!user.preferences.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Possui restrições/preferências",
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
fun BeneficiaryDetailsDialog(user: User, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(text = user.name ?: "Detalhes", fontWeight = FontWeight.Bold, color = TitleBlack)
        },
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
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Faltas",
                        tint = if (user.fault > 0) FaultRed else IpcaGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Número de Faltas: ${user.fault}",
                        fontSize = 14.sp,
                        fontWeight = if (user.fault > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (user.fault > 0) FaultRed else TitleBlack
                    )
                }

                Divider(color = BgLight)

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
                Text("Fechar", color = IpcaGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}