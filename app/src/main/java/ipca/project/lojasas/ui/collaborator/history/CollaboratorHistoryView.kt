package ipca.project.lojasas.ui.colaborator.history

import androidx.compose.foundation.Image // <--- Import Adicionado
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource // <--- Import Adicionado
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollaboratorHistoryView(
    navController: NavController,
    viewModel: CollatorHistoryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // --- ESTADO PARA O POP-UP ---
    var selectedItem by remember { mutableStateOf<HistoryUiItem?>(null) }

    // --- POP-UP (DIALOG) ---
    if (selectedItem != null) {
        HistoryDetailsDialog(
            item = selectedItem!!,
            dateFormat = dateFormat,
            onDismiss = { selectedItem = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        // --- LOGO SUBSTITUÍDO ---
        Image(
            painter = painterResource(id = ipca.project.lojasas.R.drawable.logo_sas),
            contentDescription = "Logo IPCA SAS",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )

        // Espaço entre o logo e o título
        Spacer(modifier = Modifier.height(24.dp))

        Text("Histórico", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00864F))
        Text("Toque num item para ver detalhes.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(state.items) { item ->
                HistoryCard(
                    item = item,
                    dateFormat = dateFormat,
                    onClick = { selectedItem = item }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryUiItem,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.typeLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = item.typeColor)
                Text(dateFormat.format(item.date), fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("Benificiário: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Text(item.beneficiaryName, fontSize = 14.sp, color = Color.Black)
            }

            Text(item.infoText, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))

            Spacer(modifier = Modifier.height(8.dp))

            Surface(color = item.statusBgColor, shape = RoundedCornerShape(50)) {
                Text(item.statusLabel, color = item.statusTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun HistoryDetailsDialog(
    item: HistoryUiItem,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Detalhes do ${item.typeLabel}", fontWeight = FontWeight.Bold, color = item.typeColor)
                Text(text = dateFormat.format(item.date), fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column {
                Text(text = "Beneficiário: ${item.beneficiaryName}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Produtos:", fontSize = 14.sp, color = Color.Gray)

                if (item.productsList.isEmpty()) {
                    Text("Nenhum produto listado.", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(item.productsList) { prod ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold)
                                Text(prod)
                            }
                        }
                    }
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