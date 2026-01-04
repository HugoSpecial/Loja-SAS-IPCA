package ipca.project.lojasas.ui.candidature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.ui.authentication.LoginViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AwaitCandidatureView(
    navController: NavController,
    viewModelAuth: LoginViewModel = viewModel(),
    viewModelData: AwaitCandidatureViewModel = viewModel()
) {
    val uiState = viewModelData.uiState.value
    val candidature = uiState.candidature
    val isLoading = uiState.isLoading
    val errorMessage = uiState.error

    // --- VARIÁVEIS DE UI ---
    val currentState = candidature?.state ?: CandidatureState.PENDENTE
    val rejectionReason = candidature?.statusChangeReason ?: "Sem motivo registado."

    val submissionDate = try {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        candidature?.creationDate?.let { sdf.format(it) } ?: "Data N/D"
    } catch (e: Exception) { "Data N/D" }

    // --- CORES E TEXTOS ADAPTÁVEIS ---
    // Usamos as cores do tema para garantir consistência no Dark Mode
    val (statusColor, statusBg, statusText, statusHeadline) = when (currentState) {
        CandidatureState.PENDENTE -> {
            val color = Color(0xFFFFA000) // Laranja (Padrão para alertas)
            Quadruple(color, color.copy(alpha = 0.1f), "PENDENTE", "A aguardar validação")
        }
        CandidatureState.ACEITE -> {
            val color = MaterialTheme.colorScheme.primary // Verde do Tema
            Quadruple(color, color.copy(alpha = 0.1f), "ACEITE", "Candidatura Aceite")
        }
        CandidatureState.REJEITADA -> {
            val color = MaterialTheme.colorScheme.error // Vermelho do Tema
            Quadruple(color, color.copy(alpha = 0.1f), "RECUSADA", "Candidatura Rejeitada")
        }
    }

    val friendlyMessage = when (currentState) {
        CandidatureState.PENDENTE -> "Recebemos o seu pedido. Aguarde a validação dos serviços."
        CandidatureState.ACEITE -> "Parabéns! O seu pedido foi aceite. Já pode usufruir dos apoios."
        CandidatureState.REJEITADA -> "Infelizmente o pedido não reuniu as condições necessárias."
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background // Fundo Adaptável
    ) { paddingValues ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModelData.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Tentar Novamente")
                    }
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val screenHeight = maxHeight
                val isCompactScreen = screenHeight < 700.dp
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isCompactScreen) Arrangement.Top else Arrangement.SpaceBetween
                ) {

                    // 1. CABEÇALHO
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(if (isCompactScreen) 10.dp else 40.dp))

                        Image(
                            painter = painterResource(id = R.drawable.logo_sas),
                            contentDescription = "Logo IPCA",
                            modifier = Modifier
                                .height(if (isCompactScreen) 60.dp else 80.dp)
                                .fillMaxWidth(),
                            alignment = Alignment.Center
                        )

                        Spacer(modifier = Modifier.height(if (isCompactScreen) 32.dp else 40.dp))

                        Text(
                            text = "Estado da Candidatura",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactScreen) 26.sp else 32.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. CARD DE STATUS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface // Branco ou Cinza Escuro
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isCompactScreen) 20.dp else 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Badge Status
                            Surface(
                                color = statusBg, // Cor com transparência
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = statusHeadline,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = if (isCompactScreen) 20.sp else 24.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = friendlyMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Submetido em $submissionDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(if (isCompactScreen) 20.dp else 32.dp))

                            // Timeline
                            val currentStep = when(currentState) {
                                CandidatureState.PENDENTE -> 2
                                CandidatureState.ACEITE -> 3
                                CandidatureState.REJEITADA -> 3
                                else -> 1
                            }
                            MinimalHorizontalTimeline(activeColor = statusColor, currentStep = currentStep)

                            // BOTÃO: SE REJEITADA
                            if (currentState == CandidatureState.REJEITADA) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(statusBg, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = statusColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Motivo:", color = statusColor, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = rejectionReason,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.navigate("candidature_form") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Corrigir Dados")
                                }
                            }

                            // BOTÃO: SE ACEITE
                            if (currentState == CandidatureState.ACEITE) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        viewModelData.activateBeneficiary {
                                            navController.navigate("home") {
                                                popUpTo(0)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = statusColor,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Começar Agora")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. RODAPÉ (LOGOUT)
                    OutlinedButton(
                        onClick = {
                            viewModelAuth.logout(onLogoutSuccess = {
                                navController.navigate("login") { popUpTo("login") { inclusive = true } }
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary // Vermelho do Tema
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ExitToApp, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terminar Sessão", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun MinimalHorizontalTimeline(activeColor: Color, currentStep: Int) {
    val steps = 3

    // Cor inativa adaptável (Cinza no Light, Cinza escuro no Dark)
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..steps) {
                val isActive = i <= currentStep

                Box(
                    modifier = Modifier
                        .size(if (isActive) 14.dp else 10.dp)
                        .background(
                            color = if (isActive) activeColor else inactiveColor,
                            shape = CircleShape
                        )
                        .border(
                            width = if (isActive) 3.dp else 0.dp,
                            color = if (isActive) activeColor.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        )
                )

                if (i < steps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 2.dp)
                            .background(
                                color = if (i < currentStep) activeColor else inactiveColor,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Enviado",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )

            Text(
                text = "Análise",
                style = MaterialTheme.typography.labelSmall,
                color = if (currentStep >= 2) textColor else textColor.copy(alpha = 0.5f)
            )

            Text(
                text = "Decisão",
                style = MaterialTheme.typography.labelSmall,
                color = if (currentStep >= 3) textColor else textColor.copy(alpha = 0.5f)
            )
        }
    }
}