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

    val selectedProducts = remember { mutableStateMapOf<String, Boolean>() }

    val categories = listOf("alimentar", "higiene", "limpeza")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logo",
            modifier = Modifier
                .height(80.dp)
                .padding(bottom = 16.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (!uiState.error.isNullOrEmpty()) {
            Text("Erro: ${uiState.error}", color = Color.Red)
        } else {
            categories.forEachIndexed { index, category ->
                val productsByCategory = uiState.products.filter { it.category == category }

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
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (index < categories.lastIndex) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (selectedProducts.any { it.value }) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "selectedProducts",
                            selectedProducts.filter { it.value }.keys.toList()
                        )
                        navController.navigate("newbasket")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continuar", fontSize = 16.sp)
                }
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
            .height(220.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
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
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(product.imageUrl) {
                try {
                    val decodedBytes = Base64.decode(product.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageBitmap = bitmap?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (imageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.Gray else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(36.dp)
                    .width(120.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(if (isSelected) "Adicionado" else "Adicionar", fontSize = 14.sp)
            }
        }
    }
}
