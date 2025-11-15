package ipca.project.lojasas.ui.candidature

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.TipoCurso
import ipca.project.lojasas.ui.authentication.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatureView(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: CandidatureViewModel = viewModel(),
    viewModelAuth: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val scrollState = rememberScrollState()

    // Snackbar para mostrar erros
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logout Button - Agora dentro do Column que faz scroll, no topo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        onClick = {
                            viewModelAuth.logout(
                                onLogoutSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Conteúdo do formulário - Começa logo após o logout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = ipca.project.lojasas.R.drawable.logo_sas),
                        contentDescription = "Logo IPCA SAS - Loja Social",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Título
                    Text(
                        text = "Candidatura - Loja Social",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Seção: Identificação do candidato
                    SectionTitle("Identificação")

                    OutlinedTextField(
                        value = uiState.candidatura.anoLetivo,
                        onValueChange = { viewModel.updateAnoLetivo(it) },
                        label = { Text("Ano Letivo") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Ano Letivo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        placeholder = { Text("Ex: 2024/2025") }
                    )

                    OutlinedTextField(
                        value = uiState.candidatura.dataNascimento,
                        onValueChange = { viewModel.updateDataNascimento(it) },
                        label = { Text("Data de Nascimento") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Data Nascimento",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        placeholder = { Text("DD/MM/AAAA") }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.candidatura.telemovel,
                            onValueChange = { viewModel.updateTelemovel(it) },
                            label = { Text("Telemóvel") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Telemóvel",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            placeholder = { Text("912345678") }
                        )

                        OutlinedTextField(
                            value = uiState.candidatura.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            placeholder = { Text("exemplo@email.com") }
                        )
                    }

                    // Seção: Dados académicos
                    SectionTitle("Dados Académicos")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val opcoesAcademicas = listOf("Licenciatura", "Mestrado", "CTeSP")
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = uiState.candidatura.tipoCurso?.let { tipoCurso ->
                                    when (tipoCurso) {
                                        TipoCurso.LICENCIATURA -> "Licenciatura"
                                        TipoCurso.MESTRADO -> "Mestrado"
                                        TipoCurso.CTESP -> "CTeSP"
                                        null -> ""
                                    }
                                } ?: "",
                                onValueChange = {},
                                label = { Text("Tipo de Curso") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Tipo Curso",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                readOnly = true,
                                placeholder = { Text("Selecionar...") }
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                opcoesAcademicas.forEach { opcao ->
                                    DropdownMenuItem(
                                        text = { Text(opcao) },
                                        onClick = {
                                            viewModel.updateTipoCurso(opcao)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.candidatura.numeroEstudante,
                            onValueChange = { viewModel.updateNumeroEstudante(it) },
                            label = { Text("N.º estudante") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Número Estudante",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            placeholder = { Text("Ex: 12345") }
                        )
                    }

                    OutlinedTextField(
                        value = uiState.candidatura.curso,
                        onValueChange = { viewModel.updateCurso(it) },
                        label = { Text("Curso") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Curso",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        placeholder = { Text("Nome do curso") }
                    )

                    // Seção: Tipologia do pedido
                    SectionTitle("Produtos Solicitados")

                    Column {
                        val produtos = listOf(
                            "Produtos alimentares" to uiState.candidatura.produtosAlimentares,
                            "Produtos de higiene pessoal" to uiState.candidatura.produtosHigiene,
                            "Produtos de limpeza" to uiState.candidatura.produtosLimpeza
                        )

                        produtos.forEach { (produto, isChecked) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        when (produto) {
                                            "Produtos alimentares" ->
                                                viewModel.updateProdutosAlimentares(checked)
                                            "Produtos de higiene pessoal" ->
                                                viewModel.updateProdutosHigiene(checked)
                                            "Produtos de limpeza" ->
                                                viewModel.updateProdutosLimpeza(checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = produto,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    // Seção: Outros apoios
                    SectionTitle("Outros Apoios")

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // FAES
                        Column {
                            Text(
                                text = "É apoiado(a) pelo Fundo de Apoio de Emergência Social (FAES)?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = uiState.candidatura.faesApoiado == true,
                                        onClick = { viewModel.updateFaesApoiado(true) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text("Sim", modifier = Modifier.padding(start = 4.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = uiState.candidatura.faesApoiado == false,
                                        onClick = { viewModel.updateFaesApoiado(false) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text("Não", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }

                        // Bolsa de estudo
                        Column {
                            Text(
                                text = "É beneficiário de alguma bolsa de estudo ou apoio?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = uiState.candidatura.bolsaApoio == true,
                                        onClick = { viewModel.updateBolsaApoio(true) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text("Sim", modifier = Modifier.padding(start = 4.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = uiState.candidatura.bolsaApoio == false,
                                        onClick = { viewModel.updateBolsaApoio(false) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text("Não", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }

                        if (uiState.candidatura.bolsaApoio == true) {
                            OutlinedTextField(
                                value = uiState.candidatura.detalhesBolsa,
                                onValueChange = { viewModel.updateDetalhesBolsa(it) },
                                label = { Text("Entidade e valor") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Bolsa",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                placeholder = { Text("Ex: DGES - 1500€") }
                            )
                        }
                    }

                    // Declarações
                    SectionTitle("Declarações")

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = uiState.candidatura.declaracaoVeracidade,
                                onCheckedChange = {
                                    viewModel.updateDeclaracaoVeracidade(it)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Declaro, sob compromisso de honra, a veracidade de todas as informações prestadas.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = uiState.candidatura.autorizacaoDados,
                                onCheckedChange = {
                                    viewModel.updateAutorizacaoDados(it)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Autorizo o tratamento dos meus dados pessoais em cumprimento do RGPD.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // Data e assinatura
                    SectionTitle("Finalização")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.candidatura.dataAssinatura,
                            onValueChange = { viewModel.updateDataAssinatura(it) },
                            label = { Text("Data") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Data",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            placeholder = { Text("DD/MM/AAAA") }
                        )

                        OutlinedTextField(
                            value = uiState.candidatura.assinatura,
                            onValueChange = { viewModel.updateAssinatura(it) },
                            label = { Text("Assinatura") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Assinatura",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            placeholder = { Text("Sua assinatura") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão de submissão
                    Button(
                        onClick = {
                            viewModel.submitCandidatura { success ->
                                if (success) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading && isFormValid(uiState.candidatura),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Submeter Candidatura",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun isFormValid(candidatura: Candidatura): Boolean {
    return candidatura.anoLetivo.isNotBlank() &&
            candidatura.dataNascimento.isNotBlank() &&
            candidatura.telemovel.isNotBlank() &&
            candidatura.email.isNotBlank() &&
            candidatura.tipoCurso != null &&
            candidatura.curso.isNotBlank() &&
            candidatura.numeroEstudante.isNotBlank() &&
            (candidatura.produtosAlimentares || candidatura.produtosHigiene || candidatura.produtosLimpeza) &&
            candidatura.faesApoiado != null &&
            candidatura.bolsaApoio != null &&
            candidatura.declaracaoVeracidade &&
            candidatura.autorizacaoDados &&
            candidatura.dataAssinatura.isNotBlank() &&
            candidatura.assinatura.isNotBlank()
}