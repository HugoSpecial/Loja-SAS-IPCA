package ipca.project.lojasas.ui.collaborator.stock

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Locale

// --- CORES DO TEMA ---
val IpcaGreen = Color(0xFF438F56)
val BackgroundGray = Color(0xFFF9F9F9)

@Composable
fun StockView(
    navController: NavController,
    viewModel: StockViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val filteredItems = viewModel.getFilteredItems()
    var selectedItem by remember { mutableStateOf<Product?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categories = listOf("Todos", "Alimentos", "Higiene", "Limpeza")

    // --- DIÁLOGOS ---
    if (selectedItem != null) {
        ProductBatchesDialog(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onEdit = {
                val id = selectedItem!!.docId
                navController.navigate("product?productId=$id")
                selectedItem = null
            },
            onDeleteRequest = { showDeleteConfirm = true }
        )
    }

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
                            selectedItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
            containerColor = Color.White
        )
    }

    // --- ECRÃ PRINCIPAL ---
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("product") },
                containerColor = IpcaGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(horizontal = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_sas),
                contentDescription = "Logo IPCA SAS",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO "Stock"
            Text(
                text = "Stock",
                color = IpcaGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Gerencie os produtos e validades.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // BARRA DE PESQUISA
            OutlinedTextField(
                value = state.searchText,
                onValueChange = { viewModel.onSearchTextChange(it) },
                placeholder = { Text("Pesquisar", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = IpcaGreen,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = IpcaGreen
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // FILTROS (CHIPS)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == state.selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) IpcaGreen else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) IpcaGreen else Color.Gray,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.onCategoryChange(category) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GRID DE PRODUTOS
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IpcaGreen)
                }
            } else if (filteredItems.isEmpty()) {
                EmptyState(
                    message = "Nenhum produto encontrado.",
                    icon = Icons.Outlined.ShoppingCart
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredItems) { item ->
                        ProductCard(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(item: Product, onClick: () -> Unit) {
    val totalQuantity = item.batches.sumOf { it.quantity }
    val validitiesCount = item.batches.count { it.validity != null && it.quantity > 0 }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Imagem do Produto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    val bitmap = remember(item.imageUrl) {
                        try {
                            val decodedString = Base64.decode(item.imageUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Sem imagem",
                        tint = Color.LightGray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // UPDATE: Textos com Ellipsis
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = IpcaGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.category,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Total: $totalQuantity un",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (validitiesCount > 0) "$validitiesCount validades" else "Sem validade",
                fontSize = 12.sp,
                color = if (validitiesCount > 0) IpcaGreen else Color.Gray
            )
        }
    }
}

@Composable
fun ProductBatchesDialog(
    item: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = IpcaGreen,
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis, // UPDATE
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray)
                    }
                    IconButton(onClick = onDeleteRequest) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = Color(0xFFD32F2F))
                    }
                }
            }
        },
        text = {
            Column {
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Text("Detalhes por validade:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    val batches = item.batches
                        .filter { it.quantity > 0 }
                        .sortedBy { it.validity }

                    if (batches.isEmpty()) {
                        item {
                            Text(
                                "Sem stock disponível.",
                                fontSize = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.Gray
                            )
                        }
                    }

                    items(batches) { batch ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundGray),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Validade", fontSize = 11.sp, color = Color.Gray)
                                    val dateStr = batch.validity?.let {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                    } ?: "Sem data"

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                }

                                Surface(
                                    color = IpcaGreen,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${batch.quantity} un",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
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
                Text("Fechar", color = IpcaGreen, fontSize = 16.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}