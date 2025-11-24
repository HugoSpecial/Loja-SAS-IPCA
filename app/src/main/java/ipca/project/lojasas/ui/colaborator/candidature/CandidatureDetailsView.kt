package ipca.project.lojasas.ui.colaborator.candidature

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.example.lojasas.models.DocumentoAnexo
import ipca.example.lojasas.models.EstadoCandidatura
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

    // --- DIÁLOGOS ---
    if (selectedImageBase64 != null) {
        ImagePreviewDialog(
            base64String = selectedImageBase64!!,
            onDismiss = { selectedImageBase64 = null }
        )
    }

    if (selectedPdfFile != null) {
        PdfPreviewDialog(
            file = selectedPdfFile!!,
            onDismiss = { selectedPdfFile = null }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rejeitar Candidatura") },
            text = {
                Column {
                    Text("Por favor, indique o motivo da rejeição:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            viewModel.rejectCandidature(candidatureId, rejectReason)
                            showRejectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Candidatura") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.candidatura != null) {
                val cand = state.candidatura

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    StatusBadgeDetails(estado = cand.estado)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- IDENTIFICAÇÃO ---
                    SectionTitle("Dados do Aluno")
                    InfoRow("Email:", cand.email ?: "N/A")
                    InfoRow("Telemóvel:", cand.telemovel ?: "N/A")
                    InfoRow("Nascimento:", cand.dataNascimento ?: "N/A")

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ACADÉMICOS ---
                    SectionTitle("Dados Académicos / Profissionais")
                    InfoRow("Tipo:", cand.tipo?.name ?: "N/A")
                    InfoRow("N.º Cartão:", cand.numeroCartao ?: "N/A") // ADICIONADO
                    InfoRow("Curso:", cand.curso ?: "N/A")
                    InfoRow("Ano Letivo:", cand.anoLetivo ?: "N/A")

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- APOIOS SOLICITADOS ---
                    SectionTitle("Produtos Solicitados")
                    CheckboxRow("Alimentares", cand.produtosAlimentares)
                    CheckboxRow("Higiene Pessoal", cand.produtosHigiene)
                    CheckboxRow("Limpeza", cand.produtosLimpeza)

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- OUTROS APOIOS (NOVO) ---
                    SectionTitle("Situação Socioeconómica")
                    InfoRow("Beneficiário FAES:", if (cand.faesApoiado == true) "Sim" else "Não")
                    InfoRow("Bolseiro:", if (cand.bolsaApoio == true) "Sim" else "Não")

                    if (cand.bolsaApoio == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Detalhes da Bolsa:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(cand.detalhesBolsa.ifEmpty { "Sem detalhes" }, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ANEXOS ---
                    SectionTitle("Documentos Anexos")

                    if (cand.anexos.isEmpty()) {
                        Text("Sem documentos anexados.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        cand.anexos.forEach { anexo ->
                            FileListItem(
                                anexo = anexo,
                                onClick = {
                                    val nome = anexo.nome.lowercase()
                                    if (nome.endsWith(".pdf")) {
                                        val file = saveBase64ToTempFile(context, anexo.base64, anexo.nome)
                                        selectedPdfFile = file
                                    } else {
                                        selectedImageBase64 = anexo.base64
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- DECLARAÇÕES (NOVO) ---
                    SectionTitle("Declarações")
                    CheckboxRow("Compromisso de Honra (Veracidade)", cand.declaracaoVeracidade)
                    CheckboxRow("Autorização RGPD", cand.autorizacaoDados)

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ASSINATURA (NOVO) ---
                    SectionTitle("Finalização")
                    InfoRow("Data Submissão:", cand.dataAssinatura.ifEmpty { "N/A" })
                    InfoRow("Assinado por:", cand.assinatura.ifEmpty { "N/A" })

                    Spacer(modifier = Modifier.height(30.dp))

                    // --- BOTÕES DE AÇÃO ---
                    val isFinalState = cand.estado == EstadoCandidatura.ACEITE || cand.estado == EstadoCandidatura.REJEITADA

                    if (isFinalState) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Candidatura Fechada",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Estado atual: ${cand.estado.name}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { showRejectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejeitar")
                            }

                            Button(
                                onClick = { viewModel.approveCandidature(cand.docId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aprovar")
                            }
                        }
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
// --- COMPONENTES VISUAIS AUXILIARES ---
// ----------------------------------------------------

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Divider()
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(130.dp), color = Color.DarkGray)
        Text(text = value, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (checked) Color(0xFF2E7D32) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusBadgeDetails(estado: EstadoCandidatura) {
    val (backgroundColor, contentColor) = when (estado) {
        EstadoCandidatura.PENDENTE -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        EstadoCandidatura.EM_ANALISE -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        EstadoCandidatura.ACEITE -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        EstadoCandidatura.REJEITADA -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    Surface(color = backgroundColor, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = estado.name.replace("_", " "),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FileListItem(anexo: DocumentoAnexo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = "Ficheiro", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = anexo.nome.ifEmpty { "Documento sem nome" }, fontWeight = FontWeight.Medium)
        }
    }
}

// ----------------------------------------------------
// --- DIÁLOGOS E UTILITÁRIOS ---
// ----------------------------------------------------

@Composable
fun ImagePreviewDialog(base64String: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().height(400.dp), shape = RoundedCornerShape(16.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val bitmap = remember(base64String) {
                    try {
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("Erro na imagem")
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualizar Documento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Divider()

                // --- Área de Leitura ---
                if (bitmaps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(bitmaps.size) { index ->
                            Card(
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Image(
                                    bitmap = bitmaps[index].asImageBitmap(),
                                    contentDescription = "Página ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                            Text(
                                text = "Página ${index + 1} de ${bitmaps.size}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                            )
                        }
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
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun pdfToBitmaps(file: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }
        renderer.close()
        fileDescriptor.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bitmaps
}