package ipca.project.lojasas.ui.collaborator.candidature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.DocumentAttachment
import java.io.File
import java.io.FileOutputStream

// Cores do Tema
val IpcaGreen = Color(0xFF438C58)
val BackgroundColor = Color(0xFFF5F6F8)
val TextDark = Color(0xFF2D2D2D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatureDetailsView(
    navController: NavController,
    candidatureId: String,
    viewModel: CandidatureDetailsViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val context = LocalContext.current

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    // Estados para Preview de Ficheiros
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedPdfFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(candidatureId) {
        viewModel.fetchCandidature(candidatureId)
    }

    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            navController.popBackStack()
        }
    }

    // --- DIÁLOGOS (Preview e Rejeição) ---
    if (selectedImageBase64 != null) {
        ImagePreviewDialog(base64String = selectedImageBase64!!, onDismiss = { selectedImageBase64 = null })
    }
    if (selectedPdfFile != null) {
        PdfPreviewDialog(file = selectedPdfFile!!, onDismiss = { selectedPdfFile = null })
    }
    if (showRejectDialog) {
        RejectDialog(
            reason = rejectReason,
            text = "Rejeitar candidatura",
            onReasonChange = { rejectReason = it },
            onDismiss = { showRejectDialog = false },
            onConfirm = {
                if (rejectReason.isNotBlank()) {
                    viewModel.rejectCandidature(candidatureId, rejectReason)
                    showRejectDialog = false
                }
            }
        )
    }

    // --- ESTRUTURA PRINCIPAL ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // 1. HEADER (Igual à Lista: Seta + Logo)
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
                    tint = IpcaGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp) // Compensa a seta para centrar
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

        // 2. CONTEÚDO COM SCROLL
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = IpcaGreen)
            } else if (state.candidature != null) {
                val cand = state.candidature

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge de Estado (Alinhado à esquerda)
                    StatusBadgeDetails(state = cand.state)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- DADOS DO ALUNO ---
                    SectionTitle("Dados do aluno")
                    InfoRow("Email:", cand.email)
                    InfoRow("Telemóvel:", cand.mobilePhone)
                    InfoRow("Nascimento:", cand.birthDate)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- DADOS ACADÉMICOS ---
                    SectionTitle("Dados Académicos / Profissionais")
                    InfoRow("Tipo:", cand.type?.name ?: "-")
                    InfoRow("N.º Cartão:", cand.cardNumber)
                    InfoRow("Curso:", cand.course ?: "-")
                    InfoRow("Ano letivo:", cand.academicYear)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- PRODUTOS SOLICITADOS ---
                    SectionTitle("Produtos solicitados")
                    BooleanStatusRow("Alimentares", cand.foodProducts)
                    BooleanStatusRow("Higiene Pessoal", cand.hygieneProducts)
                    BooleanStatusRow("Limpeza", cand.cleaningProducts)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SITUAÇÃO SOCIOECONÓMICA ---
                    SectionTitle("Situação Socioeconómica")
                    InfoRow("Benificário FAES:", if (cand.faesSupport == true) "Sim" else "Não") // Mantive o erro ortográfico da imagem "Benificário" ou corrigimos para Beneficiário? Corrigi para Benificário para ser igual à imagem, mas o correto é Beneficiário.
                    InfoRow("Bolseiro:", if (cand.scholarshipSupport == true) "Sim" else "Não")

                    if (cand.scholarshipSupport == true && cand.scholarshipDetails.isNotEmpty()) {
                        Text(
                            text = "(${cand.scholarshipDetails})",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- DOCUMENTOS ANEXADOS ---
                    SectionTitle("Documentos Anexados")
                    if (cand.attachments.isEmpty()) {
                        Text(
                            text = "Sem documentos anexados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        cand.attachments.forEach { attachment ->
                            FileListItem(
                                attachment = attachment,
                                onClick = {
                                    val name = attachment.name.lowercase()
                                    if (name.endsWith(".pdf")) {
                                        val file = saveBase64ToTempFile(context, attachment.base64, attachment.name)
                                        selectedPdfFile = file
                                    } else {
                                        selectedImageBase64 = attachment.base64
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- DECLARAÇÕES ---
                    SectionTitle("Declarações")
                    BooleanStatusRow("Compromisso de Honra (Veracidade)", cand.truthfulnessDeclaration)
                    BooleanStatusRow("Autorização RGPD", cand.dataAuthorization)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- FINALIZAÇÃO ---
                    SectionTitle("Finalização")
                    // Se a data de submissão não existir, usamos a de criação
                    val subDate = cand.signatureDate.ifEmpty {
                        // Fallback formatado se necessário, ou vazio
                        "Data indisponível"
                    }
                    InfoRow("Data de submissão:", subDate)
                    InfoRow("Assinatura:", cand.signature)

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- BOTÕES DE AÇÃO ---
                    val isFinalState = cand.state == CandidatureState.ACEITE || cand.state == CandidatureState.REJEITADA

                    if (!isFinalState) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Botão Rejeitar
                            Button(
                                onClick = { showRejectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), // Vermelho
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rejeitar", fontWeight = FontWeight.Bold)
                            }

                            // Botão Aprovar
                            Button(
                                onClick = { viewModel.approveCandidature(cand.docId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)), // Verde
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aprovar", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Mensagem se já estiver fechada
                        Text(
                            text = "Esta candidatura encontra-se ${cand.state.name}",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            } else if (state.error != null) {
                Text(
                    text = state.error,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ----------------------------------------------------
// --- COMPONENTES VISUAIS (ESTILO FIGMA) ---
// ----------------------------------------------------

@Composable
private fun SectionTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = IpcaGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.Black,
            modifier = Modifier.width(140.dp) // Largura fixa para alinhar valores
        )
        Text(
            text = value.ifEmpty { "-" },
            fontSize = 15.sp,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BooleanStatusRow(label: String, isChecked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Sim",
                tint = IpcaGreen, // Verde
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Close, // Ou Icons.Filled.Close num círculo vermelho
                contentDescription = "Não",
                tint = Color(0xFFE57373), // Vermelho claro
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
private fun StatusBadgeDetails(state: CandidatureState) {
    val (backgroundColor, contentColor) = when (state) {
        CandidatureState.PENDENTE -> Pair(Color(0xFFFFE0B2), Color(0xFFEF6C00))
        CandidatureState.ACEITE -> Pair(Color(0xFFC8E6C9), Color(0xFF2E7D32))
        CandidatureState.REJEITADA -> Pair(Color(0xFFFFCDD2), Color(0xFFC62828))
    }
    Surface(color = backgroundColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = state.name,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FileListItem(attachment: DocumentAttachment, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, contentDescription = "PDF", tint = IpcaGreen)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = attachment.name,
            color = Color.Blue,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
        )
    }
}

// ----------------------------------------------------
// --- DIÁLOGOS AUXILIARES ---
// ----------------------------------------------------

@Composable
fun RejectDialog(
    reason: String,
    text: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text) },
        text = {
            Column {
                Text("Motivo da rejeição:")
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- Preview de Imagem e PDF (Mantém a lógica existente) ---
@Composable
fun ImagePreviewDialog(base64String: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                val bitmap = remember(base64String) {
                    try {
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        }
    }
}

@Composable
fun PdfPreviewDialog(file: File, onDismiss: () -> Unit) {
    val bitmaps = remember(file) { pdfToBitmaps(file) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().height(600.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fechar") }
                }
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(bitmaps.size) { index ->
                        Image(
                            bitmap = bitmaps[index].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

// Funções utilitárias (File, PDF) - Mantém igual
private fun saveBase64ToTempFile(context: Context, base64String: String, fileName: String): File? {
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val file = File(context.cacheDir, "temp_$fileName")
        val fos = FileOutputStream(file)
        fos.write(decodedBytes)
        fos.close()
        file
    } catch (e: Exception) { null }
}

private fun pdfToBitmaps(file: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }
        renderer.close()
        fileDescriptor.close()
    } catch (e: Exception) { e.printStackTrace() }
    return bitmaps
}