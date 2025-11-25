package ipca.project.lojasas.ui.colaborator.stock

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.models.StockItem
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockView(
    navController: NavController,
    viewModel: StockViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val filteredItems = viewModel.getFilteredItems()

    // Controla qual produto está aberto no Pop-up
    var selectedItem by remember { mutableStateOf<StockItem?>(null) }

    // Controla o diálogo de confirmação de eliminação
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // --- DIÁLOGO DE DETALHES (LOTES) ---
    if (selectedItem != null) {
        ProductBatchesDialog(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onEdit = {
                // Navega para a edição.
                // Nota: Tens de preparar o teu CreateProductView para receber o ID
                // Exemplo: "product?productId=XYZ"
                navController.navigate("product?productId=${selectedItem!!.docId}")
                selectedItem = null
            },
            onDeleteRequest = {
                // Abre a confirmação
                showDeleteConfirm = true
            }
        )
    }

    // --- DIÁLOGO DE CONFIRMAÇÃO DE ELIMINAÇÃO ---
    if (showDeleteConfirm && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Apagar Produto") },
            text = { Text("Tem a certeza que deseja apagar '${selectedItem!!.name}' e todo o seu stock?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(selectedItem!!.docId) {
                            showDeleteConfirm = false
                            selectedItem = null // Fecha o detalhe também
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Loja Social", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(16.dp)
        ) {
            // Barra de Pesquisa
            OutlinedTextField(
                value = state.searchText,
                onValueChange = { viewModel.onSearchTextChange(it) },
                label = { Text("Pesquisar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum produto encontrado.", color = Color.Gray)
                }
            } else {
                // Grelha de Produtos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        ProductCard(
                            item = item,
                            onClick = { selectedItem = item } // Abre o Pop-up
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(item: StockItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // Imagem
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.LightGray)
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    val bitmap = remember(item.imageUrl) {
                        try {
                            val decodedString = Base64.decode(item.imageUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Sem Imagem", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Total: ", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${item.getTotalQuantity()} un",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (item.batches.size > 1) {
                    Text(
                        text = "${item.batches.size} validades",
                        fontSize = 10.sp,
                        color = Color.Blue
                    )
                }
            }
        }
    }
}

// --- O DIÁLOGO ATUALIZADO (Com Botões de Ação) ---
@Composable
fun ProductBatchesDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // Cabeçalho Personalizado com Título e Botões
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título (limitado para não empurrar os botões)
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Botões de Ação
                Row {
                    // Editar
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray)
                    }
                    // Apagar
                    IconButton(onClick = onDeleteRequest) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = Color.Red)
                    }
                }
            }
        },
        text = {
            Column {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Detalhes por validade:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(item.batches.sortedBy { it.validity }) { batch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Validade", fontSize = 10.sp, color = Color.Gray)
                                    val dateStr = batch.validity?.let {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                    } ?: "Sem data"
                                    Text(dateStr, fontWeight = FontWeight.Bold)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${batch.quantity} un",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Text("Fechar")
            }
        }
    )
}