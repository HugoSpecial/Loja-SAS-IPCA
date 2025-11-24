package ipca.project.lojasas.ui.colaborator.candidature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.example.lojasas.models.Candidature
import ipca.example.lojasas.models.CandidatureState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatureListView(
    navController: NavController,
    viewModel: CandidatureViewModel = viewModel()
) {
    val state = viewModel.uiState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gestão de Candidaturas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (state.pendingCount == 1) "1 pendente" else "${state.pendingCount} pendentes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.pendingCount > 0) Color(0xFFEF6C00) else Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else if (state.error != null) {
                Text(
                    text = state.error ?: "Erro",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else if (state.candidaturas.isEmpty()) {
                Text(
                    text = "Não existem candidaturas.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.candidaturas) { item ->
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

@Composable
fun CandidatureCard(
    candidatura: Candidature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // TIPO e ESTADO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidatura.type?.name ?: "GERAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(estado = candidatura.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // EMAIL
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Candidato: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (candidatura.email.isNotEmpty()) candidatura.email else "Sem Email",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // CURSO
            if (!candidatura.course.isNullOrEmpty()) {
                Text(
                    text = "Curso: ${candidatura.course}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(8.dp))

            // DATA
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dataStr = candidatura.creationDate?.let { dateFormat.format(it) } ?: "Data desconhecida"

            Text(
                text = "Submetido a: $dataStr",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = estado.name.replace("_", " "),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}