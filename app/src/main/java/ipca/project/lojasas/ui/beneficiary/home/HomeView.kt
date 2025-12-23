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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.ProductTest

@Composable
fun HomeView(
    navController: NavController,
    viewModel: BeneficiaryHomeViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val uiState = viewModel.uiState.value

    // Mapa para guardar quais produtos foram selecionados (ID -> Boolean)
    val selectedProducts = remember { mutableStateMapOf<String, Boolean>() }

    // Esta lista vem do ViewModel, filtrada pela Candidatura (ex: só "Alimentos" e "Higiene")
    val categories = uiState.allowedCategories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

        // --- GESTÃO DE ESTADOS DE CARREGAMENTO/ERRO ---
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (!uiState.error.isNullOrEmpty()) {
            Text("Erro: ${uiState.error}", color = Color.Red)
        } else if (categories.isEmpty()) {
            // Caso o utilizador não tenha categorias atribuídas na candidatura
            Text(
                text = "A sua candidatura não possui categorias de produtos atribuídas.",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            // --- LOOP PELAS CATEGORIAS PERMITIDAS ---
            categories.forEachIndexed { index, category ->

                // Filtra os produtos daquela categoria (Ignorando Maiúsculas/Minúsculas)
                val productsByCategory = uiState.products.filter {
                    it.category.trim().equals(category, ignoreCase = true)
                }

                // Título da Categoria
                Text(
                    text = "${category.replaceFirstChar { it.uppercase() }}:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                if (productsByCategory.isNotEmpty()) {
                    // Divide os produtos em linhas de 2 (Grid)
                    val chunked = productsByCategory.chunked(2)

                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { product ->
                                ProductCardSelectable(
                                    product = product,
                                    isSelected = selectedProducts[product.docId] == true,
                                    onClick = {
                                        val current = selectedProducts[product.docId] ?: false
                                        selectedProducts[product.docId] = !current
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Se a linha tiver apenas 1 produto, adiciona um espaço vazio para manter o tamanho
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    // Mensagem caso a categoria exista mas não tenha stock
                    Text(
                        text = "De momento não existem produtos disponíveis.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Espaçamento entre categorias, exceto na última
                if (index < categories.lastIndex) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // --- BOTÃO CONTINUAR (Só aparece se houver produtos selecionados) ---
            if (selectedProducts.any { it.value }) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        // Passa a lista de IDs selecionados para o próximo ecrã
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "selectedProducts",
                            selectedProducts.filter { it.value }.keys.toList()
                        )
                        navController.navigate("newbasket")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Continuar", fontSize = 18.sp, color = Color.White)
                }
                // Espaço extra no final para o scroll não cortar o botão
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProductCardSelectable(
    product: ProductTest,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(240.dp) // Altura fixa para uniformidade
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Nome do Produto
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2, // Limita a 2 linhas para não estragar o layout
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            // Imagem do Produto (Base64)
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(product.imageUrl) {
                try {
                    // Verifica se a string tem cabeçalho "data:image..." e remove se necessário
                    val cleanBase64 = if (product.imageUrl.contains(",")) {
                        product.imageUrl.split(",")[1]
                    } else {
                        product.imageUrl
                    }

                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageBitmap = bitmap?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Ocupa o espaço disponível
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f)), // Fundo cinza claro enquanto carrega/sem imagem
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder se não houver imagem
                    Text("Sem Imagem", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Adicionar / Adicionado
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, // Verde se selecionado
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp), // Remove padding interno para caber texto
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isSelected) "Selecionado" else "Adicionar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}