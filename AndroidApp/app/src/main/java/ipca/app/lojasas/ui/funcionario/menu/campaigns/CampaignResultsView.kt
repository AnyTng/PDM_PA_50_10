package ipca.app.lojasas.ui.funcionario.menu.campaigns

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PdfCanvas
import android.graphics.Color as PdfColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ipca.app.lojasas.R
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.campaigns.CampaignStats
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.displayStatus
import ipca.app.lojasas.ui.theme.GreenSas
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CampaignResultsView(navController: NavController, campaignName: String) {
    var stats by remember { mutableStateOf<CampaignStats?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoadingProducts by remember { mutableStateOf(true) }
    var productsError by remember { mutableStateOf<String?>(null) }
    val repo = remember { CampaignRepository() }
    val context = LocalContext.current

    LaunchedEffect(campaignName) {
        stats = null
        products = emptyList()
        isLoadingProducts = true
        productsError = null
        repo.getCampaignStats(campaignName) { stats = it }
        repo.getCampaignProducts(
            campaignName = campaignName,
            onSuccess = { list ->
                products = list
                isLoadingProducts = false
            },
            onError = { error ->
                productsError = error.message ?: "Erro ao carregar produtos."
                isLoadingProducts = false
            }
        )
    }

    val s = stats
    CampaignResultsContent(
        campaignName = campaignName,
        isLoading = (s == null),
        totalProducts = s?.totalProducts ?: 0,
        categoryPercentages = s?.categoryPercentages ?: emptyMap(),
        categoryCounts = s?.categoryCounts ?: emptyMap(),
        isExportEnabled = s != null && !isLoadingProducts && productsError == null,
        exportHint = when {
            isLoadingProducts -> "A carregar dados para exportacao..."
            productsError != null -> "Erro ao carregar dados para exportacao."
            else -> null
        },
        onExportPdf = {
            val current = stats
            when {
                current == null -> {
                    Toast.makeText(context, "Dados indisponiveis.", Toast.LENGTH_SHORT).show()
                }
                productsError != null -> {
                    Toast.makeText(context, "Erro ao carregar produtos.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    exportCampaignResultsPdf(context, campaignName, current, products)
                }
            }
        }
    )
}

/**
 * UI "pura" -> dá para Preview sem repo.
 */
@Composable
private fun CampaignResultsContent(
    campaignName: String,
    isLoading: Boolean,
    totalProducts: Int,
    categoryPercentages: Map<String, Float>,
    categoryCounts: Map<String, Int>,
    isExportEnabled: Boolean,
    exportHint: String?,
    onExportPdf: () -> Unit
) {
    Scaffold(
        // topBar = { AppHeader("Resultados", true, { navController.popBackStack() }) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenSas)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        campaignName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSas
                    )
                    Text(
                        "Total angariado: $totalProducts produtos",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onExportPdf,
                        enabled = isExportEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar PDF", color = Color.White)
                    }
                    exportHint?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    if (totalProducts > 0 && categoryPercentages.isNotEmpty()) {
                        SimplePieChart(categoryPercentages)
                        Spacer(Modifier.height(32.dp))
                    } else {
                        Text("Sem dados para apresentar.")
                    }
                }

                item {
                    categoryCounts.forEach { (cat, count) ->
                        val pct = categoryPercentages[cat] ?: 0f
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.ifEmpty { "Outros" }, fontWeight = FontWeight.Bold)
                            Text("$count un. (${String.format("%.1f", pct)}%)")
                        }
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = GreenSas,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(data: Map<String, Float>) {
    val colors = listOf(GreenSas, Color(0xFFD88C28), Color(0xFF0F4C5C), Color.Gray, Color.Magenta)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = -90f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / 100f) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
        Spacer(Modifier.width(24.dp))
        Column {
            data.keys.forEachIndexed { index, name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = colors[index % colors.size],
                        shape = MaterialTheme.shapes.small
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(name.ifEmpty { "Outros" }, fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------- PREVIEWS ----------------

@Preview(showBackground = true, name = "CampaignResults - Loading")
@Composable
private fun CampaignResultsPreview_Loading() {
    CampaignResultsContent(
        campaignName = "Campanha de Inverno",
        isLoading = true,
        totalProducts = 0,
        categoryPercentages = emptyMap(),
        categoryCounts = emptyMap(),
        isExportEnabled = false,
        exportHint = "A carregar dados para exportacao...",
        onExportPdf = {}
    )
}

@Preview(showBackground = true, name = "CampaignResults - Com dados")
@Composable
private fun CampaignResultsPreview_WithData() {
    val pct = mapOf(
        "Alimentar" to 55f,
        "Higiene" to 25f,
        "Bebidas" to 10f,
        "Infantil" to 7f,
        "" to 3f // Outros
    )
    val counts = mapOf(
        "Alimentar" to 110,
        "Higiene" to 50,
        "Bebidas" to 20,
        "Infantil" to 14,
        "" to 6
    )

    CampaignResultsContent(
        campaignName = "Campanha de Inverno",
        isLoading = false,
        totalProducts = 200,
        categoryPercentages = pct,
        categoryCounts = counts,
        isExportEnabled = true,
        exportHint = null,
        onExportPdf = {}
    )
}

@Preview(showBackground = true, name = "CampaignResults - Sem dados")
@Composable
private fun CampaignResultsPreview_NoData() {
    CampaignResultsContent(
        campaignName = "Campanha X",
        isLoading = false,
        totalProducts = 0,
        categoryPercentages = emptyMap(),
        categoryCounts = emptyMap(),
        isExportEnabled = true,
        exportHint = null,
        onExportPdf = {}
    )
}

private data class CategorySlice(
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Int
)

private fun exportCampaignResultsPdf(
    context: Context,
    campaignName: String,
    stats: CampaignStats,
    products: List<Product>
) {
    if (stats.totalProducts == 0 && products.isEmpty()) {
        Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
        return
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
    val ipcaGreen = PdfColor.parseColor("#094E33")

    val headerPaint = Paint().apply { color = ipcaGreen }
    val footerPaint = Paint().apply { color = ipcaGreen }
    val titlePaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val sectionPaint = Paint().apply {
        color = ipcaGreen
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textPaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 11f
    }
    val mutedPaint = Paint().apply {
        color = PdfColor.DKGRAY
        textSize = 10f
    }
    val footerTextPaint = Paint().apply {
        color = PdfColor.WHITE
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
        PdfColor.parseColor("#D88C28"),
        PdfColor.parseColor("#0F4C5C"),
        PdfColor.GRAY,
        PdfColor.MAGENTA
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

    try {
        val safeName = campaignName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "campanha_${safeName}_${System.currentTimeMillis()}.pdf"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, fileName)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        document.close()
    }
}

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
