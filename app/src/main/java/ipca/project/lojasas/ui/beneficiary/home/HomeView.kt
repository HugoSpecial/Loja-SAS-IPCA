package ipca.project.lojasas.ui.beneficiary.home

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

@Composable
fun HomeView(
    navController: NavController,
    viewModel: BeneficiaryHomeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val uiState = viewModel.uiState.value
    val categories = uiState.allowedCategories

    val cartItems = CartManager.cartItems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fundo adaptável (Cinza/Preto)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOGO ---
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo",
            modifier = Modifier
                .height(80.dp)
                .padding(bottom = 16.dp)
        )

        // --- LOADING / ERRO ---
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (!uiState.error.isNullOrEmpty()) {
            Text(
                text = "Erro: ${uiState.error}",
                color = MaterialTheme.colorScheme.error // Vermelho do tema
            )
        } else if (categories.isEmpty()) {
            Box(modifier = Modifier.height(300.dp)) {
                EmptyState(
                    message = "A sua candidatura não tem produtos atribuídos.",
                    icon = Icons.Outlined.ShoppingCart
                )
            }
        } else {
            // --- LISTA DE PRODUTOS ---
            categories.forEachIndexed { index, category ->
                val productsByCategory = uiState.products.filter {
                    it.category.trim().equals(category, ignoreCase = true)
                }

                // Título da Categoria
                Text(
                    text = "${category.replaceFirstChar { it.uppercase() }}:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, // Preto (Light) ou Branco (Dark)
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                if (productsByCategory.isNotEmpty()) {
                    val chunked = productsByCategory.chunked(2)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { product ->
                                val isSelected = cartItems.containsKey(product.docId)

                                ProductCardSelectable(
                                    product = product,
                                    isSelected = isSelected,
                                    onClick = {
                                        CartManager.toggleProduct(product.docId)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        text = "Sem produtos disponíveis nesta categoria.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), // Cinza adaptável
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (index < categories.lastIndex) Spacer(modifier = Modifier.height(24.dp))
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

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
            containerColor = MaterialTheme.colorScheme.surface // Branco (Light) ou Preto (Dark)
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
                color = MaterialTheme.colorScheme.onSurface, // Texto adaptável
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
                    // Fundo cinza clarinho para placeholder (adaptável)
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
                    // Se selecionado: Vermelho (Erro/Remover). Se não: Verde (Primary/Adicionar)
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