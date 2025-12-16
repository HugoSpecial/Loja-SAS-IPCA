package ipca.project.lojasas.ui.beneficiary.newBasket

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Product
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

// --- Cores Personalizadas ---
val CabazLightBg = Color(0xFFF7F9F8)
val DisabledDayColor = Color(0xFFE0E0E0)
val SelectedDayBg = Color(0xFFDBECDF)
val TextDark = Color(0xFF3E3448)
val TextGray = Color(0xFF999999)

// Cores dos Botões
val ButtonGreen = Color(0xFF438E63)   // Verde (Ação normal)
val ButtonGray = Color(0xFFAAB0AD)    // Cinzento (Desativado/Stock Máximo)
val ButtonRemoveRed = Color(0xFFE57373) // Vermelho Suave (Remover/Lixo)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NewBasketView(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: NewBasketViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val uiState = viewModel.uiState.value

    // --- Recupera produtos selecionados da HomeView ---
    val savedProducts = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<List<String>>("selectedProducts") ?: emptyList()

    val selectedProductList = uiState.products.filter { savedProducts.contains(it.docId) }

    // --- ESTADO DE DATA ---
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    // --- ESTADO LOCAL DAS QUANTIDADES ---
    val productQuantities = remember { mutableStateMapOf<String, Int>() }

    // Inicializa quantidades a 1 quando entra no ecrã
    LaunchedEffect(uiState.products, savedProducts) {
        uiState.products.forEach { product ->
            if (savedProducts.contains(product.docId) && productQuantities[product.docId] == null) {
                productQuantities[product.docId] = 1
            }
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CabazLightBg)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // --- BOTÃO DE VOLTAR ---
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Novo Cabaz",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp, top = 8.dp)
        )

        SectionHeader("Qual dia?", "Escolha um dia disponível para o levantamento do cabaz.")
        Spacer(modifier = Modifier.height(16.dp))

        // --- CALENDÁRIO ---
        DynamicCalendarView(
            displayedYearMonth = displayedYearMonth,
            selectedDate = selectedDate,
            onMonthChange = { newMonth -> displayedYearMonth = newMonth },
            onDateSelected = { date -> selectedDate = date }
        )

        Spacer(modifier = Modifier.height(40.dp))

        SectionHeader("Produtos", "Escolha a Quantidade de cada produto que deseja para o cabaz.")
        Spacer(modifier = Modifier.height(16.dp))

        if (!uiState.error.isNullOrEmpty()) {
            Text("Erro: ${uiState.error}", color = Color.Red)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Verifica se há produtos com quantidade > 0 para mostrar
                val visibleProducts = selectedProductList.filter { (productQuantities[it.docId] ?: 0) > 0 }

                if (visibleProducts.isEmpty()) {
                    Text(
                        "O cabaz está vazio.",
                        color = TextGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    visibleProducts.forEach { product ->
                        val quantity = productQuantities[product.docId] ?: 0
                        val stock = product.batches.sumOf { it.quantity }

                        ProductCardItem(
                            product = product,
                            quantity = quantity,
                            stock = stock,
                            onAdd = {
                                if (quantity < stock) productQuantities[product.docId] = quantity + 1
                            },
                            onRemove = {
                                // Se chegar a 0, o filtro `visibleProducts` vai escondê-lo automaticamente
                                if (quantity > 0) productQuantities[product.docId] = quantity - 1
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão Enviar só aparece se houver produtos
                if (visibleProducts.isNotEmpty()) {
                    Button(
                        onClick = {
                            val dateConverted = Date.from(
                                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            )
                            val finalProducts = productQuantities.filter { it.value > 0 }

                            viewModel.createOrder(dateConverted, finalProducts)
                            { success ->
                                if (success) {
                                    Toast.makeText(context, "Pedido Enviado!", Toast.LENGTH_LONG).show()
                                    navController.navigate("home") {
                                        popUpTo("solicitation") { inclusive = true }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Pedir Cabaz",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pedir Cabaz",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// --- CARD DO PRODUTO (Estilo Card + Imagem Base64 + Botões Inteligentes) ---
@Composable
fun ProductCardItem(
    product: Product,
    quantity: Int,
    stock: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    // --- LÓGICA DE IMAGEM ---
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(product.imageUrl) {
        if (product.imageUrl.isNotEmpty()) {
            try {
                // Remove cabeçalho se existir e descodifica
                val cleanBase64 = if (product.imageUrl.contains(","))
                    product.imageUrl.substringAfter(",")
                else product.imageUrl
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imageBitmap = bitmap?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Esquerdo: Imagem + Texto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Caixa da Imagem
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.1f)),
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
                        // Fallback placeholder
                        Text("Img", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = product.name,
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    // (Opcional) Mostra stock restante se quiseres
                    Text("Stock: $stock", fontSize = 10.sp, color = TextGray)
                }
            }

            // Lado Direito: Botões
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
// --- BOTÃO REMOVER / LIXO ---
                val isLastItem = quantity == 1

                QuantityButton(
                    icon = if (isLastItem) {
                        Icons.Default.Delete
                    } else {
                        ImageVector.vectorResource(id = R.drawable.sub_round)
                    },
                    backgroundColor = if (isLastItem) ButtonRemoveRed else ButtonGreen,
                    onClick = onRemove
                )

                Text(
                    text = "$quantity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // --- BOTÃO ADICIONAR ---
                val isMaxStock = quantity >= stock

                QuantityButton(
                    icon = Icons.Default.Add,
                    // Se estiver no máximo, fica cinzento
                    backgroundColor = if (isMaxStock) ButtonGray else ButtonGreen,
                    onClick = {
                        if (!isMaxStock) onAdd()
                    }
                )
            }
        }
    }
}

// Componente Auxiliar para o botão quadrado
@Composable
fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- COMPONENTES DO CALENDÁRIO & HEADER (Mantidos Iguais) ---

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            color = TextDark.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DynamicCalendarView(
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val daysInMonth = displayedYearMonth.lengthOfMonth()
    val firstDayOfWeekValue = displayedYearMonth.atDay(1).dayOfWeek.value
    val startOffset = firstDayOfWeekValue % 7
    val monthName = displayedYearMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "PT")).uppercase()
    val year = displayedYearMonth.year

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val canGoBack = displayedYearMonth.isAfter(currentMonth)
            IconButton(
                onClick = { if (canGoBack) onMonthChange(displayedYearMonth.minusMonths(1)) },
                enabled = canGoBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mês Anterior",
                    tint = if (canGoBack) TextDark else Color.Transparent
                )
            }

            Text(
                text = "$monthName $year",
                color = TextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = { onMonthChange(displayedYearMonth.plusMonths(1)) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Próximo Mês",
                    tint = TextDark
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = if (day == "Dom" || day == "Sáb") Color(0xFF5E4B68) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val totalSlots = startOffset + daysInMonth
        val rows = (totalSlots / 7) + if (totalSlots % 7 != 0) 1 else 0

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (c in 0..6) {
                    val dayNumber = (r * 7 + c) - startOffset + 1
                    val isValidDay = dayNumber in 1..daysInMonth
                    Box(modifier = Modifier.width(40.dp).height(40.dp), contentAlignment = Alignment.Center) {
                        if (isValidDay) {
                            val cellDate = displayedYearMonth.atDay(dayNumber)
                            val isWeekend = c == 0 || c == 6
                            val isPastOrToday = !cellDate.isAfter(today)
                            val isDisabled = isWeekend || isPastOrToday
                            DayCell(
                                day = dayNumber,
                                isSelected = selectedDate.isEqual(cellDate),
                                isDisabled = isDisabled,
                                onClick = { if (!isDisabled) onDateSelected(cellDate) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: Int, isSelected: Boolean, isDisabled: Boolean, onClick: () -> Unit) {
    val backgroundColor = when {
        isSelected -> SelectedDayBg
        isDisabled -> DisabledDayColor
        else -> Color.Transparent
    }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isDisabled) TextGray else TextDark
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = !isDisabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.toString(), color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}