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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.ui.beneficiary.CartManager
import ipca.project.lojasas.ui.components.EmptyState
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NewBasketView(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: NewBasketViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val uiState = viewModel.uiState.value
    val context = LocalContext.current

    val cartItems = CartManager.cartItems
    val productsToShow = uiState.products.filter { cartItems.containsKey(it.docId) }

    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fundo adaptável
    ) {
        // --- CABEÇALHO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
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
                    tint = MaterialTheme.colorScheme.primary, // GreenPrimary
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

        // --- CONTEÚDO ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Novo Cabaz",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp, top = 8.dp)
            )

            SectionHeader("Qual dia?", "Escolha um dia disponível para levantamento.")
            Spacer(modifier = Modifier.height(16.dp))

            DynamicCalendarView(
                displayedYearMonth,
                selectedDate,
                { displayedYearMonth = it },
                { selectedDate = it }
            )
            Spacer(modifier = Modifier.height(40.dp))

            SectionHeader("Produtos", "Ajuste as quantidades.")
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (productsToShow.isEmpty()) {
                        Box(modifier = Modifier.height(150.dp)) {
                            EmptyState(
                                message = "O cabaz está vazio. Volte à Home para adicionar produtos.",
                                icon = Icons.Outlined.ShoppingCart
                            )
                        }
                    } else {
                        productsToShow.forEach { product ->
                            val quantity = cartItems[product.docId] ?: 1
                            val stock = product.batches.sumOf { it.quantity }
                            ProductCardItem(
                                product = product,
                                quantity = quantity,
                                stock = stock,
                                onAdd = { if (quantity < stock) CartManager.addQuantity(product.docId) },
                                onRemove = { CartManager.removeQuantity(product.docId) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (productsToShow.isNotEmpty()) {
                    Button(
                        onClick = {
                            val dateConverted = Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                            viewModel.createOrder(dateConverted, cartItems) { success ->
                                if (success) {
                                    Toast.makeText(context, "Pedido Enviado!", Toast.LENGTH_LONG).show()
                                    CartManager.clear()
                                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Pedir Cabaz", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- Componentes Auxiliares ---

@Composable
fun ProductCardItem(product: Product, quantity: Int, stock: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
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

    // Card agora usa 'surface' (Branco no Light, Cinza Escuro no Dark)
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)), // Placeholder adaptável
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
                        Text("Img", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = product.name,
                        color = MaterialTheme.colorScheme.onSurface, // Preto ou Branco
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Stock: $stock",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(
                    icon = if (quantity == 1) Icons.Default.Delete else ImageVector.vectorResource(id = R.drawable.sub_round),
                    // Vermelho se for para remover, Verde se for subtrair
                    backgroundColor = if (quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    onClick = onRemove
                )
                Text(
                    text = "$quantity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                QuantityButton(
                    icon = Icons.Default.Add,
                    // Cinza se stock maximo, Verde se normal
                    backgroundColor = if (quantity >= stock) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    onClick = onAdd
                )
            }
        }
    }
}

@Composable
fun QuantityButton(icon: ImageVector, backgroundColor: Color, onClick: () -> Unit) {
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
            tint = Color.White, // Ícone sempre branco
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DynamicCalendarView(displayedYearMonth: YearMonth, selectedDate: LocalDate, onMonthChange: (YearMonth) -> Unit, onDateSelected: (LocalDate) -> Unit) {
    val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    val today = LocalDate.now()
    val monthName = displayedYearMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "PT")).uppercase()
    val year = displayedYearMonth.year
    val daysInMonth = displayedYearMonth.lengthOfMonth()
    val startOffset = displayedYearMonth.atDay(1).dayOfWeek.value % 7

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Navegação Mês
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val canGoBack = displayedYearMonth.isAfter(YearMonth.now())
            IconButton(
                onClick = { if (canGoBack) onMonthChange(displayedYearMonth.minusMonths(1)) },
                enabled = canGoBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    null,
                    tint = if (canGoBack) MaterialTheme.colorScheme.onBackground else Color.Transparent
                )
            }
            Text(
                "$monthName $year",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { onMonthChange(displayedYearMonth.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Cabeçalho Semanal
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { dayName ->
                // Fins de semana com cor diferente ou padrão
                val color = if (dayName == "Dom" || dayName == "Sáb")
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.primary

                Text(
                    dayName,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Grelha de Dias
        val totalSlots = startOffset + daysInMonth
        val rows = (totalSlots / 7) + if (totalSlots % 7 != 0) 1 else 0
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (c in 0..6) {
                    val dayNum = (r * 7 + c) - startOffset + 1
                    val isValid = dayNum in 1..daysInMonth
                    Box(modifier = Modifier.width(40.dp).height(40.dp), contentAlignment = Alignment.Center) {
                        if (isValid) {
                            val date = displayedYearMonth.atDay(dayNum)
                            val disabled = (c == 0 || c == 6) || !date.isAfter(today)
                            DayCell(
                                day = dayNum,
                                selected = selectedDate.isEqual(date),
                                disabled = disabled
                            ) { if (!disabled) onDateSelected(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: Int, selected: Boolean, disabled: Boolean, onClick: () -> Unit) {
    // Lógica de cores do Calendário
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // Verde suave
        disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) // Cinza muito claro
        else -> Color.Transparent
    }

    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    val text = if (disabled)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .clickable(enabled = !disabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("$day", color = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}