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
import ipca.project.lojasas.ui.components.InfoRow
import ipca.project.lojasas.ui.components.SectionTitle
import ipca.project.lojasas.ui.components.StatusBadge
import java.io.File
import java.io.FileOutputStream

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
            .background(MaterialTheme.colorScheme.background) // Adaptável
    ) {
        // 1. HEADER
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

        // 2. CONTEÚDO COM SCROLL
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (state.candidature != null) {
                val cand = state.candidature

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge de Estado
                    CandidatureStatusBadge(state = cand.state)

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
                    InfoRow("Beneficiário FAES:", if (cand.faesSupport == true) "Sim" else "Não")
                    InfoRow("Bolseiro:", if (cand.scholarshipSupport == true) "Sim" else "Não")

                    if (cand.scholarshipSupport == true && cand.scholarshipDetails.isNotEmpty()) {
                        Text(
                            text = "(${cand.scholarshipDetails})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                    val subDate = cand.signatureDate.ifEmpty { "Data indisponível" }
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rejeitar", fontWeight = FontWeight.Bold)
                            }

                            // Botão Aprovar
                            Button(
                                onClick = { viewModel.approveCandidature(cand.docId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aprovar", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Esta candidatura encontra-se ${cand.state.name}",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            } else if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ----------------------------------------------------
// --- COMPONENTES VISUAIS ---
// ----------------------------------------------------

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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Não",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CandidatureStatusBadge(state: CandidatureState) {
    // Cores adaptáveis com transparência para o fundo
    val (bg, color) = when (state) {
        CandidatureState.PENDENTE -> Color(0xFFEF6C00).copy(alpha = 0.1f) to Color(0xFFEF6C00)
        CandidatureState.ACEITE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.primary
        CandidatureState.REJEITADA -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error
    }
    StatusBadge(label = state.name, backgroundColor = bg, contentColor = color)
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
        Icon(
            Icons.Default.Info,
            contentDescription = "Documento",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = attachment.name,
            color = MaterialTheme.colorScheme.primary,
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
        containerColor = MaterialTheme.colorScheme.surface, // Branco ou Cinza Escuro
        title = {
            Text(text, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column {
                Text("Motivo da rejeição:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

// --- Preview de Imagem e PDF ---
@Composable
fun ImagePreviewDialog(base64String: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val bitmap = remember(base64String) {
                    try {
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.Black // Ícone preto para contraste sobre a imagem (geralmente clara) ou fundo
                    )
                }
            }
        }
    }
}

@Composable
fun PdfPreviewDialog(file: File, onDismiss: () -> Unit) {
    val bitmaps = remember(file) { pdfToBitmaps(file) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(600.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurface)
                    }
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

// Funções utilitárias (File, PDF)
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
            // Renderiza com fundo branco para garantir visibilidade
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