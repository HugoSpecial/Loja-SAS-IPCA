package ipca.project.lojasas.ui.collaborator.candidature

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Face // Icone para visualizar
import androidx.compose.material.icons.outlined.KeyboardArrowDown // Icone para download
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
import java.io.OutputStream
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

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

    // --- NOVO: Estado para o documento selecionado para opções (View/Download) ---
    var selectedDocumentForOptions by remember { mutableStateOf<DocumentAttachment?>(null) }

    LaunchedEffect(candidatureId) {
        viewModel.fetchCandidature(candidatureId)
    }

    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            navController.popBackStack()
        }
    }

    // --- DIÁLOGOS ---

    // 1. Dialog de Opções (Visualizar / Download)
    if (selectedDocumentForOptions != null) {
        DocumentOptionsDialog(
            document = selectedDocumentForOptions!!,
            onDismiss = { selectedDocumentForOptions = null },
            onView = { doc ->
                // Lógica de visualização
                val name = doc.name.lowercase()
                if (name.endsWith(".pdf")) {
                    val file = saveBase64ToTempFile(context, doc.base64, doc.name)
                    selectedPdfFile = file
                } else {
                    selectedImageBase64 = doc.base64
                }
                selectedDocumentForOptions = null
            },
            onDownload = { doc ->
                // Lógica de Download
                saveToDownloads(context, doc.base64, doc.name)
                selectedDocumentForOptions = null
            }
        )
    }

    // 2. Previews
    if (selectedImageBase64 != null) {
        ImagePreviewDialog(base64String = selectedImageBase64!!, onDismiss = { selectedImageBase64 = null })
    }
    if (selectedPdfFile != null) {
        PdfPreviewDialog(file = selectedPdfFile!!, onDismiss = { selectedPdfFile = null })
    }

    // 3. Rejeição
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
            .background(MaterialTheme.colorScheme.background)
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

                    CandidatureStatusBadge(state = cand.state)

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Dados do aluno")
                    InfoRow("Email:", cand.email)
                    InfoRow("Telemóvel:", cand.mobilePhone)
                    InfoRow("Nascimento:", cand.birthDate)

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Dados Académicos / Profissionais")
                    InfoRow("Tipo:", cand.type?.name ?: "-")
                    InfoRow("N.º Cartão:", cand.cardNumber)
                    InfoRow("Curso:", cand.course ?: "-")
                    InfoRow("Ano letivo:", cand.academicYear)

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Produtos solicitados")
                    BooleanStatusRow("Alimentares", cand.foodProducts)
                    BooleanStatusRow("Higiene Pessoal", cand.hygieneProducts)
                    BooleanStatusRow("Limpeza", cand.cleaningProducts)

                    Spacer(modifier = Modifier.height(24.dp))

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
                                    // Ao clicar, abrimos o diálogo de opções em vez de abrir direto
                                    selectedDocumentForOptions = attachment
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Declarações")
                    BooleanStatusRow("Compromisso de Honra (Veracidade)", cand.truthfulnessDeclaration)
                    BooleanStatusRow("Autorização RGPD", cand.dataAuthorization)

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Finalização")
                    val subDate = cand.signatureDate.ifEmpty { "Data indisponível" }
                    InfoRow("Data de submissão:", subDate)
                    InfoRow("Assinatura:", cand.signature)

                    Spacer(modifier = Modifier.height(40.dp))

                    val isFinalState = cand.state == CandidatureState.ACEITE || cand.state == CandidatureState.REJEITADA

                    if (!isFinalState) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
// --- DIÁLOGOS E FUNÇÕES AUXILIARES ---
// ----------------------------------------------------

@Composable
fun DocumentOptionsDialog(
    document: DocumentAttachment,
    onDismiss: () -> Unit,
    onView: (DocumentAttachment) -> Unit,
    onDownload: (DocumentAttachment) -> Unit
) {
    // --- CORREÇÃO: Removi o AlertDialog que estava aqui a mais ---

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = document.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Opção Visualizar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onView(document) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id=R.drawable.file_dock), // Ou Icons.Default.Info
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Visualizar Documento", fontSize = 16.sp)
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Opção Download
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDownload(document) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id=R.drawable.file_download), // Ou Icons.Filled.Check
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Fazer Download", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão Cancelar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// --- FUNÇÃO DE DOWNLOAD COMPATÍVEL (API 24+) ---
fun saveToDownloads(context: Context, base64String: String, fileName: String) {
    try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                val mimeType = when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "application/octet-stream"
                }
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(decodedBytes)
                }
                Toast.makeText(context, "Download concluído (Galeria/Downloads)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Erro ao criar ficheiro", Toast.LENGTH_SHORT).show()
            }

        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(decodedBytes)
            }

            Toast.makeText(context, "Download guardado em: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Erro ao fazer download: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- COMPONENTES VISUAIS (Mantidos iguais ao original, apenas reposicionados se necessário) ---

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

// --- OUTROS DIÁLOGOS (Reject, ImagePreview, PdfPreview, Helpers) ---
// (Estes mantêm-se iguais ao teu código original, incluídos aqui para completude)

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
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text, color = MaterialTheme.colorScheme.onSurface) },
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
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Black)
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