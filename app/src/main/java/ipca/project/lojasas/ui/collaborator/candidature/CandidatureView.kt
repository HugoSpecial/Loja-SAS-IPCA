/*package ipca.project.lojasas.ui.collaborator.candidature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import java.text.SimpleDateFormat
import java.util.Locale

val BackgroundGray = Color(0xFFF5F6F8)
val TextGray = Color(0xFF8C8C8C)

@Composable
fun CandidatureListView(
    navController: NavController,
    viewModel: CandidatureViewModel = viewModel()
) {
    val state = viewModel.uiState.value

    // 1. Estado para controlar o filtro selecionado (null = Todas)
    var selectedFilter by remember { mutableStateOf<CandidatureState?>(null) }

    // Lógica de filtragem da lista
    val filteredList = remember(state.candidaturas, selectedFilter) {
        if (selectedFilter == null) {
            state.candidaturas
        } else {
            state.candidaturas.filter { it.state == selectedFilter }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {

        // --- CABEÇALHO (Seta e Logótipo) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Cabeçalho IPCA SAS",
                    modifier = Modifier
                        .heightIn(max = 55.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // --- CONTEÚDO DA PÁGINA ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Título Principal
            Text(
                text = "Gestão de candidaturas",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            // Subtítulo
            Text(
                text = "Aqui pode consultar todas as candidaturas.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contador (Baseado na lista filtrada ou geral? Geralmente mostra-se o total de pendentes geral)
            val pendingColor = Color(0xFFEF6C00)
            Text(
                text = if (state.pendingCount == 1) "1 pendente" else "${state.pendingCount} pendentes",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (state.pendingCount > 0) pendingColor else Color.LightGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- 2. BARRA DE FILTROS HORIZONTAL ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()), // Permite scroll se o ecrã for pequeno
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipButton(
                    text = "Todas",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterChipButton(
                    text = "Pendentes",
                    isSelected = selectedFilter == CandidatureState.PENDENTE,
                    onClick = { selectedFilter = CandidatureState.PENDENTE }
                )
                FilterChipButton(
                    text = "Aceites",
                    isSelected = selectedFilter == CandidatureState.ACEITE,
                    onClick = { selectedFilter = CandidatureState.ACEITE }
                )
                FilterChipButton(
                    text = "Rejeitadas",
                    isSelected = selectedFilter == CandidatureState.REJEITADA,
                    onClick = { selectedFilter = CandidatureState.REJEITADA }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LISTA (Usa a filteredList) ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state.error != null) {
                    Text(
                        text = state.error ?: "Erro",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredList.isEmpty()) {
                    Text(
                        text = "Não existem candidaturas neste filtro.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredList) { item ->
                            CandidatureCard(
                                candidatura = item,
                                onClick = {
                                    if (item.docId.isNotEmpty()) {
                                        navController.navigate("candidature_details/${item.docId}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTE AUXILIAR PARA O BOTÃO DE FILTRO ---
@Composable
fun FilterChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (isSelected) Color.White else TextGray
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(50), // Redondo tipo "Chip"
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp) // Altura mais compacta
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CandidatureCard(
    candidatura: Candidature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            // Linha Superior: Tipo (GERAL) e Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidatura.type?.name ?: "GERAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(estado = candidatura.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email do Candidato
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Candidato: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (candidatura.email.isNotEmpty()) candidatura.email else "Sem Email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }

            // Curso
            if (!candidatura.course.isNullOrEmpty()) {
                Text(
                    text = "Curso: ${candidatura.course}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linha Divisória Fina
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))

            Spacer(modifier = Modifier.height(8.dp))

            // --- 3. LÓGICA DA DATA (Submetido vs Avaliado) ---
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

            // Define o texto e a data baseados no estado
            val (labelText, dateObj) = when (candidatura.state) {
                CandidatureState.PENDENTE -> "Submetido a:" to candidatura.creationDate
                else -> "Avaliado a:" to candidatura.evaluationDate
            }

            val dataStr = dateObj?.let { dateFormat.format(it) } ?: "--"

            Text(
                text = "$labelText $dataStr",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun StatusBadge(estado: CandidatureState) {
    val (backgroundColor, contentColor) = when (estado) {
        CandidatureState.PENDENTE -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        CandidatureState.ACEITE -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        CandidatureState.REJEITADA -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = estado.name.replace("_", " "),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold
        )
    }
}

 */

package ipca.project.lojasas.ui.collaborator.candidature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.CandidatureState
import java.text.SimpleDateFormat
import java.util.Locale

// --- PALETA DE CORES ---
val IpcaDarkTeal = Color(0xFF005A49)
val BgLight = Color(0xFFF5F7FA)
val TextGray = Color(0xFF95A5A6)
val BorderColor = Color(0xFFE0E0E0)

@Composable
fun CandidatureListView(
    navController: NavController,
    viewModel: CandidatureViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var selectedFilter by remember { mutableStateOf<CandidatureState?>(null) }

    val filteredList = remember(state.candidaturas, selectedFilter) {
        if (selectedFilter == null) state.candidaturas
        else state.candidaturas.filter { it.state == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {

        // --- CABEÇALHO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = IpcaDarkTeal,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Cabeçalho IPCA SAS",
                    modifier = Modifier
                        .heightIn(max = 55.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // --- CONTEÚDO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            Spacer(modifier = Modifier.height(16.dp))

            // Título e Contador
            Text(
                text = "Candidaturas",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "${filteredList.size} registos encontrados",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- FILTROS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SoftTicketFilter("Todas", selectedFilter == null) { selectedFilter = null }
                SoftTicketFilter("Pendentes", selectedFilter == CandidatureState.PENDENTE) { selectedFilter = CandidatureState.PENDENTE }
                SoftTicketFilter("Aceites", selectedFilter == CandidatureState.ACEITE) { selectedFilter = CandidatureState.ACEITE }
                SoftTicketFilter("Rejeitadas", selectedFilter == CandidatureState.REJEITADA) { selectedFilter = CandidatureState.REJEITADA }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LISTA ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = IpcaGreen
                    )
                } else if (state.error != null) {
                    Text(
                        text = state.error ?: "Erro",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredList.isEmpty()) {
                    Text(
                        text = "Não existem candidaturas.",
                        color = TextGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filteredList) { item ->
                            SoftCandidatureCard(
                                candidatura = item,
                                onClick = {
                                    if (item.docId.isNotEmpty()) {
                                        navController.navigate("candidature_details/${item.docId}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- CARD ATUALIZADO ---
@Composable
fun SoftCandidatureCard(
    candidatura: Candidature,
    onClick: () -> Unit
) {
    val (accentColor, statusText) = when (candidatura.state) {
        CandidatureState.PENDENTE -> Color(0xFFF39C12) to "Pendente"
        CandidatureState.ACEITE -> IpcaGreen to "Aceite"
        CandidatureState.REJEITADA -> Color(0xFFC0392B) to "Rejeitada"
    }

    // DATA COMPLETA (Sem hora)
    val dateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
    val dateToUse = if (candidatura.state == CandidatureState.PENDENTE) candidatura.creationDate else candidatura.evaluationDate
    val dateStr = dateToUse?.let { dateFormat.format(it) } ?: "--"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // LADO ESQUERDO
            Column(modifier = Modifier.weight(1f)) {

                // Badge de Estado
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tipo (Título)
                Text(
                    text = candidatura.type?.name ?: "Geral",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                // Email
                if (candidatura.email.isNotEmpty()) {
                    Text(
                        text = candidatura.email,
                        fontSize = 13.sp,
                        color = TextGray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Data Completa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.calendar_outline),
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Divisória Vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color(0xFFF0F0F0))
            )

            // LADO DIREITO (Apenas a Seta)
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Ver detalhes",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// --- FILTRO ---
@Composable
fun SoftTicketFilter(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) IpcaDarkTeal else Color.White
    val txt = if (isSelected) Color.White else TextGray
    val border = if (isSelected) null else BorderStroke(1.dp, BorderColor)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .then(if (border != null) Modifier.padding(1.dp) else Modifier)
    ) {
        if (border != null) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(50),
                border = border,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = text,
                    color = txt,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            Text(
                text = text,
                color = txt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}