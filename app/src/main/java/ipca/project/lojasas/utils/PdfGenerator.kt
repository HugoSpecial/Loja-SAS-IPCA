package ipca.project.lojasas.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // =================================================================================
    // 1. RELATÓRIO DE STOCK (NOVO)
    // =================================================================================
    fun generateStockReport(
        context: Context,
        products: List<Product>,
        month: Int,
        year: Int
    ): File? {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        val paint = Paint()

        // --- COLUNAS ---
        val xProduto = 40f
        val xCategoria = 250f
        val xValido = 380f
        val xExpirado = 480f

        fun drawStockHeader(c: Canvas, pNum: Int): Float {
            val p = Paint()
            p.color = Color.BLACK

            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 20f
            c.drawText("Inventário de Stock - $month/$year", MARGIN, 50f, p)

            p.textSize = 14f
            p.typeface = Typeface.DEFAULT
            c.drawText("Página $pNum (Total Produtos: ${products.size})", MARGIN, 80f, p)

            p.textSize = 11f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val hY = 120f

            c.drawText("Produto", xProduto, hY, p)
            c.drawText("Categoria", xCategoria, hY, p)
            c.drawText("Qtd. Válida", xValido, hY, p)
            c.drawText("Qtd. Expirada", xExpirado, hY, p)

            p.strokeWidth = 1f
            c.drawLine(MARGIN, hY + 10f, PAGE_WIDTH - MARGIN, hY + 10f, p)

            return hY + 40f
        }

        var y = drawStockHeader(canvas, pageNumber)

        for (product in products) {
            // Calcular quantidades
            val validQty = product.batches.filter { isDateValid(it.validity) }.sumOf { it.quantity }
            val expiredQty = product.batches.filter { !isDateValid(it.validity) }.sumOf { it.quantity }

            if (y > PAGE_HEIGHT - 50) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                y = drawStockHeader(canvas, pageNumber)
            }

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = Color.BLACK

            // Produto (pode ser longo, cortar se necessário)
            var pName = product.name
            if (pName.length > 35) pName = pName.substring(0, 32) + "..."
            canvas.drawText(pName, xProduto, y, paint)

            // Categoria
            canvas.drawText(product.category, xCategoria, y, paint)

            // Qtd Válida
            canvas.drawText("$validQty un", xValido, y, paint)

            // Qtd Expirada (Vermelho se > 0)
            if (expiredQty > 0) {
                paint.color = Color.RED
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                paint.color = Color.LTGRAY
            }
            canvas.drawText("$expiredQty un", xExpirado, y, paint)

            y += 25f
        }

        pdfDocument.finishPage(currentPage)
        return saveFile(context, "Relatorio_Stock_${month}_${year}.pdf", pdfDocument)
    }

    // Função auxiliar para verificar data (usada no stock)
    private fun isDateValid(date: Date?): Boolean {
        if (date == null) return false
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return !date.before(today.time)
    }

    // =================================================================================
    // 2. RELATÓRIO DE ENTREGAS
    // =================================================================================
    fun generateDeliveryReport(
        context: Context,
        deliveries: List<Delivery>,
        month: Int,
        year: Int,
        collaboratorNames: Map<String, String>,
        beneficiaryNames: Map<String, String>
    ): File? {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        val paint = Paint()

        val xData = 40f
        val xBeneficiario = 130f
        val xEstado = 330f
        val xColaborador = 430f

        fun drawDeliveryHeader(c: Canvas, pNum: Int): Float {
            val p = Paint()
            p.color = Color.BLACK
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 20f
            c.drawText("Relatório de Entregas - $month/$year", MARGIN, 50f, p)
            p.textSize = 14f
            p.typeface = Typeface.DEFAULT
            c.drawText("Página $pNum (Total: ${deliveries.size})", MARGIN, 80f, p)
            p.textSize = 11f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val hY = 120f
            c.drawText("Data Marcação", xData, hY, p)
            c.drawText("Beneficiário", xBeneficiario, hY, p)
            c.drawText("Estado", xEstado, hY, p)
            c.drawText("Avaliado Por / Data", xColaborador, hY, p)
            p.strokeWidth = 1f
            c.drawLine(MARGIN, hY + 10f, PAGE_WIDTH - MARGIN, hY + 10f, p)
            return hY + 40f
        }

        var y = drawDeliveryHeader(canvas, pageNumber)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        for (delivery in deliveries) {
            if (y > PAGE_HEIGHT - 50) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                y = drawDeliveryHeader(canvas, pageNumber)
            }

            paint.color = Color.BLACK
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f

            val dateStr = delivery.surveyDate?.let { dateFormat.format(it) } ?: "--"
            canvas.drawText(dateStr, xData, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            var benName = beneficiaryNames[delivery.docId] ?: "Anónimo"
            if (benName.length > 28) benName = benName.substring(0, 25) + "..."
            canvas.drawText(benName, xBeneficiario, y, paint)

            paint.typeface = Typeface.DEFAULT
            val estadoTexto = when(delivery.state) {
                DeliveryState.PENDENTE -> "Pendente"
                DeliveryState.ENTREGUE -> "Entregue"
                DeliveryState.CANCELADO -> "Não Entregue"
                DeliveryState.EM_ANALISE -> "Em Análise"
            }
            canvas.drawText(estadoTexto, xEstado, y, paint)

            val colabId = delivery.evaluatedBy ?: ""
            var colabName = collaboratorNames[colabId] ?: "--"
            if (colabName.length > 18) colabName = colabName.substring(0, 15) + "..."
            paint.color = Color.BLACK
            paint.textSize = 10f
            canvas.drawText(colabName, xColaborador, y, paint)

            val evalDate = delivery.evaluationDate?.let { dateTimeFormat.format(it) }
            if (evalDate != null) {
                paint.color = Color.DKGRAY
                paint.textSize = 8f
                canvas.drawText(evalDate, xColaborador, y + 10f, paint)
            }
            y += 30f
        }
        pdfDocument.finishPage(currentPage)
        return saveFile(context, "Relatorio_Entregas_${month}_${year}.pdf", pdfDocument)
    }

    // =================================================================================
    // 3. RELATÓRIO DE PEDIDOS / ORDERS
    // =================================================================================
    fun generateOrderReport(
        context: Context,
        orders: List<Order>,
        month: Int,
        year: Int,
        collaboratorNames: Map<String, String>
    ): File? {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        val paint = Paint()
        val textPaint = TextPaint(paint)

        val xData = 40f
        val xInfo = 110f
        val xItems = 300f
        val itemsColumnWidth = (PAGE_WIDTH - MARGIN - xItems).toInt()

        fun drawOrderHeader(c: Canvas, pNum: Int): Float {
            val p = Paint()
            p.color = Color.BLACK
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 20f
            c.drawText("Relatório Mensal - $month/$year", MARGIN, 50f, p)
            p.textSize = 14f
            p.typeface = Typeface.DEFAULT
            c.drawText("Página $pNum (Total Pedidos: ${orders.size})", MARGIN, 80f, p)
            p.textSize = 12f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val hY = 120f
            c.drawText("Data", xData, hY, p)
            c.drawText("Beneficiário / Avaliação", xInfo, hY, p)
            c.drawText("Itens do Cabaz", xItems, hY, p)
            p.strokeWidth = 1f
            c.drawLine(MARGIN, hY + 10f, PAGE_WIDTH - MARGIN, hY + 10f, p)
            return hY + 40f
        }

        var y = drawOrderHeader(canvas, pageNumber)
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        for (order in orders) {
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.DEFAULT
            val itemsListStr = if (order.items.isEmpty()) "Sem itens" else order.items.joinToString(", ") { "${it.name} (${it.quantity})" }
            val itemsLayout = StaticLayout.Builder.obtain(itemsListStr, 0, itemsListStr.length, textPaint, itemsColumnWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(1.0f, 1.0f).setIncludePad(false).build()

            val rowHeight = max(40f, itemsLayout.height.toFloat())

            if (y + rowHeight > PAGE_HEIGHT - 50) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                y = drawOrderHeader(canvas, pageNumber)
            }

            paint.typeface = Typeface.DEFAULT; paint.textSize = 10f; paint.color = Color.BLACK
            canvas.drawText(order.orderDate?.let { dateFormat.format(it) } ?: "--", xData, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            var name = order.userName ?: "Anónimo"
            if (name.length > 25) name = name.substring(0, 22) + "..."
            canvas.drawText(name, xInfo, y, paint)

            paint.typeface = Typeface.DEFAULT; paint.textSize = 9f; paint.color = Color.DKGRAY
            val estadoInfo = when (order.accept) {
                OrderState.PENDENTE -> "Pendente"
                else -> {
                    val acao = if(order.accept == OrderState.ACEITE) "Aceite" else "Recusado"
                    val nomeColab = collaboratorNames[order.evaluatedBy ?: ""] ?: "Desconhecido"
                    val quando = order.evaluationDate?.let { dateTimeFormat.format(it) } ?: ""
                    "$acao por: $nomeColab\n($quando)"
                }
            }
            val lines = estadoInfo.split("\n")
            var yEst = y + 12f
            lines.forEach { canvas.drawText(it, xInfo, yEst, paint); yEst += 10f }

            paint.color = Color.BLACK
            canvas.save()
            canvas.translate(xItems, y - 8f)
            itemsLayout.draw(canvas)
            canvas.restore()

            y += rowHeight + 20f
        }
        pdfDocument.finishPage(currentPage)
        return saveFile(context, "Relatorio_Pedidos_${month}_${year}.pdf", pdfDocument)
    }

    private fun saveFile(context: Context, fileName: String, document: PdfDocument): File? {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)
        try {
            document.writeTo(FileOutputStream(file))
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }
}