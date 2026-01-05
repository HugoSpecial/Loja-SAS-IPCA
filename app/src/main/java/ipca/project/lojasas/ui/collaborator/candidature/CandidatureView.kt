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
            .background(MaterialTheme.colorScheme.background) // Adaptável
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

        // --- CONTEÚDO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            Spacer(modifier = Modifier.height(16.dp))

            // Título e Contador
            Text(
                text = "Candidaturas",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${filteredList.size} registos encontrados",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state.error != null) {
                    Text(
                        text = state.error ?: "Erro",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredList.isEmpty()) {
                    Text(
                        text = "Não existem candidaturas.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
        CandidatureState.ACEITE -> MaterialTheme.colorScheme.primary to "Aceite"
        CandidatureState.REJEITADA -> MaterialTheme.colorScheme.error to "Rejeitada"
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Branco ou Cinza Escuro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Borda subtil adaptável
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Email
                if (candidatura.email.isNotEmpty()) {
                    Text(
                        text = candidatura.email,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Data Completa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.calendar_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Divisória Vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )

            // LADO DIREITO (Apenas a Seta)
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Ver detalhes",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// --- FILTRO ---
@Composable
fun SoftTicketFilter(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val txt = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

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