package ipca.project.lojasas.ui.colaborator.history

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollaboratorHistoryView(
    navController: NavController,
    viewModel: CollatorHistoryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var selectedItem by remember { mutableStateOf<HistoryUiItem?>(null) }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo IPCA SAS",
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Histórico",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Toque num item para ver detalhes.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.typeLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = item.typeColor
                )
                Text(
                    text = dateFormat.format(item.date),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Beneficiário: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.beneficiaryName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = item.infoText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = item.statusBgColor,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = item.statusLabel,
                    color = item.statusTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Detalhes do ${item.typeLabel}",
                    fontWeight = FontWeight.Bold,
                    color = item.typeColor
                )
                Text(
                    text = dateFormat.format(item.date),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Beneficiário: ${item.beneficiaryName}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Produtos:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (item.productsList.isEmpty()) {
                    Text(
                        text = "Nenhum produto listado.",
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(item.productsList) { product ->

                            // --- LINHA DO PRODUTO COM IMAGEM ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Caixa de Imagem
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageBitmap = remember(product.imageUrl) {
                                        try {
                                            if (!product.imageUrl.isNullOrBlank()) {
                                                val decodedBytes = Base64.decode(product.imageUrl, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                                            } else null
                                        } catch (e: Exception) { null }
                                    }

                                    if (imageBitmap != null) {
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingCart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 2. Nome e Quantidade
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${product.quantity} un",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}