package ipca.project.lojasas.ui.collaborator.stock

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.models.ProductBatch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockView(
    navController: NavController,
    viewModel: StockViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val filteredItems = viewModel.getFilteredItems()
    var selectedItem by remember { mutableStateOf<Product?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categories = listOf("Todos", "Fora de validade", "Alimentos", "Higiene", "Limpeza")

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
            title = { Text("Apagar Produto", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Tem a certeza que deseja apagar '${selectedItem!!.name}' e todo o seu stock?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(selectedItem!!.docId) {
                            showDeleteConfirm = false
                            selectedItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // --- ECRÃ PRINCIPAL ---
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // 1. CABEÇALHO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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

                    Text(
                        text = "Stock",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Gerencie os produtos e validades.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                }
            }

            // 2. STICKY HEADER (Filtros)
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        OutlinedTextField(
                            value = state.searchText,
                            onValueChange = { viewModel.onSearchTextChange(it) },
                            placeholder = { Text("Pesquisar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == state.selectedCategory
                            val isExpiredCat = category == "Fora de validade"

                            val activeColor = if (isExpiredCat) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                            val chipBg = if (isSelected) activeColor else Color.Transparent
                            val chipBorder = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .border(width = 1.dp, color = chipBorder, shape = RoundedCornerShape(20.dp))
                                    .clickable { viewModel.onCategoryChange(category) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(text = category, color = chipText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    Divider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }

            // 3. CONTEÚDO
            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    // --- AQUI ESTÁ A ALTERAÇÃO ---
                    // Substituí o EmptyState por um Box com Text simples
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum produto encontrado nesta categoria.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredItems.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProductCard(
                            item = rowItems[0],
                            currentCategory = state.selectedCategory,
                            onClick = { selectedItem = rowItems[0] },
                            modifier = Modifier.weight(1f)
                        )

                        if (rowItems.size > 1) {
                            ProductCard(
                                item = rowItems[1],
                                currentCategory = state.selectedCategory,
                                onClick = { selectedItem = rowItems[1] },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// --- PRODUCT CARD ---
@Composable
fun ProductCard(
    item: Product,
    currentCategory: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validBatches = item.batches.filter { it.quantity > 0 && isDateValid(it.validity) }
    val expiredBatches = item.batches.filter { it.quantity > 0 && !isDateValid(it.validity) }

    val validQuantity = validBatches.sumOf { it.quantity }
    val expiredQuantity = expiredBatches.sumOf { it.quantity }

    val showExpiredInfo = currentCategory == "Fora de validade"

    val quantityToShow = if (showExpiredInfo) expiredQuantity else validQuantity
    val labelToShow = if (showExpiredInfo) "Stock Expirado" else "Stock Válido"
    val colorToShow = if (showExpiredInfo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.category,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$labelToShow: $quantityToShow un",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorToShow
            )

            if (!showExpiredInfo && expiredQuantity > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tem $expiredQuantity expirados",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// --- DIÁLOGO DE DETALHES ---
@Composable
fun ProductBatchesDialog(
    item: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val validBatches = item.batches.filter { it.quantity > 0 && isDateValid(it.validity) }.sortedBy { it.validity }
    val expiredBatches = item.batches.filter { it.quantity > 0 && !isDateValid(it.validity) }.sortedBy { it.validity }

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
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
                    IconButton(onClick = onDeleteRequest) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        },
        text = {
            Column {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    if (validBatches.isNotEmpty()) {
                        item { Text("Lotes Válidos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        items(validBatches) { batch -> BatchCard(batch, isExpired = false) }
                    } else if (expiredBatches.isEmpty()) {
                        item { Text("Sem stock disponível.", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                    }

                    if (expiredBatches.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Lotes Expirados", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        items(expiredBatches) { batch -> BatchCard(batch, isExpired = true) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun BatchCard(batch: ProductBatch, isExpired: Boolean) {
    val bgColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.background
    val textColor = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(if(isExpired) "Expirou em" else "Validade", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                val dateStr = batch.validity?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "Sem data"
                Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            }
            Surface(
                color = if(isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${batch.quantity} un",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

fun isDateValid(date: Date?): Boolean {
    if (date == null) return false
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val checkDate = Calendar.getInstance()
    checkDate.time = date
    checkDate.set(Calendar.HOUR_OF_DAY, 0)
    checkDate.set(Calendar.MINUTE, 0)
    checkDate.set(Calendar.SECOND, 0)
    checkDate.set(Calendar.MILLISECOND, 0)

    return !checkDate.before(today)
}