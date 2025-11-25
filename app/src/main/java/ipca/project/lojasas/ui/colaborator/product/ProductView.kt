package ipca.project.lojasas.ui.colaborator.product

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductView(
    navController: NavController,
    productId: String? = null, // <--- Recebe o ID (opcional)
    viewModel: ProductViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- EFEITO: CARREGAR DADOS SE TIVER ID ---
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

    Scaffold(
        topBar = {
            TopAppBar(
                // Muda o título consoante estamos a editar ou a criar
                title = { Text(if (productId.isNullOrEmpty()) "Novo Produto" else "Editar Produto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // IMAGEM
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.imageBase64 != null && state.imageBase64!!.isNotEmpty()) {
                            val bitmap = remember(state.imageBase64) {
                                try {
                                    val decodedString = Base64.decode(state.imageBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)?.asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Produto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.White, RoundedCornerShape(50))
                                        .padding(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                                Text("Adicionar Foto", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // NOME
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Nome do Produto") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QUANTIDADE
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = { viewModel.onQuantityChange(it) },
                        // Muda o texto de ajuda se for edição
                        label = { Text(if (productId.isNullOrEmpty()) "Quantidade (un)" else "Adicionar Stock (un)") },
                        placeholder = { Text(if (productId.isNullOrEmpty()) "0" else "Deixe 0 para manter") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (!productId.isNullOrEmpty()) {
                        Text("Deixe a quantidade em branco ou 0 se quiser apenas editar o nome/foto.", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DATA
                    val dateText = state.validity?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { },
                        label = { Text("Validade (Opcional)") },
                        placeholder = { Text("Selecione a data") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        enabled = false,
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveProduct {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isLoading
                    ) {
                        Text(
                            text = if (productId.isNullOrEmpty()) "Criar Produto" else "Guardar Alterações",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}