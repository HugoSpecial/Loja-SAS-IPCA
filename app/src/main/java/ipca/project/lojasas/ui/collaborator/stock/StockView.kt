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

@OptIn(ExperimentalFoundationApi::class) // Necessário para o stickyHeader
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

    // --- DIÁLOGOS (Mantidos iguais) ---
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

        // Usamos LazyColumn em vez de Column + LazyVerticalGrid
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 80.dp) // Espaço para o FAB não tapar o último item
        ) {

            // 1. CABEÇALHO (Logo e Títulos) - Isto vai fazer scroll e desaparecer
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

            // 2. STICKY HEADER (Barra de Pesquisa e Filtros) - Isto vai fixar no topo
            stickyHeader {
                // É CRUCIAL ter um background aqui, senão os itens passam por trás transparentes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 8.dp) // Padding vertical para dar ar
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
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
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
                    // Linha separadora subtil quando fixa (opcional)
                    Divider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }

            // 3. CONTEÚDO (Grelha simulada)
            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    EmptyState(message = "Nenhum produto encontrado.", icon = Icons.Outlined.ShoppingCart)
                }
            } else {
                // Chunked(2) cria pares de produtos para simular a grelha de 2 colunas
                items(filteredItems.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Primeiro item
                        ProductCard(
                            item = rowItems[0],
                            onClick = { selectedItem = rowItems[0] },
                            modifier = Modifier.weight(1f)
                        )

                        // Segundo item (se existir) ou espaço vazio para alinhar
                        if (rowItems.size > 1) {
                            ProductCard(
                                item = rowItems[1],
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

// Atualizei o ProductCard para aceitar Modifier, para funcionar com weight(1f) na grelha
@Composable
fun ProductCard(
    item: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuantity = item.batches.sumOf { it.quantity }
    val validitiesCount = item.batches.count { it.validity != null && it.quantity > 0 }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier // Usa o modificador passado (importante para o layout da grelha)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Imagem do Produto
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
                        contentDescription = "Sem imagem",
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Total: $totalQuantity un",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (validitiesCount > 0) "$validitiesCount validades" else "Sem validade",
                fontSize = 12.sp,
                color = if (validitiesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// O resto do código (ProductBatchesDialog) mantém-se igual,
// apenas certifique-se de que a função existe no ficheiro.
@Composable
fun ProductBatchesDialog(
    item: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    // ... (O teu código do dialog original aqui) ...
    // Podes copiar o bloco original do teu código para aqui se não o tiveres noutro ficheiro
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
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onDeleteRequest) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Detalhes por validade:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    val batches = item.batches.filter { it.quantity > 0 }.sortedBy { it.validity }
                    if (batches.isEmpty()) {
                        item {
                            Text("Sem stock disponível.", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    items(batches) { batch ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Validade", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    val dateStr = batch.validity?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "Sem data"
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
                                    Text(text = "${batch.quantity} un", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}