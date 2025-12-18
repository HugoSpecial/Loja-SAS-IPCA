package ipca.project.lojasas.ui.collaborator.donation

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Campaign
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationView(
    navController: NavController,
    productId: String? = null,
    viewModel: DonationViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) {
            viewModel.loadProduct(productId)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            viewModel.onDateSelected(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var campaignExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) } // Estado para o menu da categoria

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // --- CABEÇALHO ---
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
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

        // --- CONTEÚDO SCROLLABLE ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Registo de doações",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Aqui pode registar uma nova doação.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF8C8C8C),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. DADOS GERAIS DO DOADOR ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Dados Gerais",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = campaignExpanded,
                        onExpandedChange = { campaignExpanded = !campaignExpanded }) {
                        OutlinedTextField(
                            value = state.selectedCampaign?.name ?: "Sem Campanha",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Campanha (Opcional)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = campaignExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = campaignExpanded,
                            onDismissRequest = { campaignExpanded = false }) {
                            DropdownMenuItem(text = { Text("Sem Campanha") }, onClick = {
                                viewModel.onCampaignSelected(Campaign(name = ""))
                                campaignExpanded = false
                            })

                            state.activeCampaigns.forEach { campaign ->
                                DropdownMenuItem(text = { Text(campaign.name ?: "") }, onClick = {
                                    viewModel.onCampaignSelected(campaign)
                                    campaignExpanded = false
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = state.isAnonymous,
                            onCheckedChange = { viewModel.onAnonymousChange(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Doador Anónimo")
                    }

                    if (!state.isAnonymous) {
                        OutlinedTextField(
                            value = state.donorName,
                            onValueChange = { viewModel.onDonorNameChange(it) },
                            label = { Text("Nome do Doador") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. ADICIONAR PRODUTO ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Adicionar Produto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.currentImageBase64 != null) {
                        val bitmap = remember(state.currentImageBase64) {
                            try {
                                val decoded = Base64.decode(state.currentImageBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                            } catch (e: Exception) { null }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NOME DO PRODUTO (AUTOCOMPLETE)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.currentName,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("Nome do Produto") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.filteredProducts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp)
                            .heightIn(max = 150.dp)
                            .zIndex(2f),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn {
                            items(state.filteredProducts) { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = { viewModel.onProductSelected(product) })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- SELETOR DE CATEGORIA (NOVO) ---
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = state.currentCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    placeholder = { Text("Selecione...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QUANTIDADE E DATA
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.currentQuantity,
                    onValueChange = { viewModel.onQuantityChange(it) },
                    label = { Text("Qtd") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                val dateText = state.currentValidity?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: ""

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Validade") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { datePickerDialog.show() },
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÃO ADICIONAR À LISTA
            Button(
                onClick = { viewModel.addProductToList() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar à Lista")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. LISTA DE PRODUTOS ADICIONADOS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Produtos na Doação (${state.productsToAdd.size})",
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.productsToAdd.forEachIndexed { index, product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                // Mostra a categoria no cartão
                                Text(product.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)

                                val batch = product.batches.firstOrNull()
                                val dateStr = batch?.validity?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                } ?: "Sem data"
                                Text(
                                    "Qtd: ${batch?.quantity} | Val: $dateStr",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.removeProductFromList(index) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // BOTÃO FINALIZAR
            Button(
                onClick = {
                    viewModel.saveDonation { navController.popBackStack() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isLoading && state.productsToAdd.isNotEmpty()
            ) {
                Text("FINALIZAR DOAÇÃO", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}