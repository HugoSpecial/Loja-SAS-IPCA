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
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

object PdfGenerator {

    fun generateOrderReport(
        context: Context,
        orders: List<Order>,
        month: Int,
        year: Int,
        collaboratorNames: Map<String, String>
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = TextPaint(paint)

        // Configurações da Página A4
        val pageWidth = 595
        val pageHeight = 842
        val contentWidth = 595f
        val rightMargin = 40f

        // Colunas
        val xData = 40f
        val xInfo = 110f
        val xItems = 300f
        val itemsColumnWidth = (contentWidth - rightMargin - xItems).toInt()

        // Variáveis de controlo de página
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var y = 0f // Será definido pela função drawHeader

        // --- FUNÇÃO AUXILIAR: DESENHAR CABEÇALHO ---
        // Usamos uma função local para poder chamar sempre que criamos uma nova página
        fun drawHeader(c: Canvas, pNum: Int): Float {
            val p = Paint()

            // Título
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 20f
            p.color = Color.BLACK
            c.drawText("Relatório Mensal - $month/$year", 40f, 50f, p)

            p.textSize = 14f
            p.typeface = Typeface.DEFAULT
            c.drawText("Página $pNum (Total Pedidos: ${orders.size})", 40f, 80f, p)

            // Cabeçalho da Tabela
            p.textSize = 12f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerY = 120f

            c.drawText("Data", xData, headerY, p)
            c.drawText("Beneficiário / Avaliação", xInfo, headerY, p)
            c.drawText("Itens do Cabaz", xItems, headerY, p)

            p.strokeWidth = 1f
            c.drawLine(40f, headerY + 10f, pageWidth - rightMargin, headerY + 10f, p)

            return headerY + 40f // Retorna a nova posição Y inicial para os dados
        }

        // Desenhar cabeçalho da primeira página
        y = drawHeader(canvas, pageNumber)

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        // --- CICLO PELOS PEDIDOS ---
        for (order in orders) {

            // 1. Calcular altura necessária para esta linha ANTES de desenhar
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = Color.BLACK

            val itemsListStr = if (order.items.isEmpty()) "Sem itens" else
                order.items.joinToString(", ") { "${it.name} (${it.quantity})" }

            val itemsLayout = StaticLayout.Builder.obtain(
                itemsListStr, 0, itemsListStr.length, textPaint, itemsColumnWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.0f, 1.0f)
                .setIncludePad(false)
                .build()

            // Altura da linha = Máximo entre (texto fixo ~40px) e (altura da lista de itens)
            val rowHeight = max(40f, itemsLayout.height.toFloat())

            // 2. VERIFICAR SE CABE NA PÁGINA (Margem inferior de 50px)
            if (y + rowHeight > pageHeight - 50) {
                // Não cabe! Fechar página atual e criar nova.
                pdfDocument.finishPage(currentPage)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas

                // Desenhar cabeçalho na nova página e reiniciar Y
                y = drawHeader(canvas, pageNumber)
            }

            // 3. DESENHAR OS DADOS (Agora sabemos que cabe)

            // Coluna 1: Data
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.BLACK
            paint.textSize = 10f
            val dateStr = order.orderDate?.let { dateFormat.format(it) } ?: "--"
            canvas.drawText(dateStr, xData, y, paint)

            // Coluna 2: Nome e Info
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            var name = order.userName ?: "Anónimo"
            if (name.length > 25) name = name.substring(0, 22) + "..."
            canvas.drawText(name, xInfo, y, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            paint.color = Color.DKGRAY

            val estadoInfo = when (order.accept) {
                OrderState.PENDENTE -> "Pendente"
                else -> {
                    val acao = if(order.accept == OrderState.ACEITE) "Aceite" else "Recusado"
                    val idColab = order.evaluatedBy ?: ""
                    val nomeColab = collaboratorNames[idColab] ?: "Desconhecido"
                    val quando = order.evaluationDate?.let { dateTimeFormat.format(it) } ?: ""
                    "$acao por: $nomeColab\n($quando)"
                }
            }

            // Desenhar multilinha manual para o estado
            val estadoLines = estadoInfo.split("\n")
            var yEstado = y + 12f
            for (line in estadoLines) {
                canvas.drawText(line, xInfo, yEstado, paint)
                yEstado += 10f
            }

            // Coluna 3: Itens (StaticLayout)
            canvas.save()
            canvas.translate(xItems, y - 8f)
            itemsLayout.draw(canvas)
            canvas.restore()

            // 4. AVANÇAR Y
            y += rowHeight + 20f // Espaçamento entre encomendas
        }

        // Fechar a última página
        pdfDocument.finishPage(currentPage)

        // Guardar ficheiro
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, "Relatorio_${month}_${year}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}