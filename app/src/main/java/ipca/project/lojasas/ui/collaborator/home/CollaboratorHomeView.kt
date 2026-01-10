package ipca.project.lojasas.ui.collaborator.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R

// --- PALETA DE CORES ---
val IpcaGreen = Color(0xFF00864F)
val IpcaDarkTeal = Color(0xFF005A49)
val IpcaOlive = Color(0xFF689F38)
val IpcaBlueGray = Color(0xFF455A64)
val IpcaBlackGreen = Color(0xFF1B2E25)
val IpcaRed = Color(0xFFB71C1C)

@Composable
fun CollaboratorHomeView(
    navController: NavController,
    viewModel: CollaboratorHomeViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {

        // --- 1. CABEÇALHO ---
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logótipo",
            modifier = Modifier.fillMaxWidth().height(80.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. BOAS-VINDAS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Olá, ${state.userName}",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sair",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "Resumo da atividade hoje.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- 3. RESUMO (DASHBOARD) ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(
                    title = "Entregas Hoje",
                    count = state.deliveriesTodayCount.toString(),
                    icon = ImageVector.vectorResource(id = R.drawable.delivery),
                    iconColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Campanhas",
                    count = state.activeCampaignsCount.toString(),
                    icon = ImageVector.vectorResource(id = R.drawable.megaphone),
                    iconColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(
                    title = "Candidaturas",
                    count = state.pendingCount.toString(),
                    icon = ImageVector.vectorResource(id = R.drawable.file_dock),
                    iconColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Pedidos",
                    count = state.pendingSolicitationsCount.toString(),
                    icon = ImageVector.vectorResource(id = R.drawable.shopping_cart),
                    iconColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 4. ÁREA DE GESTÃO ---
        Text(
            text = "Menu de Gestão",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "(Pressione longo para ver texto completo)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), // Cor adaptável
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // LINHA 1
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "Entregas",
                    subtitle = "Gestão Geral",
                    icon = ImageVector.vectorResource(id = R.drawable.delivery),
                    backgroundColor = IpcaOlive,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("deliveries") }
                )
                ActionCard(
                    title = "Pedidos",
                    subtitle = "Validação",
                    icon = ImageVector.vectorResource(id = R.drawable.shopping_cart),
                    backgroundColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("orders") }
                )
            }

            // LINHA 2
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "Doações",
                    subtitle = "Entradas",
                    icon = ImageVector.vectorResource(id = R.drawable.donate),
                    backgroundColor = IpcaBlueGray,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("donations_list") }
                )
                ActionCard(
                    title = "Campanhas",
                    subtitle = "Gestão",
                    icon = ImageVector.vectorResource(id = R.drawable.megaphone),
                    backgroundColor = IpcaBlackGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("campaigns") }
                )
            }

            // LINHA 3
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "Candidaturas",
                    subtitle = "Processos",
                    icon = ImageVector.vectorResource(id = R.drawable.file_dock),
                    backgroundColor = IpcaGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("candidature_list") }
                )
                ActionCard(
                    title = "Urgente",
                    subtitle = "Entrega Rápida",
                    icon = Icons.Default.Warning,
                    backgroundColor = IpcaRed,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("urgent_delivery") }
                )
            }

            // LINHA 4
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "PDF's",
                    subtitle = "Relatórios",
                    icon = ImageVector.vectorResource(id = R.drawable.file_dock),
                    backgroundColor = IpcaBlueGray,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("reports_history") }
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    // Popup Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Terminar Sessão", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Tem a certeza que pretende sair?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut()
                    navController.navigate("login") { popUpTo(0) }
                }) {
                    Text("Sair", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

// --- COMPONENTES AUXILIARES ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummaryCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    var showFullText by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .height(105.dp)
                .combinedClickable(
                    onClick = { /* Ação de clique normal */ },
                    onLongClick = { showFullText = true }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = count,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- POPUP PARA TEXTO CORTADO (Adaptado para Modo Escuro) ---
        if (showFullText) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showFullText = false }
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    // CORRIGIDO: Usa a cor da superfície do tema (Branco no claro, Cinza no escuro)
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .width(200.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = count,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // CORRIGIDO
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // CORRIGIDO: Cor de texto secundário adaptável
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var showFullText by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .height(190.dp)
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { showFullText = true }
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-20).dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Normal,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- POPUP DE ACTION CARD (Adaptado para Modo Escuro) ---
        if (showFullText) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showFullText = false }
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    // CORRIGIDO: Usa a cor da superfície do tema
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .width(200.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = IpcaGreen, // Mantém a cor da marca, mas o fundo muda
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // CORRIGIDO: Cor adaptável para subtítulo
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}