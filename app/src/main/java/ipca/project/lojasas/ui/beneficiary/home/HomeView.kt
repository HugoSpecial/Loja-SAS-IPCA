package ipca.project.lojasas.ui.beneficiary.home

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import ipca.project.lojasas.ui.beneficiary.CartManager
import ipca.project.lojasas.ui.components.EmptyState

@OptIn(ExperimentalFoundationApi::class) // Necessário para stickyHeader
@Composable
fun HomeView(
    navController: NavController,
    viewModel: BeneficiaryHomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState.value

    // Obtém produtos filtrados pelo ViewModel
    val filteredItems = viewModel.getFilteredProducts()

    // Categorias para os filtros (chips)
    val displayCategories = listOf("Todos") + uiState.allowedCategories

    // Mantém a referência ao CartManager (Assumindo que cartItems é um MutableStateMap ou similar)
    val cartItems = CartManager.cartItems

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {

            // --- 1. CABEÇALHO (Scrollável) ---
            // Logo e Texto de boas-vindas sobem e desaparecem
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_sas),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .height(80.dp)
                            .padding(bottom = 16.dp)
                    )

                    // Saudação alinhada à esquerda
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (uiState.userName.isNotEmpty()) "Olá, ${uiState.userName}" else "Olá,",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Estes são os produtos disponíveis para si.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // --- 2. CABEÇALHO FIXO (Sticky Header) ---
            // Barra de pesquisa e Filtros colam no topo
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background) // Fundo opaco para tapar o conteúdo ao passar por baixo
                        .padding(vertical = 8.dp)
                ) {
                    // Barra de Pesquisa
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = uiState.searchText,
                            onValueChange = { viewModel.onSearchTextChange(it) },
                            placeholder = {
                                Text("Pesquisar produto...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filtros (Chips)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(displayCategories) { category ->
                            val isSelected = category.equals(uiState.selectedCategory, ignoreCase = true)
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
                                Text(
                                    text = category,
                                    color = chipText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }

            // --- 3. CONTEÚDO (Grelha de Produtos) ---
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (!uiState.error.isNullOrEmpty()) {
                item {
                    Text(text = "Erro: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    EmptyState(
                        message = "Nenhum produto encontrado.",
                        icon = Icons.Outlined.ShoppingCart
                    )
                }
            } else {
                // Criação da Grelha (2 colunas) manualmente
                items(filteredItems.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Item 1
                        val product1 = rowItems[0]
                        val isSelected1 = cartItems.containsKey(product1.docId)
                        ProductCardSelectable(
                            product = product1,
                            isSelected = isSelected1,
                            onClick = { CartManager.toggleProduct(product1.docId) },
                            modifier = Modifier.weight(1f)
                        )

                        // Item 2 (se existir)
                        if (rowItems.size > 1) {
                            val product2 = rowItems[1]
                            val isSelected2 = cartItems.containsKey(product2.docId)
                            ProductCardSelectable(
                                product = product2,
                                isSelected = isSelected2,
                                onClick = { CartManager.toggleProduct(product2.docId) },
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

// O Card mantém-se o mesmo que enviaste
@Composable
fun ProductCardSelectable(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(240.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(product.imageUrl) {
                if (product.imageUrl.isNotEmpty()) {
                    try {
                        val cleanBase64 = if (product.imageUrl.contains(",")) product.imageUrl.split(",")[1] else product.imageUrl
                        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageBitmap = bitmap?.asImageBitmap()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Sem Imagem",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isSelected) "Remover" else "Adicionar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}