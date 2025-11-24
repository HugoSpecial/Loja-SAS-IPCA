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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.EstadoCandidatura
import ipca.project.lojasas.R // <-- Verifica se este import está correto para o teu projeto
import ipca.project.lojasas.ui.authentication.LoginViewModel
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AwaitCandidatureView(
    navController: NavController,
    viewModelAuth: LoginViewModel = viewModel()
) {
    // --- ESTADOS LOCAIS ---
    var candidatura by remember { mutableStateOf<Candidatura?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- CARREGAR DADOS DO FIREBASE AO INICIAR A VIEW ---
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            try {
                // 1. Ler User para obter o ID da candidatura
                val userDoc = db.collection("users").document(uid).get().await()
                val candId = userDoc.getString("candidature")

                if (!candId.isNullOrEmpty()) {
                    // 2. Ler a Candidatura
                    val candDoc = db.collection("candidatures").document(candId).get().await()
                    if (candDoc.exists()) {
                        val candObj = candDoc.toObject(Candidatura::class.java)
                        candObj?.docId = candDoc.id
                        candidatura = candObj
                    } else {
                        errorMessage = "Candidatura não encontrada."
                    }
                } else {
                    errorMessage = "Nenhuma candidatura associada."
                }
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar: ${e.message}"
            }
        }
        isLoading = false
    }

    // --- VARIÁVEIS DE UI BASEADAS NOS DADOS CARREGADOS ---
    val estadoAtual = candidatura?.estado ?: EstadoCandidatura.PENDENTE
    val motivoRejeicao = candidatura?.motivoAlteracaoEstado ?: "Sem motivo registado."

    val dataSubmissao = try {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        candidatura?.dataCriacao?.let { sdf.format(it) } ?: "Data N/D"
    } catch (e: Exception) { "Data N/D" }

    // --- CORES E TEXTOS ---
    val (statusColor, statusBg, statusText, statusHeadline) = when (estadoAtual) {
        EstadoCandidatura.PENDENTE -> Quadruple(Color(0xFFFFA000), Color(0xFFFFF8E1), "PENDENTE", "A aguardar validação")
        EstadoCandidatura.EM_ANALISE -> Quadruple(Color(0xFF2196F3), Color(0xFFE3F2FD), "EM ANÁLISE", "Em análise técnica")
        EstadoCandidatura.ACEITE -> Quadruple(Color(0xFF4CAF50), Color(0xFFE8F5E9), "ACEITE", "Candidatura Aceite")
        EstadoCandidatura.REJEITADA -> Quadruple(Color(0xFFF44336), Color(0xFFFFEBEE), "RECUSADA", "Candidatura Rejeitada")
    }

    val mensagemAmigavel = when (estadoAtual) {
        EstadoCandidatura.PENDENTE -> "Recebemos o seu pedido. Aguarde a validação dos serviços."
        EstadoCandidatura.EM_ANALISE -> "A nossa equipa técnica está a analisar o seu processo."
        EstadoCandidatura.ACEITE -> "Parabéns! O seu pedido foi aceite. Já pode usufruir dos apoios."
        EstadoCandidatura.REJEITADA -> "Infelizmente o pedido não reuniu as condições necessárias."
    }

    val BackgroundColor = Color(0xFFFAFAFA)
    val TextPrimary = Color(0xFF1F2937)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundColor
    ) { paddingValues ->

        if (isLoading) {
            // TELA DE LOADING
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null && candidatura == null) {
            // TELA DE ERRO (Caso raro)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage!!, color = Color.Red)
            }
        } else {
            // TELA PRINCIPAL (Layout Responsivo)
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
                            painter = painterResource(id = R.drawable.logo_sas), // TEU LOGO
                            contentDescription = "Logo IPCA",
                            modifier = Modifier
                                .height(if (isCompactScreen) 60.dp else 80.dp)
                                .fillMaxWidth(),
                            alignment = Alignment.Center
                        )
                        Spacer(modifier = Modifier.height(if (isCompactScreen) 16.dp else 24.dp))
                        Text(
                            text = "Estado da Candidatura",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactScreen) 20.sp else 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. CARD DE STATUS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isCompactScreen) 20.dp else 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Badge
                            Surface(
                                color = statusBg,
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

                            // Título e Texto
                            Text(
                                text = statusHeadline,
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = if (isCompactScreen) 20.sp else 24.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = mensagemAmigavel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Submetido em $dataSubmissao",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(if (isCompactScreen) 20.dp else 32.dp))

                            // Timeline Visual
                            val stepAtual = when(estadoAtual) {
                                EstadoCandidatura.PENDENTE -> 1
                                EstadoCandidatura.EM_ANALISE -> 2
                                else -> 3
                            }
                            MinimalHorizontalTimeline(activeColor = statusColor, currentStep = stepAtual)

                            // BOTÃO: SE REJEITADA
                            if (estadoAtual == EstadoCandidatura.REJEITADA) {
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
                                    Text(motivoRejeicao, color = TextPrimary, textAlign = TextAlign.Center)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        // Navegar de volta ao form (sem passar dados, o form que se encarregue de buscar se quiser editar)
                                        navController.navigate("candidature_form")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Corrigir Dados")
                                }
                            }

                            // BOTÃO: SE ACEITE
                            if (estadoAtual == EstadoCandidatura.ACEITE) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navController.navigate("home") },
                                    colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ir para a Loja")
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
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

// --- CLASSES AUXILIARES (Para o ficheiro compilar sozinho) ---

@Composable
fun MinimalHorizontalTimeline(activeColor: Color, currentStep: Int) {
    val steps = 3
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..steps) {
                val isActive = i <= currentStep
                Box(
                    modifier = Modifier
                        .size(if (isActive) 14.dp else 10.dp)
                        .background(if (isActive) activeColor else Color(0xFFE5E7EB), CircleShape)
                        .border(if (isActive) 3.dp else 0.dp, if (isActive) activeColor.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                )
                if (i < steps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 2.dp)
                            .background(if (i < currentStep) activeColor else Color(0xFFE5E7EB), RoundedCornerShape(1.dp))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enviado", style = MaterialTheme.typography.labelSmall, color = Color.Black)
            Text("Análise", style = MaterialTheme.typography.labelSmall, color = if(currentStep >= 2) Color.Black else Color.Gray)
            Text("Decisão", style = MaterialTheme.typography.labelSmall, color = if(currentStep >= 3) Color.Black else Color.Gray)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)