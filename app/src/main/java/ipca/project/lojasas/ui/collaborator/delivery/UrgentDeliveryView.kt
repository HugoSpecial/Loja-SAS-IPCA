package ipca.project.lojasas.ui.collaborator.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.models.Product

// Cores IPCA (reutilizadas)
val IpcaGreen = Color(0xFF00864F)
val IpcaRed = Color(0xFFB71C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentDeliveryView(
    navController: NavController,
    viewModel: UrgentDeliveryViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    var showProductDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrega Urgente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IpcaRed, // Vermelho para indicar urgência/atenção
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // --- 1. DADOS DO BENEFICIÁRIO ---
                Text("Dados da Entrega", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IpcaRed)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.beneficiaryName,
                    onValueChange = { viewModel.onBeneficiaryNameChange(it) },
                    label = { Text("Nome do Beneficiário") },
                    placeholder = { Text("Ex: João Silva ou Anónimo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IpcaRed,
                        focusedLabelColor = IpcaRed
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. LISTA DE PRODUTOS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Produtos a Entregar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IpcaRed)

                    // Botão Adicionar Produto
                    Button(
                        onClick = { showProductDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IpcaGreen),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum produto adicionado.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f), // Ocupa o espaço disponível
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.cart) { item ->
                            CartItemRow(item = item, onRemove = { viewModel.removeFromCart(item) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. BOTÃO FINALIZAR (Entregar e Marcar Entregue) ---
                if (state.error != null) {
                    Text(state.error!!, color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        viewModel.submitUrgentDelivery {
                            navController.popBackStack() // Volta ao menu após sucesso
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IpcaRed),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isLoading && state.cart.isNotEmpty()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINALIZAR ENTREGA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA SELECIONAR PRODUTO ---
    if (showProductDialog) {
        ProductSelectionDialog(
            products = state.products,
            onDismiss = { showProductDialog = false },
            onConfirm = { product, qty ->
                viewModel.addToCart(product, qty)
                showProductDialog = false
            }
        )
    }
}

@Composable
fun CartItemRow(item: CartItem, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.product.name, fontWeight = FontWeight.Bold)
                Text("${item.quantity} unidades", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (Product, Int) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Produto") },
        text = {
            Column {
                // Lista Simplificada de Produtos (Dropdown seria ideal, mas Lista funciona bem aqui)
                Text("Selecione o produto:", fontSize = 12.sp, color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                ) {
                    LazyColumn {
                        items(products) { product ->
                            val isSelected = selectedProduct?.docId == product.docId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProduct = product }
                                    .background(if (isSelected) IpcaGreen.copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(product.name, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                                // Mostra total em stock
                                val totalStock = product.batches.sumOf { it.quantity }
                                Text("$totalStock em stock", fontSize = 10.sp, color = Color.Gray)
                            }
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quantidade:", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityText = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 0
                    if (selectedProduct != null && qty > 0) {
                        onConfirm(selectedProduct!!, qty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IpcaGreen),
                enabled = selectedProduct != null && (quantityText.toIntOrNull() ?: 0) > 0
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}