package ipca.project.lojasas.ui.collaborator.delivery

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Product

// --- CORES ---
val CabazLightBg = Color(0xFFF7F9F8)
val TextDark = Color(0xFF3E3448)
val ButtonGreen = Color(0xFF438E63)
val ButtonGray = Color(0xFFAAB0AD)
val ButtonRemoveRed = Color(0xFFE57373)
val IpcaRed = Color(0xFFB71C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentDeliveryView(
    navController: NavController,
    viewModel: UrgentDeliveryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CabazLightBg)
        // Removemos o padding horizontal aqui para o header tocar nas bordas,
        // aplicamos o padding apenas no conteúdo abaixo
    ) {
        // --- NOVO CABEÇALHO (Igual ao DeliveryListView) ---
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
                    tint = TextDark, // Usamos TextDark para contrastar com o fundo claro
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp) // Compensação para centrar o logo visualmente
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

        // --- CONTEÚDO PRINCIPAL (Com padding lateral recuperado) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {

            Text("Entrega Urgente", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = IpcaRed)
            Text("Saída rápida de stock.", fontSize = 16.sp, color = TextGray, modifier = Modifier.padding(bottom = 24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

                // --- SEÇÃO 1: BENEFICIÁRIO ---
                item {
                    Text("Quem recebe?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(bottom = 8.dp))

                    Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = state.beneficiaryName,
                                    onValueChange = { viewModel.onBeneficiaryNameChange(it) },
                                    placeholder = { Text("Nome do Beneficiário") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        if(state.isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = IpcaRed)
                                        else Icon(Icons.Default.Search, null, tint = TextGray)
                                    },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IpcaRed,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }

                        if (state.searchResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .padding(top = 85.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                LazyColumn {
                                    items(state.searchResults) { user ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.onUserSelected(user) }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(user.name ?: "Sem nome", fontWeight = FontWeight.SemiBold, color = TextDark)
                                        }
                                        HorizontalDivider(color = CabazLightBg)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SEÇÃO 2: PRODUTOS ---
                item {
                    Text("Produtos (${state.cart.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }

                if (state.cart.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Lista vazia.", color = TextGray)
                        }
                    }
                } else {
                    items(state.cart) { item ->
                        val stock = item.product.batches.sumOf { it.quantity }
                        ProductCardItem(
                            product = item.product,
                            quantity = item.quantity,
                            stock = stock,
                            onAdd = { if (item.quantity < stock) viewModel.addToCart(item.product, 1) },
                            onRemove = {
                                if (item.quantity == 1) viewModel.removeFromCart(item)
                                else viewModel.addToCart(item.product, -1)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // --- SEÇÃO 3: AÇÕES ---
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Produto", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.error != null) {
                        Text(state.error!!, color = IpcaRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    if (state.cart.isNotEmpty()) {
                        Button(
                            onClick = { if (!state.isLoading) viewModel.submitUrgentDelivery { navController.popBackStack() } },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IpcaRed)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Finalizar Entrega", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            ProductSelectionSheet(
                products = state.products,
                onAdd = { p ->
                    viewModel.addToCart(p, 1)
                    showBottomSheet = false
                }
            )
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun ProductCardItem(
    product: Product,
    quantity: Int,
    stock: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(product.imageUrl) {
        if (product.imageUrl.isNotEmpty()) {
            try {
                val cleanBase64 = if (product.imageUrl.contains(",")) product.imageUrl.substringAfter(",") else product.imageUrl
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                imageBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (e: Exception) {}
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(bitmap = imageBitmap!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(product.name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = product.name, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("Stock: $stock", fontSize = 10.sp, color = TextGray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(
                    icon = if (quantity == 1) Icons.Default.Delete else ImageVector.vectorResource(id = R.drawable.sub_round),
                    backgroundColor = if (quantity == 1) ButtonRemoveRed else ButtonGreen,
                    onClick = onRemove
                )
                Text(text = "$quantity", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.padding(horizontal = 12.dp))
                QuantityButton(icon = Icons.Default.Add, backgroundColor = if (quantity >= stock) ButtonGray else ButtonGreen, onClick = onAdd)
            }
        }
    }
}

@Composable
fun QuantityButton(icon: ImageVector, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(backgroundColor).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ProductSelectionSheet(
    products: List<Product>,
    onAdd: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {

        Text("Escolha o Produto", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Pesquisar...") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        LazyColumn(modifier = Modifier.heightIn(max = 400.dp).padding(top = 12.dp)) {
            val filteredList = products.filter { it.name.contains(searchQuery, true) }

            items(filteredList) { p ->
                var listImage by remember(p.docId) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(p.imageUrl) {
                    if (p.imageUrl.isNotEmpty()) {
                        try {
                            val clean = if (p.imageUrl.contains(",")) p.imageUrl.substringAfter(",") else p.imageUrl
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            listImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (e: Exception) {}
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAdd(p) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (listImage != null) {
                            Image(
                                bitmap = listImage!!,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(p.name.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextGray)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text(
                            "${p.batches.sumOf { it.quantity }} un.",
                            color = ButtonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                HorizontalDivider(color = CabazLightBg)
            }
        }
    }
}