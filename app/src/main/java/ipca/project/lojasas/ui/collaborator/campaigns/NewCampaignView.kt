package ipca.project.lojasas.ui.collaborator.campaigns

import android.widget.DatePicker
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.models.CampaignType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCampaignView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: NewCampaignViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Definimos as cores padrão reutilizáveis baseadas no teu pedido
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = MaterialTheme.colorScheme.primary,
        // Cores para quando o campo de data está "bloqueado" para escrita (mas clicável)
        disabledContainerColor = Color.White,
        disabledBorderColor = MaterialTheme.colorScheme.primary,
        disabledTextColor = Color.Black,
        disabledTrailingIconColor = MaterialTheme.colorScheme.primary
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Campanha") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp) // Aumentei um pouco o padding lateral para ficar igual ao login
                .verticalScroll(scrollState)
        ) {

            // --- NOME ---
            Text(
                "Nome da Campanha",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                placeholder = { Text("Ex: Saldos de Verão", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit, // Ícone de edição
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = customTextFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- DATA INÍCIO ---
            Text(
                "Data de Início",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )

            // Usamos Box para garantir que o clique funciona sobre o campo desativado
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateFormat.format(uiState.startDate),
                    onValueChange = {},
                    placeholder = { Text("Selecione a data", color = Color.Gray) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // Desativado para não abrir teclado
                    shape = RoundedCornerShape(12.dp),
                    colors = customTextFieldColors, // Usa as mesmas cores (incluindo disabled)
                    singleLine = true
                )
                // Overlay invisível para capturar o clique
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            showDatePicker(context, uiState.startDate) { date ->
                                viewModel.onStartDateChange(date)
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- DATA FIM ---
            Text(
                "Data de Fim",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateFormat.format(uiState.endDate),
                    onValueChange = {},
                    placeholder = { Text("Selecione a data", color = Color.Gray) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = customTextFieldColors,
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            showDatePicker(context, uiState.endDate) { date ->
                                viewModel.onEndDateChange(date)
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TIPO DE CAMPANHA ---
            Text(
                "Tipo de Campanha",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = uiState.campaignType == CampaignType.INTERNO,
                    onClick = { viewModel.onTypeChange(CampaignType.INTERNO) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Interno",
                    modifier = Modifier.clickable { viewModel.onTypeChange(CampaignType.INTERNO) },
                    color = Color.Black
                )

                Spacer(modifier = Modifier.width(24.dp))

                RadioButton(
                    selected = uiState.campaignType == CampaignType.EXTERNO,
                    onClick = { viewModel.onTypeChange(CampaignType.EXTERNO) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Externo",
                    modifier = Modifier.clickable { viewModel.onTypeChange(CampaignType.EXTERNO) },
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- MENSAGEM DE ERRO ---
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- BOTÃO CRIAR ---
            Button(
                onClick = {
                    viewModel.addCampaign {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Altura igual ao do Login
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Criar Campanha", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Função auxiliar para abrir o Calendário nativo do Android
fun showDatePicker(context: android.content.Context, initialDate: Date, onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.time = initialDate

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    // Aqui usamos explicitamente o android.app.DatePickerDialog para não confundir com o Compose
    android.app.DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(newCalendar.time)
        },
        year,
        month,
        day
    ).show()
}