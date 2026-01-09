package ipca.app.lojasas.ui.funcionario.menu.campaigns

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PdfCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import ipca.app.lojasas.R
import ipca.app.lojasas.data.campaigns.CampaignStats
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.displayStatus
import ipca.app.lojasas.ui.theme.AndroidBlack
import ipca.app.lojasas.ui.theme.AndroidChartOrange
import ipca.app.lojasas.ui.theme.AndroidChartTeal
import ipca.app.lojasas.ui.theme.AndroidDarkGrey
import ipca.app.lojasas.ui.theme.AndroidGreenSas
import ipca.app.lojasas.ui.theme.AndroidGrey
import ipca.app.lojasas.ui.theme.AndroidMagenta
import ipca.app.lojasas.ui.theme.AndroidWhite
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

sealed class CampaignResultsPdfResult {
    data class Success(val fileName: String) : CampaignResultsPdfResult()
    data class Error(val message: String) : CampaignResultsPdfResult()
}

@Singleton
class CampaignResultsPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun export(
        campaignName: String,
        stats: CampaignStats,
        products: List<Product>
    ): CampaignResultsPdfResult {
        if (stats.totalProducts == 0 && products.isEmpty()) {
            return CampaignResultsPdfResult.Error("Sem dados para exportar.")
        }

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val headerHeight = 70f
        val footerHeight = 40f
        val margin = 36f
        val topPadding = 20f
        val bottomPadding = 18f
        val contentTop = headerHeight + topPadding
        val contentBottom = pageHeight - footerHeight - bottomPadding
        val maxWidth = pageWidth - margin * 2
        val lineHeight = 14f
        val sectionSpacing = 10f
        val ipcaGreen = AndroidGreenSas

        val headerPaint = Paint().apply { color = ipcaGreen }
        val footerPaint = Paint().apply { color = ipcaGreen }
        val titlePaint = Paint().apply {
            color = AndroidBlack
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint().apply {
            color = ipcaGreen
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = AndroidBlack
            textSize = 11f
        }
        val mutedPaint = Paint().apply {
            color = AndroidDarkGrey
            textSize = 10f
        }
        val footerTextPaint = Paint().apply {
            color = AndroidWhite
            textSize = 9f
        }

        val loginLogo = BitmapFactory.decodeResource(context.resources, R.drawable.loginlogo)
        val sasLogo = BitmapFactory.decodeResource(context.resources, R.drawable.sas)
        val watermarkBase = BitmapFactory.decodeResource(context.resources, R.drawable.lswhitecircle)
        val headerLogoHeight = headerHeight - 20f
        val loginLogoScaled = scaleBitmapToHeight(loginLogo, headerLogoHeight)
        val sasLogoScaled = scaleBitmapToHeight(sasLogo, headerLogoHeight)
        val watermark = scaleBitmapToHeight(watermarkBase, pageHeight * 0.55f)
        val watermarkPaint = Paint().apply { alpha = 35 }

        val chartColors = listOf(
            ipcaGreen,
            AndroidChartOrange,
            AndroidChartTeal,
            AndroidGrey,
            AndroidMagenta
        )
        val slices = stats.categoryPercentages
            .filter { it.value > 0f }
            .toList()
            .sortedByDescending { it.second }
            .mapIndexed { index, entry ->
                val label = if (entry.first.isBlank()) "Outros" else entry.first
                CategorySlice(
                    label = label,
                    count = stats.categoryCounts[entry.first] ?: 0,
                    percentage = entry.second,
                    color = chartColors[index % chartColors.size]
                )
            }

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val exportDate = dateFormatter.format(Date())
        val sortedProducts = products.sortedWith(
            compareBy<Product> { it.categoria?.trim()?.lowercase(Locale.getDefault()).orEmpty() }
                .thenBy { it.nomeProduto.trim().lowercase(Locale.getDefault()) }
        )

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        var y = contentTop

        fun drawHeaderFooter(target: PdfCanvas, number: Int) {
            target.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, headerPaint)
            val headerPadding = 12f
            val loginY = (headerHeight - loginLogoScaled.height) / 2f
            target.drawBitmap(loginLogoScaled, headerPadding, loginY, null)
            val sasX = pageWidth - headerPadding - sasLogoScaled.width
            val sasY = (headerHeight - sasLogoScaled.height) / 2f
            target.drawBitmap(sasLogoScaled, sasX, sasY, null)

            val footerTop = pageHeight - footerHeight
            target.drawRect(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(), footerPaint)
            val footerTextY = footerTop + (footerHeight + footerTextPaint.textSize) / 2f - 2f
            footerTextPaint.textAlign = Paint.Align.LEFT
            target.drawText("Loja Social IPCA", margin, footerTextY, footerTextPaint)
            footerTextPaint.textAlign = Paint.Align.RIGHT
            target.drawText("Pagina $number", pageWidth - margin, footerTextY, footerTextPaint)
            footerTextPaint.textAlign = Paint.Align.LEFT
        }

        fun drawWatermark(target: PdfCanvas) {
            val wmX = pageWidth - watermark.width * 0.5f
            val wmY = (contentTop + contentBottom) / 2f - watermark.height / 2f
            target.drawBitmap(watermark, wmX, wmY, watermarkPaint)
        }

        fun startPage(number: Int) {
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
            canvas = page.canvas
            drawHeaderFooter(canvas, number)
            drawWatermark(canvas)
            y = contentTop
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            startPage(pageNumber)
        }

        fun ensureSpace(heightNeeded: Float) {
            if (y + heightNeeded > contentBottom) {
                newPage()
            }
        }

        fun ensureLines(lines: Int, extraSpacing: Float = 0f) {
            ensureSpace(lines * lineHeight + extraSpacing)
        }

        fun drawLine(text: String, paint: Paint = textPaint) {
            ensureLines(1)
            y = drawTextLine(canvas, text, margin, y, paint, maxWidth, lineHeight)
        }

        fun drawSection(title: String) {
            ensureLines(1, sectionSpacing)
            y = drawTextLine(canvas, title, margin, y, sectionPaint, maxWidth, lineHeight)
            y += 2f
        }

        drawHeaderFooter(canvas, pageNumber)
        drawWatermark(canvas)

        y = drawTextLine(canvas, "Relatorio da campanha", margin, y, titlePaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, campaignName, margin, y, sectionPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Total angariado: ${stats.totalProducts} produtos", margin, y, textPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Produtos listados: ${products.size}", margin, y, textPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Categorias: ${stats.categoryCounts.size}", margin, y, textPaint, maxWidth, lineHeight)
        y += sectionSpacing

        drawSection("Grafico por categoria")
        if (slices.isEmpty()) {
            drawLine("Sem dados para grafico.")
        } else {
            val chartSize = 150f
            val legendLineHeight = 12f
            val legendSpacing = 6f
            val legendHeight = slices.size * (legendLineHeight + legendSpacing)
            val blockHeight = max(chartSize, legendHeight)
            ensureSpace(blockHeight + sectionSpacing)

            val chartLeft = margin
            val chartTop = y
            drawPieChart(canvas, chartLeft, chartTop, chartSize, slices)
            drawLegend(
                canvas = canvas,
                startX = chartLeft + chartSize + 20f,
                startY = chartTop,
                slices = slices,
                lineHeight = legendLineHeight,
                spacing = legendSpacing,
                textPaint = mutedPaint
            )
            y += blockHeight + sectionSpacing
        }

        drawSection("Resumo por categoria")
        if (slices.isEmpty()) {
            drawLine("Sem dados.")
        } else {
            slices.forEach { slice ->
                drawLine("${slice.label}: ${slice.count} un. (${formatPercentage(slice.percentage)}%)")
            }
        }
        y += sectionSpacing

        drawSection("Alimentos doados")
        if (sortedProducts.isEmpty()) {
            drawLine("Sem alimentos.")
        } else {
            sortedProducts.forEach { produto ->
                ensureLines(2, 4f)
                val name = produto.nomeProduto.ifBlank { produto.id }
                val categoria = produto.categoria?.takeIf { it.isNotBlank() } ?: "-"
                val subCategoria = produto.subCategoria.takeIf { it.isNotBlank() } ?: "-"
                val marca = produto.marca?.takeIf { it.isNotBlank() } ?: "-"
                val tamanho = formatTamanhoPdf(produto.tamanhoValor, produto.tamanhoUnidade)
                val origem = produto.doado?.takeIf { it.isNotBlank() } ?: "-"
                val estado = produto.displayStatus()

                y = drawTextLine(canvas, "- $name", margin, y, textPaint, maxWidth, lineHeight)
                y = drawTextLine(
                    canvas,
                    "  Categoria: $categoria / $subCategoria | Marca: $marca | Tam: $tamanho | Origem: $origem | Estado: $estado",
                    margin,
                    y,
                    mutedPaint,
                    maxWidth,
                    lineHeight
                )
                y += 4f
            }
        }

        document.finishPage(page)

        return try {
            val normalizedName = campaignName.trim().ifBlank { "campanha" }
            val safeName = normalizedName.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "campanha" }
            val fileName = "campanha_${safeName}_${System.currentTimeMillis()}.pdf"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, fileName)
            FileOutputStream(file).use { output -> document.writeTo(output) }
            CampaignResultsPdfResult.Success(fileName)
        } catch (e: Exception) {
            CampaignResultsPdfResult.Error("Erro ao exportar: ${e.message}")
        } finally {
            document.close()
        }
    }

    private data class CategorySlice(
        val label: String,
        val count: Int,
        val percentage: Float,
        val color: Int
    )

    private fun drawPieChart(
        canvas: PdfCanvas,
        x: Float,
        y: Float,
        size: Float,
        slices: List<CategorySlice>
    ) {
        var startAngle = -90f
        val rect = RectF(x, y, x + size, y + size)
        slices.forEach { slice ->
            val sweepAngle = (slice.percentage / 100f) * 360f
            val paint = Paint().apply {
                color = slice.color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }

    private fun drawLegend(
        canvas: PdfCanvas,
        startX: Float,
        startY: Float,
        slices: List<CategorySlice>,
        lineHeight: Float,
        spacing: Float,
        textPaint: Paint
    ) {
        val boxSize = 10f
        var y = startY + lineHeight
        slices.forEach { slice ->
            val colorPaint = Paint().apply {
                color = slice.color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(startX, y - boxSize + 2f, startX + boxSize, y + 2f, colorPaint)
            canvas.drawText(
                "${slice.label} (${formatPercentage(slice.percentage)}%)",
                startX + boxSize + 6f,
                y,
                textPaint
            )
            y += lineHeight + spacing
        }
    }

    private fun drawTextLine(
        canvas: PdfCanvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float
    ): Float {
        val clipped = clipText(text, paint, maxWidth)
        canvas.drawText(clipped, x, y, paint)
        return y + lineHeight
    }

    private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end -= 1
        }
        return if (end > 0) text.substring(0, end) + ellipsis else ellipsis
    }

    private fun scaleBitmapToHeight(bitmap: Bitmap, targetHeight: Float): Bitmap {
        val height = targetHeight.roundToInt().coerceAtLeast(1)
        val ratio = height.toFloat() / bitmap.height.toFloat()
        val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun formatTamanhoPdf(valor: Double?, unidade: String?): String {
        if (valor == null) return "-"
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val base = nf.format(valor)
        return if (unidade.isNullOrBlank()) base else "$base $unidade"
    }

    private fun formatPercentage(value: Float): String {
        return String.format(Locale.getDefault(), "%.1f", value)
    }
}
