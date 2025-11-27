package ipca.project.lojasas.ui.benefeciary.newBasket

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.ui.colaborator.campaigns.CampaignDetailsViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// --- Cores ---
val CabazGreen = Color(0xFF438758)
val CabazLightBg = Color(0xFFF7F9F8)
val DisabledDayColor = Color(0xFFE0E0E0)
val SelectedDayBg = Color(0xFFDBECDF)
val TextDark = Color(0xFF3E3448)
val TextGray = Color(0xFF999999)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NewBasketView(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: NewBasketViewModel = viewModel()
) {
    val scrollState = rememberScrollState()

    val uiState = viewModel.uiState.value

    // --- ESTADO DE DATA E HORA ---
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var selectedTime by remember { mutableStateOf("09:00") }

    val dateForApi = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    // --- ESTADO LOCAL DAS QUANTIDADES ---
    val productQuantities = remember { mutableStateMapOf<String, Int>() }

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
                onClick = { navController.popBackStack() }, // Volta para o ecrã anterior
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = CabazGreen
                )
            }
        }

        Text(
            text = "Novo Cabaz",
            color = CabazGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp, top = 8.dp)
        )

        SectionHeader("Qual dia?", "Escolha um dia disponível para o levantamento do cabaz.")
        Spacer(modifier = Modifier.height(16.dp))

        // --- CALENDÁRIO COM NAVEGAÇÃO ---
        DynamicCalendarView(
            displayedYearMonth = displayedYearMonth,
            selectedDate = selectedDate,
            onMonthChange = { newMonth -> displayedYearMonth = newMonth },
            onDateSelected = { date -> selectedDate = date }
        )

        Spacer(modifier = Modifier.height(32.dp))

        SectionHeader("Qual hora?", "Escolha uma hora disponível para o levantamento do cabaz.")
        Spacer(modifier = Modifier.height(16.dp))

        TimeDropdownSelector(
            selectedTime = selectedTime,
            onTimeSelected = { time -> selectedTime = time }
        )

        // Debug visual (Podes remover depois)
        Text(
            text = "Agendamento: $dateForApi às $selectedTime",
            fontSize = 12.sp,
            color = CabazGreen,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        SectionHeader("Produtos", "Escolha os produtos disponíveis para o cabaz.")
        Spacer(modifier = Modifier.height(60.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(color = CabazGreen)
        } else if (!uiState.error.isNullOrEmpty()) {
            Text("Erro: ${uiState.error}", color = Color.Red)
        } else {
            Column {
                uiState.products.filter { it.batches.any { batch -> batch.quantity > 0 } }
                    .forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, CabazGreen, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                product.name,
                                color = TextDark,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val current = productQuantities[product.docId] ?: 0
                                        if (current > 0) productQuantities[product.docId] =
                                            current - 1
                                    }
                                ) {
                                    Text(
                                        "-",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CabazGreen
                                    )
                                }

                                Text(
                                    "${productQuantities[product.docId] ?: 0}",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val current = productQuantities[product.docId] ?: 0
                                        val stock = product.batches.sumOf { it.quantity }
                                        if (current < stock) productQuantities[product.docId] =
                                            current + 1
                                    }
                                ) {
                                    Text(
                                        "+",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CabazGreen
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = CabazGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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

    val monthName = displayedYearMonth.month
        .getDisplayName(TextStyle.FULL, Locale("pt", "PT"))
        .uppercase()
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
                    color = if (day == "Dom" || day == "Sáb") Color(0xFF5E4B68) else CabazGreen,
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
    val borderColor = if (isSelected) CabazGreen else Color.Transparent
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeDropdownSelector(selectedTime: String, onTimeSelected: (String) -> Unit) {
    val availableHours = remember {
        val slots = mutableListOf<String>()
        var t = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)
        while (!t.isAfter(end)) {
            slots.add(String.format("%02d:%02d", t.hour, t.minute))
            t = t.plusMinutes(15)
        }
        slots
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, CabazGreen, RoundedCornerShape(4.dp))
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .width(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = selectedTime, color = CabazGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CabazGreen)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White).heightIn(max = 250.dp)
        ) {
            availableHours.forEach { time ->
                DropdownMenuItem(
                    text = { Text(text = time, color = if(time == selectedTime) CabazGreen else TextDark, fontWeight = if(time == selectedTime) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onTimeSelected(time); expanded = false }
                )
            }
        }
    }
}

// Preview para testares visualmente (mock do NavController)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun NewBasketPreview() {
    NewBasketView(navController = rememberNavController())
}