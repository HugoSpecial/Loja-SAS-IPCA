package ipca.project.lojasas.ui.beneficiary.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
// Certifica-te que estes imports existem no teu projeto, senão usa as cores hardcoded ou Color(0xFF...)
import ipca.project.lojasas.ui.beneficiary.orders.BackgroundColor
import ipca.project.lojasas.ui.beneficiary.orders.TextGray
import ipca.project.lojasas.ui.components.IpcaGreen
import ipca.project.lojasas.ui.components.SectionTitle

@Composable
fun DeliveryDetailView(
    navController: NavController,
    deliveryId: String,
    notificationId: String, // <--- CORREÇÃO AQUI: Recebe o ID em vez do título/corpo
    viewModel: DeliveryDetailViewModel = viewModel()
) {
    val state = viewModel.uiState

    // Agora o notificationId já existe e pode ser usado aqui
    LaunchedEffect(Unit) {
        viewModel.fetchNotificationData(notificationId)
    }

    DeliveryDetailContent(
        state = state,
        onBackClick = { navController.popBackStack() },
        onNoteChange = { viewModel.onUserNoteChange(it) },
        onSaveClick = { viewModel.saveDeliveryNote(deliveryId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailContent(
    state: DeliveryDetailState,
    onBackClick: () -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = IpcaGreen)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Detalhes da Entrega",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = IpcaGreen
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Notificação")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = state.notificationTitle.ifEmpty { "Sem Título" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.notificationBody.ifEmpty { "Sem conteúdo." },
                            fontSize = 16.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionTitle("Observação/Resposta")

                OutlinedTextField(
                    value = state.userNote,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IpcaGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = IpcaGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IpcaGreen),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.isSaved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enviado com sucesso!",
                        color = IpcaGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Erro desconhecido",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}