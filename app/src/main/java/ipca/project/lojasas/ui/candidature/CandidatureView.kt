package ipca.project.lojasas.ui.candidature

import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.models.Type // Atualizado de Tipo para Type
import ipca.project.lojasas.ui.authentication.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ipca.project.lojasas.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatureView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: CandidatureViewModel = viewModel(),
    viewModelAuth: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        if (bytes.size > 800_000) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ficheiro muito grande! Máx: 800KB", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                            var realName = "attachment_${System.currentTimeMillis()}"
                            val cursor = context.contentResolver.query(it, null, null, null, null)
                            cursor?.use { c ->
                                if (c.moveToFirst()) {
                                    val indexNome = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (indexNome != -1) realName = c.getString(indexNome)
                                }
                            }
                            // addAnexo -> addAttachment
                            withContext(Dispatchers.Main) { viewModel.addAttachment(realName, base64String) }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao ler ficheiro", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->

        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = Color(0xFFF5F5F5)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Logo",
                    modifier = Modifier.height(60.dp).fillMaxWidth(),
                    alignment = Alignment.CenterStart
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "candidature",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )

                    Surface(
                        onClick = {
                            viewModelAuth.logout(onLogoutSuccess = {
                                navController.navigate("login") { popUpTo("login") { inclusive = true } }
                            })
                        },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.outline_user), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Text(" Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- MENSAGEM DE ERRO (FEEDBACK VISUAL) ---
                if (uiState.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                SectionHeader("Identificação")

                // ANO LETIVO -> ACADEMIC YEAR
                AppTextField(
                    value = uiState.candidature.academicYear,
                    onValueChange = { viewModel.updateAcademicYear(it) },
                    label = "Ano Letivo",
                    placeholder = "Ex: 2024/2025",
                    icon = ImageVector.vectorResource(id = R.drawable.student),
                    keyboardType = KeyboardType.Number,
                    visualTransformation = SchoolYearVisualTransformation()
                )

                // DATA NASCIMENTO -> BIRTH DATE
                AppTextField(
                    value = uiState.candidature.birthDate,
                    onValueChange = { viewModel.updateBirthDate(it) },
                    label = "Data de Nascimento",
                    placeholder = "DD/MM/AAAA",
                    icon = ImageVector.vectorResource(id = R.drawable.calendar_outline),
                    keyboardType = KeyboardType.Number,
                    visualTransformation = DateVisualTransformation()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // TELEMOVEL -> MOBILE PHONE
                    AppTextField(
                        value = uiState.candidature.mobilePhone,
                        onValueChange = { viewModel.updateMobilePhone(it) },
                        label = "Telemóvel",
                        placeholder = "999999999",
                        icon = Icons.Default.Phone,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Phone
                    )

                    AppTextField(
                        value = uiState.candidature.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = "Email",
                        placeholder = "email@...",
                        icon = Icons.Default.Email,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Email
                    )
                }

                SectionHeader("Dados Académicos / Profissionais")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expanded by remember { mutableStateOf(false) }

                    // DROPDOWN TIPO -> TYPE
                    Column(modifier = Modifier.weight(1f).padding(bottom = 20.dp)) {
                        Text(
                            text = "Tipo",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            fun formatEnum(t: Type): String {
                                return t.name.lowercase().replaceFirstChar { it.uppercase() }
                            }

                            OutlinedTextField(
                                value = uiState.candidature.type?.let { formatEnum(it) } ?: "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("Selecionar", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                Type.values().forEach { typeEnum ->
                                    DropdownMenuItem(
                                        text = { Text(formatEnum(typeEnum)) },
                                        onClick = {
                                            viewModel.updateType(typeEnum)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // NÚMERO CARTÃO -> CARD NUMBER
                    AppTextField(
                        value = uiState.candidature.cardNumber,
                        onValueChange = { viewModel.updateCardNumber(it) },
                        label = "N.º Cartão",
                        icon = ImageVector.vectorResource(id = R.drawable.numbercard),
                        placeholder = "00000",
                        modifier = Modifier.weight(1f)
                    )
                }

                // CURSO -> COURSE (SÓ APARECE SE NÃO FOR FUNCIONÁRIO)
                // Nota: Assumo que existe Type.EMPLOYEE ou similar no teu Enum em inglês
                if (uiState.candidature.type != Type.FUNCIONARIO) {
                    AppTextField(
                        value = uiState.candidature.course ?: "",
                        onValueChange = { viewModel.updateCourse(it) },
                        label = "Curso",
                        placeholder = "Ex: Engenharia de Sistemas...",
                        icon = Icons.Default.Info
                    )
                }

                SectionHeader("Produtos Solicitados")

                AppCard {
                    // Mapeamento dos produtos em Inglês
                    val produtos = listOf(
                        "Produtos alimentares" to uiState.candidature.foodProducts,
                        "Produtos de higiene pessoal" to uiState.candidature.hygieneProducts,
                        "Produtos de limpeza" to uiState.candidature.cleaningProducts
                    )

                    produtos.forEach { (label, isChecked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    when (label) {
                                        "Produtos alimentares" -> viewModel.updateFoodProducts(checked)
                                        "Produtos de higiene pessoal" -> viewModel.updateHygieneProducts(checked)
                                        "Produtos de limpeza" -> viewModel.updateCleaningProducts(checked)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                SectionHeader("Outros Apoios")

                AppCard {
                    Text("Apoio FAES?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row {
                        // FAES SUPPORT
                        RadioButton(selected = uiState.candidature.faesSupport == true, onClick = { viewModel.updateFaesSupport(true) })
                        Text("Sim", Modifier.align(Alignment.CenterVertically))
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = uiState.candidature.faesSupport == false, onClick = { viewModel.updateFaesSupport(false) })
                        Text("Não", Modifier.align(Alignment.CenterVertically))
                    }
                }
                Spacer(Modifier.height(8.dp))

                AppCard {
                    Text("Bolsa de Estudo?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row {
                        // SCHOLARSHIP SUPPORT
                        RadioButton(selected = uiState.candidature.scholarshipSupport == true, onClick = { viewModel.updateScholarshipSupport(true) })
                        Text("Sim", Modifier.align(Alignment.CenterVertically))
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = uiState.candidature.scholarshipSupport == false, onClick = { viewModel.updateScholarshipSupport(false) })
                        Text("Não", Modifier.align(Alignment.CenterVertically))
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (uiState.candidature.scholarshipSupport == true) {
                    // SCHOLARSHIP DETAILS
                    AppTextField(
                        value = uiState.candidature.scholarshipDetails,
                        onValueChange = { viewModel.updateScholarshipDetails(it) },
                        label = "Entidade e Valor",
                        placeholder = "Ex: DGES - 1000€"
                    )
                }

                SectionHeader("Documentação Necessária")

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nota: Beneficiários FAES não necessitam entregar os documentos abaixo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (uiState.candidature.faesSupport != true) {
                    AppCard {
                        Text("Obrigatório anexar:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        DocumentoRequisitoItem("Despesas fixas (Habitação, Saúde, Transportes)")
                        DocumentoRequisitoItem("3 últimos recibos de vencimento")
                        DocumentoRequisitoItem("Extratos bancários (3 meses)")
                        DocumentoRequisitoItem("Internacionais: Título residência + Comprovativos")
                    }

                    Spacer(Modifier.height(8.dp))

                    AppCard {
                        // ANEXOS -> ATTACHMENTS
                        if (uiState.candidature.attachments.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.candidature.attachments.forEachIndexed { index, attachment ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp)
                                    ) {
                                        Icon( ImageVector.vectorResource(id = R.drawable.file_dock), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(attachment.name, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { viewModel.removeAttachment(index) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { launcher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
                        ) {
                            Icon(ImageVector.vectorResource(id = R.drawable.file_dock), null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar Ficheiro")
                        }
                        Text("Máx total: 1MB", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally))
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionHeader("Declarações")

                Row(verticalAlignment = Alignment.Top) {
                    // TRUTHFULNESS DECLARATION
                    Checkbox(checked = uiState.candidature.truthfulnessDeclaration, onCheckedChange = { viewModel.updateTruthfulnessDeclaration(it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                    Text("Declaro, sob compromisso de honra, a veracidade das informações.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
                Row(verticalAlignment = Alignment.Top) {
                    // DATA AUTHORIZATION
                    Checkbox(checked = uiState.candidature.dataAuthorization, onCheckedChange = { viewModel.updateDataAuthorization(it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                    Text("Autorizo o tratamento de dados (RGPD).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }

                Spacer(Modifier.height(20.dp))
                SectionHeader("Finalização")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // SIGNATURE DATE
                    AppTextField(
                        value = uiState.candidature.signatureDate,
                        onValueChange = { viewModel.updateSignatureDate(it) },
                        label = "Data",
                        icon = ImageVector.vectorResource(id = R.drawable.calendar_outline),
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        placeholder = "DD/MM/AAAA",
                        visualTransformation = DateVisualTransformation()
                    )
                    // SIGNATURE
                    AppTextField(
                        value = uiState.candidature.signature,
                        onValueChange = { viewModel.updateSignature(it) },
                        label = "Assinatura",
                        icon = Icons.Default.Edit,
                        placeholder = "Escreve o teu nome",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(30.dp))

                Button(
                    onClick = {
                        viewModel.submitCandidature { success ->
                            if (success) {
                                Toast.makeText(context, "candidature enviada!", Toast.LENGTH_LONG).show()
                                navController.navigate("await-candidature") {
                                    popUpTo("candidature") { inclusive = true }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    enabled = !uiState.isLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Submeter candidature", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            leadingIcon = icon?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation
        )
    }
}

@Composable
fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun DocumentoRequisitoItem(texto: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

class SchoolYearVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 3) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 8) return offset + 1
                return 9
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}