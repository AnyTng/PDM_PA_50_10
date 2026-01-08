package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.R
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.cestas.CestaItem
import ipca.app.lojasas.data.cestas.CestasRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

const val ESTADO_TODOS = "Todos"
const val ORIGEM_TODOS = "Todas"
const val YEAR_FILTER_ALL = "Todos"

data class CestasListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cestas: List<CestaItem> = emptyList(),
    val filteredCestas: List<CestaItem> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: String = ESTADO_TODOS,
    val selectedOrigem: String = ORIGEM_TODOS,
    val selectedYear: String = YEAR_FILTER_ALL,
    val availableEstados: List<String> = listOf(ESTADO_TODOS),
    val availableOrigens: List<String> = listOf(ORIGEM_TODOS),
    val availableYears: List<String> = listOf(YEAR_FILTER_ALL)
)

@HiltViewModel
class CestasListViewModel @Inject constructor(
    private val repository: CestasRepository
) : ViewModel() {

    private var listener: ListenerHandle? = null
    private var allCestas: List<CestaItem> = emptyList()
    private val estadoPriority = mapOf(
        "Agendada" to 0,
        "Por preparar" to 1,
        "Em preparar" to 2,
        "Entregue" to 3,
        "Nao levantou" to 4,
        "Cancelada" to 5,
        "Sem estado" to 6
    )

    var uiState = mutableStateOf(CestasListState())
        private set

    init {
        listenCestas()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    private fun listenCestas() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        listener?.remove()
        listener = repository.listenCestas(
            onSuccess = { list ->
                allCestas = list
                val estados = list
                    .map { normalizeEstado(it.estado) }
                    .distinct()
                val origens = list
                    .map { normalizeOrigem(it.origem) }
                    .distinct()
                val years = list.mapNotNull { extractCestaYear(it) }
                    .distinct()
                    .sortedDescending()
                val sortedEstados = estados.sortedWith(
                    compareBy<String> { estadoPriority[it] ?: 99 }
                        .thenBy { it.lowercase(Locale.getDefault()) }
                )
                val sortedOrigens = origens.sortedBy { it.lowercase(Locale.getDefault()) }
                val availableEstados = listOf(ESTADO_TODOS) + sortedEstados
                val availableOrigens = listOf(ORIGEM_TODOS) + sortedOrigens
                val availableYears = listOf(YEAR_FILTER_ALL) + years
                val currentEstado = uiState.value.selectedEstado
                val currentOrigem = uiState.value.selectedOrigem
                val currentYear = uiState.value.selectedYear
                val resolvedEstado = if (currentEstado != ESTADO_TODOS && !availableEstados.contains(currentEstado)) {
                    ESTADO_TODOS
                } else {
                    currentEstado
                }
                val resolvedOrigem = if (currentOrigem != ORIGEM_TODOS && !availableOrigens.contains(currentOrigem)) {
                    ORIGEM_TODOS
                } else {
                    currentOrigem
                }
                val resolvedYear = if (currentYear != YEAR_FILTER_ALL && !years.contains(currentYear)) {
                    YEAR_FILTER_ALL
                } else {
                    currentYear
                }
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = null,
                    cestas = list,
                    selectedEstado = resolvedEstado,
                    selectedOrigem = resolvedOrigem,
                    selectedYear = resolvedYear,
                    availableEstados = availableEstados,
                    availableOrigens = availableOrigens,
                    availableYears = availableYears
                )
                applyFilters()
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
        )
    }

    fun onSearchQueryChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onEstadoSelected(estado: String) {
        uiState.value = uiState.value.copy(selectedEstado = estado)
        applyFilters()
    }

    fun onOrigemSelected(origem: String) {
        uiState.value = uiState.value.copy(selectedOrigem = origem)
        applyFilters()
    }

    fun onYearSelected(year: String) {
        uiState.value = uiState.value.copy(selectedYear = year)
        applyFilters()
    }

    private fun applyFilters() {
        val state = uiState.value
        val query = state.searchQuery.trim()
        var result = allCestas

        if (query.isNotBlank()) {
            result = result.filter { it.apoiadoId.contains(query, ignoreCase = true) }
        }
        if (state.selectedEstado != ESTADO_TODOS) {
            result = result.filter { normalizeEstado(it.estado) == state.selectedEstado }
        }
        if (state.selectedOrigem != ORIGEM_TODOS) {
            result = result.filter { normalizeOrigem(it.origem) == state.selectedOrigem }
        }
        if (state.selectedYear != YEAR_FILTER_ALL) {
            result = result.filter { cesta ->
                extractCestaYear(cesta) == state.selectedYear
            }
        }

        uiState.value = state.copy(
            isLoading = false,
            filteredCestas = result
        )
    }

    fun exportToCSV(context: Context) {
        val data = uiState.value.filteredCestas
        if (data.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val header = listOf(
            "ID",
            "ApoiadoID",
            "FuncionarioID",
            "Estado",
            "Origem",
            "TipoApoio",
            "Faltas",
            "DataAgendada",
            "DataRecolha"
        ).joinToString(",")
        val body = data.joinToString("\n") { cesta ->
            listOf(
                cesta.id,
                cesta.apoiadoId,
                cesta.funcionarioId,
                normalizeEstado(cesta.estado),
                normalizeOrigem(cesta.origem),
                cesta.tipoApoio.orEmpty(),
                cesta.faltas.toString(),
                cesta.dataAgendada?.let { dateFormatter.format(it) }.orEmpty(),
                cesta.dataRecolha?.let { dateFormatter.format(it) }.orEmpty()
            ).joinToString(",") { csvValue(it) }
        }
        val csvContent = "$header\n$body"

        try {
            val fileName = "cestas_export_${System.currentTimeMillis()}.csv"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, fileName)
            FileOutputStream(file).use { output ->
                OutputStreamWriter(output, Charset.forName("windows-1252")).use { writer ->
                    writer.write(csvContent)
                }
            }
            Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPDF(context: Context) {
        val data = uiState.value.filteredCestas
        if (data.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val state = uiState.value
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val exportDate = dateFormatter.format(Date())
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
        val blockSpacing = 8f
        val lineHeight = 12f
        val maxWidth = pageWidth - margin * 2
        val ipcaGreen = Color.parseColor(HEX_GREEN_SAS)
        val branding = createPdfBranding(context, ipcaGreen, headerHeight, pageHeight)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint().apply {
            color = ipcaGreen
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }
        val mutedPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = contentTop

        fun drawContentHeader() {
            y = drawTextLine(canvas, "Cestas export", margin, y, titlePaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Total: ${data.size}", margin, y, textPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Estado: ${state.selectedEstado}", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Origem: ${state.selectedOrigem}", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Ano: ${state.selectedYear}", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(
                canvas,
                "Pesquisa: ${state.searchQuery.ifBlank { "-" }}",
                margin,
                y,
                mutedPaint,
                maxWidth,
                lineHeight
            )
            y += blockSpacing
        }

        fun startPage(number: Int) {
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
            canvas = page.canvas
            drawPdfHeaderFooter(canvas, pageWidth, pageHeight, headerHeight, footerHeight, margin, number, branding)
            drawPdfWatermark(canvas, pageWidth, contentTop, contentBottom, branding)
            y = contentTop
            drawContentHeader()
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            startPage(pageNumber)
        }

        fun ensureSpace(linesNeeded: Int, extraSpacing: Float = 0f) {
            val needed = linesNeeded * lineHeight + extraSpacing
            if (y + needed > contentBottom) {
                newPage()
            }
        }

        drawPdfHeaderFooter(canvas, pageWidth, pageHeight, headerHeight, footerHeight, margin, pageNumber, branding)
        drawPdfWatermark(canvas, pageWidth, contentTop, contentBottom, branding)
        drawContentHeader()

        data.forEach { cesta ->
            ensureSpace(5, blockSpacing)
            y = drawTextLine(canvas, "Cesta: ${cesta.id}", margin, y, sectionPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Apoiado: ${cesta.apoiadoId}", margin, y, textPaint, maxWidth, lineHeight)
            y = drawTextLine(
                canvas,
                "Estado: ${normalizeEstado(cesta.estado)} | Origem: ${normalizeOrigem(cesta.origem)}",
                margin,
                y,
                textPaint,
                maxWidth,
                lineHeight
            )
            val agendada = formatDate(cesta.dataAgendada, dateFormatter)
            val recolha = formatDate(cesta.dataRecolha, dateFormatter)
            y = drawTextLine(canvas, "Agendada: $agendada | Recolha: $recolha", margin, y, textPaint, maxWidth, lineHeight)
            y = drawTextLine(
                canvas,
                "Faltas: ${cesta.faltas} | Funcionario: ${cesta.funcionarioId}",
                margin,
                y,
                textPaint,
                maxWidth,
                lineHeight
            )
            y += blockSpacing
        }

        document.finishPage(page)

        try {
            val fileName = "cestas_export_${System.currentTimeMillis()}.pdf"
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

    fun cancelarCesta(cestaId: String) {
        repository.cancelarCesta(
            cestaId = cestaId,
            onSuccess = {},
            onError = { e -> uiState.value = uiState.value.copy(error = e.message) }
        )
    }

    fun marcarEntregue(cesta: CestaItem) {
        repository.marcarEntregue(
            cesta = cesta,
            onSuccess = {},
            onError = { e -> uiState.value = uiState.value.copy(error = e.message) }
        )
    }

    /**
     * Reagendar SEM contar como falta.
     */
    fun reagendarEntrega(cesta: CestaItem, novaData: Date) {
        repository.reagendarEntrega(
            cestaId = cesta.id,
            novaData = novaData,
            onSuccess = {},
            onError = { e -> uiState.value = uiState.value.copy(error = e.message) }
        )
    }

    fun reagendarComFalta(cesta: CestaItem, novaData: Date) {
        repository.reagendarComFalta(
            cesta = cesta,
            novaData = novaData,
            onSuccess = {},
            onError = { e -> uiState.value = uiState.value.copy(error = e.message) }
        )
    }

    /**
     * 3ª falta: não permite escolher dia, passa para Nao_Levantou.
     * Também devolve os produtos para Disponível para não ficarem presos em "Reservados".
     */
    fun registarTerceiraFaltaSemReagendar(cesta: CestaItem) {
        repository.registarTerceiraFaltaSemReagendar(
            cesta = cesta,
            onSuccess = {},
            onError = { e -> uiState.value = uiState.value.copy(error = e.message) }
        )
    }
}

private fun normalizeEstado(estado: String): String {
    val n = normalizeEstadoKey(estado)
    return when {
        n == "entregue" -> "Entregue"
        n == "agendada" -> "Agendada"
        n == "por preparar" || n == "por_preparar" -> "Por preparar"
        n == "em preparar" || n == "em_preparar" -> "Em preparar"
        n == "nao_levantou" || n == "nao levantou" -> "Nao levantou"
        n == "cancelada" -> "Cancelada"
        n.isBlank() -> "Sem estado"
        else -> estado.trim()
    }
}

private val estadoDiacriticsRegex = "\\p{Mn}+".toRegex()

private fun normalizeEstadoKey(value: String): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
    return estadoDiacriticsRegex.replace(normalized, "").trim().lowercase(Locale.getDefault())
}

private fun normalizeOrigem(origem: String?): String {
    val trimmed = origem?.trim().orEmpty()
    return if (trimmed.isBlank()) "Sem origem" else trimmed
}

private fun extractCestaYear(cesta: CestaItem): String? {
    val date = cesta.dataAgendada ?: cesta.dataRecolha
    return date?.let { extractYear(it) }
}

private fun extractYear(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return calendar.get(Calendar.YEAR).toString()
}

private fun formatDate(date: Date?, formatter: SimpleDateFormat): String {
    return date?.let { formatter.format(it) } ?: "-"
}

private fun drawTextLine(
    canvas: Canvas,
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

private fun drawTextClipped(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    maxWidth: Float
) {
    val clipped = clipText(text, paint, maxWidth)
    canvas.drawText(clipped, x, y, paint)
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

private data class PdfBranding(
    val headerPaint: Paint,
    val footerPaint: Paint,
    val footerTextPaint: Paint,
    val loginLogo: Bitmap,
    val sasLogo: Bitmap,
    val watermark: Bitmap,
    val watermarkPaint: Paint
)

private fun createPdfBranding(
    context: Context,
    ipcaGreen: Int,
    headerHeight: Float,
    pageHeight: Int
): PdfBranding {
    val headerPaint = Paint().apply { color = ipcaGreen }
    val footerPaint = Paint().apply { color = ipcaGreen }
    val footerTextPaint = Paint().apply {
        color = Color.WHITE
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

    return PdfBranding(
        headerPaint = headerPaint,
        footerPaint = footerPaint,
        footerTextPaint = footerTextPaint,
        loginLogo = loginLogoScaled,
        sasLogo = sasLogoScaled,
        watermark = watermark,
        watermarkPaint = watermarkPaint
    )
}

private fun drawPdfHeaderFooter(
    canvas: Canvas,
    pageWidth: Int,
    pageHeight: Int,
    headerHeight: Float,
    footerHeight: Float,
    margin: Float,
    pageNumber: Int,
    branding: PdfBranding
) {
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, branding.headerPaint)
    val headerPadding = 12f
    val loginY = (headerHeight - branding.loginLogo.height) / 2f
    canvas.drawBitmap(branding.loginLogo, headerPadding, loginY, null)
    val sasX = pageWidth - headerPadding - branding.sasLogo.width
    val sasY = (headerHeight - branding.sasLogo.height) / 2f
    canvas.drawBitmap(branding.sasLogo, sasX, sasY, null)

    val footerTop = pageHeight - footerHeight
    canvas.drawRect(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(), branding.footerPaint)
    val footerTextY = footerTop + (footerHeight + branding.footerTextPaint.textSize) / 2f - 2f
    branding.footerTextPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("Loja Social IPCA", margin, footerTextY, branding.footerTextPaint)
    branding.footerTextPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Pagina $pageNumber", pageWidth - margin, footerTextY, branding.footerTextPaint)
    branding.footerTextPaint.textAlign = Paint.Align.LEFT
}

private fun drawPdfWatermark(
    canvas: Canvas,
    pageWidth: Int,
    contentTop: Float,
    contentBottom: Float,
    branding: PdfBranding
) {
    val wmX = pageWidth - branding.watermark.width * 0.5f
    val wmY = (contentTop + contentBottom) / 2f - branding.watermark.height / 2f
    canvas.drawBitmap(branding.watermark, wmX, wmY, branding.watermarkPaint)
}

private fun scaleBitmapToHeight(bitmap: Bitmap, targetHeight: Float): Bitmap {
    val height = targetHeight.roundToInt().coerceAtLeast(1)
    val ratio = height.toFloat() / bitmap.height.toFloat()
    val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private fun csvValue(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    val needsQuotes = escaped.contains(',') || escaped.contains('"') ||
        escaped.contains('\n') || escaped.contains('\r')
    return if (needsQuotes) "\"$escaped\"" else escaped
}
