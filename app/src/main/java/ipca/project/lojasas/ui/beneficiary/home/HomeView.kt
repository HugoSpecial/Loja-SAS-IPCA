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

@OptIn(ExperimentalFoundationApi::class) // Necessário para usar stickyHeader
@Composable
fun HomeView(
    navController: NavController,
    viewModel: BeneficiaryHomeViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val filteredItems = viewModel.getFilteredProducts()

    // Constrói a lista de filtros (Todos + Categorias permitidas)
    val displayCategories = listOf("Todos") + state.allowedCategories

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {

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
                        text = if (state.userName.isNotEmpty()) "Olá, ${state.userName}" else "Olá,",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Estes são os produtos disponíveis para si.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                }
            }

            // --- 2. STICKY HEADER (Pesquisa e Filtros) ---
            // Este bloco sobe e "cola" no topo do ecrã
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background) // Fundo opaco é essencial
                        .padding(vertical = 8.dp)
                ) {
                    // Barra de Pesquisa
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        OutlinedTextField(
                            value = state.searchText,
                            onValueChange = { viewModel.onSearchTextChange(it) },
                            placeholder = { Text("Pesquisar produto...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
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

                    // Filtros (Chips)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(displayCategories) { category ->
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
                                Text(
                                    text = category,
                                    color = chipText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Separador visual subtil
                    Divider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }

            // --- 3. CONTEÚDO (Grelha de Produtos) ---
            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    EmptyState(
                        message = "Nenhum produto encontrado.",
                        icon = Icons.Outlined.ShoppingCart
                    )
                }
            } else {
                // Divide a lista em pares para simular grelha de 2 colunas
                items(filteredItems.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Primeiro produto
                        BeneficiaryProductCard(
                            item = rowItems[0],
                            onClick = {
                                // Navegação para detalhe:
                                // navController.navigate("product_detail/${rowItems[0].docId}")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Segundo produto (ou espaço vazio para alinhar)
                        if (rowItems.size > 1) {
                            BeneficiaryProductCard(
                                item = rowItems[1],
                                onClick = {
                                    // navController.navigate("product_detail/${rowItems[1].docId}")
                                },
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

// --- COMPONENTE DO CARD DE PRODUTO ---
@Composable
fun BeneficiaryProductCard(
    item: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.clickable { onClick() }
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

            // Nome
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Categoria
            Text(
                text = item.category,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botão/Ação
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Adicionar", fontSize = 12.sp)
            }
        }
    }
}