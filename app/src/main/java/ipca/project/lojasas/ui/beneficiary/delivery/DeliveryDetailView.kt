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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
// Imports do teu projeto (Certifica-te que o caminho do SectionTitle está certo)
import ipca.project.lojasas.ui.components.SectionTitle

@Composable
fun DeliveryDetailView(
    navController: NavController,
    orderId: String,
    notificationId: String,
    viewModel: DeliveryDetailViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.fetchNotificationData(notificationId)
        viewModel.checkExistingNote(orderId)
    }

    DeliveryDetailContent(
        state = state,
        onBackClick = { navController.popBackStack() },
        onNoteChange = { viewModel.onUserNoteChange(it) },
        onSaveClick = { note ->
            viewModel.saveDeliveryNote(orderId, note)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailContent(
    state: DeliveryDetailState,
    onBackClick: () -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onSaveClick: (String) -> Unit = {}
) {
    // 1. Fundo adaptável (Cinza Claro no Light, Preto no Dark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- CABEÇALHO ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.primary // GreenPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Detalhes da Entrega",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // GreenPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Notificação") // Certifica-te que este componente aceita cores do tema

                // --- CARD DA NOTIFICAÇÃO ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // Branco (Light) ou Preto (Dark)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = state.notificationTitle.ifEmpty { "Sem Título" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // Preto (Light) ou Branco (Dark)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.notificationBody.ifEmpty { "Sem conteúdo." },
                            fontSize = 16.sp,
                            // Texto cinza que se adapta ao modo escuro
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionTitle("Observação/Resposta")

                // --- CAIXA DE TEXTO ---
                OutlinedTextField(
                    value = state.userNote,
                    onValueChange = onNoteChange,
                    enabled = !state.isSaved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        // Bordas
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),

                        // Fundo
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,

                        // Texto e Cursor
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- BOTÃO ENVIAR ---
                if (!state.isSaved) {
                    Button(
                        onClick = { onSaveClick(state.userNote) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary // Branco
                        ),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Enviar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // --- MENSAGEM DE SUCESSO ---
                if (state.isSaved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enviado com sucesso!",
                        color = MaterialTheme.colorScheme.primary, // GreenPrimary
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // --- MENSAGEM DE ERRO ---
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Erro",
                        color = MaterialTheme.colorScheme.error, // RedPrimary
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}