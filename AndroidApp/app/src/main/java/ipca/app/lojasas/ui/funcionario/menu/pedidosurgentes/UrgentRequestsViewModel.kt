package ipca.app.lojasas.ui.funcionario.pedidosurgentes

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
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.funcionario.FuncionarioRepository
import ipca.app.lojasas.data.requests.PedidoUrgenteItem
import ipca.app.lojasas.data.requests.UrgentRequestsRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

private const val YEAR_FILTER_ALL = "Todos"

data class UrgentRequestsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val funcionarioId: String = "",
    val pedidos: List<PedidoUrgenteItem> = emptyList(),
    val filteredPedidos: List<PedidoUrgenteItem> = emptyList(),
    val searchQuery: String = "",
    val selectedYear: String = YEAR_FILTER_ALL,
    val availableYears: List<String> = listOf(YEAR_FILTER_ALL)
)

@HiltViewModel
class UrgentRequestsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val urgentRequestsRepository: UrgentRequestsRepository
) : ViewModel() {

    var uiState = mutableStateOf(UrgentRequestsState())
        private set

    private var listener: ListenerHandle? = null

    init {
        loadFuncionarioId()
        listenPedidosUrgentes()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    private fun loadFuncionarioId() {
        val uid = authRepository.currentUserId()
        if (uid.isNullOrBlank()) {
            uiState.value = uiState.value.copy(error = "Utilizador não autenticado.", isLoading = false)
            return
        }

        funcionarioRepository.fetchFuncionarioIdByUid(
            uid = uid,
            onSuccess = { id ->
                uiState.value = uiState.value.copy(funcionarioId = id.orEmpty())
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
        )
    }

    private fun listenPedidosUrgentes() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        listener?.remove()
        listener = urgentRequestsRepository.listenUrgentRequests(
            onSuccess = { pedidos ->
                val years = pedidos.mapNotNull { item ->
                    item.dataSubmissao?.let { extractYear(it) }
                }.distinct()
                    .sortedDescending()
                val availableYears = listOf(YEAR_FILTER_ALL) + years
                val currentYear = uiState.value.selectedYear
                val resolvedYear = if (currentYear != YEAR_FILTER_ALL && !years.contains(currentYear)) {
                    YEAR_FILTER_ALL
                } else {
                    currentYear
                }

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    pedidos = pedidos,
                    selectedYear = resolvedYear,
                    availableYears = availableYears
                )
                applyFilters()
            },
            onError = { error ->
                uiState.value = uiState.value.copy(isLoading = false, error = error.message)
            }
        )
    }

    fun onSearchQueryChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onYearSelected(year: String) {
        uiState.value = uiState.value.copy(selectedYear = year)
        applyFilters()
    }

    private fun applyFilters() {
        val state = uiState.value
        val query = state.searchQuery.trim()
        var result = state.pedidos

        if (query.isNotBlank()) {
            result = result.filter { it.numeroMecanografico.contains(query, ignoreCase = true) }
        }
        if (state.selectedYear != YEAR_FILTER_ALL) {
            result = result.filter { item ->
                item.dataSubmissao?.let { extractYear(it) == state.selectedYear } ?: false
            }
        }

        uiState.value = state.copy(filteredPedidos = result)
    }

    fun exportToPDF(context: Context) {
        val state = uiState.value
        val data = state.filteredPedidos
        if (data.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        createUrgentRequestsListPdf(context, state)
    }

    fun exportPedidoPdf(context: Context, pedido: PedidoUrgenteItem) {
        createUrgentRequestPdf(context, pedido)
    }

    fun negarPedido(pedidoId: String, onDone: (Boolean) -> Unit = {}) {
        val funcId = uiState.value.funcionarioId
        val updates = mutableMapOf<String, Any>(
            "estado" to "Negado",
            "dataDecisao" to Date()
        )
        if (funcId.isNotBlank()) updates["funcionarioID"] = funcId

        urgentRequestsRepository.updateUrgentRequest(
            pedidoId = pedidoId,
            updates = updates,
            onSuccess = {
                AuditLogger.logAction("Negou pedido urgente", "pedido_ajuda", pedidoId)
                onDone(true)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onDone(false)
            }
        )
    }

    fun aprovarPedido(pedidoId: String, onDone: (Boolean) -> Unit = {}) {
        val funcId = uiState.value.funcionarioId
        val updates = mutableMapOf<String, Any>(
            "estado" to "Preparar_Apoio",
            "dataDecisao" to Date()
        )
        if (funcId.isNotBlank()) updates["funcionarioID"] = funcId

        urgentRequestsRepository.updateUrgentRequest(
            pedidoId = pedidoId,
            updates = updates,
            onSuccess = {
                AuditLogger.logAction("Aprovou pedido urgente", "pedido_ajuda", pedidoId)
                onDone(true)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onDone(false)
            }
        )
    }
}

private fun extractYear(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return calendar.get(Calendar.YEAR).toString()
}

private fun resolveEstadoLabel(estado: String, cestaId: String?): String {
    val estadoNorm = estado.trim().lowercase(Locale.getDefault())
    val isAnalise = estadoNorm.isBlank() || estadoNorm == "analise" ||
        estadoNorm == "em analise" || estadoNorm == "em_analise"
    return when {
        estadoNorm == "preparar_apoio" || estadoNorm == "preparar apoio" -> "Aprovado"
        estadoNorm == "negado" -> "Negado"
        isAnalise -> "Em Analise"
        !cestaId.isNullOrBlank() -> "Concluido (Cesta Criada)"
        else -> estado.ifBlank { "-" }
    }
}

private fun formatDate(date: Date?, formatter: SimpleDateFormat): String {
    return date?.let { formatter.format(it) } ?: "-"
}

private fun createUrgentRequestsListPdf(context: Context, state: UrgentRequestsState) {
    val data = state.filteredPedidos
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
    val maxWidth = pageWidth - margin * 2
    val lineHeight = 12f
    val blockSpacing = 8f
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
        y = drawTextLine(canvas, "Pedidos urgentes", margin, y, titlePaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Total: ${data.size}", margin, y, textPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
        y = drawTextLine(
            canvas,
            "Numero mecanografico: ${state.searchQuery.ifBlank { "-" }}",
            margin,
            y,
            mutedPaint,
            maxWidth,
            lineHeight
        )
        y = drawTextLine(canvas, "Ano: ${state.selectedYear}", margin, y, mutedPaint, maxWidth, lineHeight)
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

    data.forEach { pedido ->
        val estadoLabel = resolveEstadoLabel(pedido.estado, pedido.cestaId)
        val dataSub = formatDate(pedido.dataSubmissao, dateFormatter)
        val dataDec = formatDate(pedido.dataDecisao, dateFormatter)
        val descricao = pedido.descricao.ifBlank { "-" }
        val numero = pedido.numeroMecanografico.ifBlank { "-" }
        ensureSpace(4, blockSpacing)
        y = drawTextLine(canvas, "Pedido: ${pedido.id}", margin, y, sectionPaint, maxWidth, lineHeight)
        y = drawTextLine(
            canvas,
            "Apoiado: $numero | Estado: $estadoLabel",
            margin,
            y,
            textPaint,
            maxWidth,
            lineHeight
        )
        y = drawTextLine(
            canvas,
            "Submetido: $dataSub | Decisao: $dataDec",
            margin,
            y,
            mutedPaint,
            maxWidth,
            lineHeight
        )
        y = drawTextLine(canvas, "Descricao: $descricao", margin, y, textPaint, maxWidth, lineHeight)
        y += blockSpacing
    }

    document.finishPage(page)

    try {
        val fileName = "pedidos_urgentes_export_${System.currentTimeMillis()}.pdf"
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

private fun createUrgentRequestPdf(context: Context, pedido: PedidoUrgenteItem) {
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
    val maxWidth = pageWidth - margin * 2
    val lineHeight = 12f
    val sectionSpacing = 8f
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

    fun startPage(number: Int) {
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, number).create())
        canvas = page.canvas
        drawPdfHeaderFooter(canvas, pageWidth, pageHeight, headerHeight, footerHeight, margin, number, branding)
        drawPdfWatermark(canvas, pageWidth, contentTop, contentBottom, branding)
        y = contentTop
    }

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        startPage(pageNumber)
    }

    fun ensureLines(lines: Int, extraSpacing: Float = 0f) {
        val needed = lines * lineHeight + extraSpacing
        if (y + needed > contentBottom) {
            newPage()
        }
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

    drawPdfHeaderFooter(canvas, pageWidth, pageHeight, headerHeight, footerHeight, margin, pageNumber, branding)
    drawPdfWatermark(canvas, pageWidth, contentTop, contentBottom, branding)

    val estadoLabel = resolveEstadoLabel(pedido.estado, pedido.cestaId)
    val dataSub = formatDate(pedido.dataSubmissao, dateFormatter)
    val dataDec = formatDate(pedido.dataDecisao, dateFormatter)
    val numero = pedido.numeroMecanografico.ifBlank { "-" }
    val descricao = pedido.descricao.ifBlank { "-" }

    y = drawTextLine(canvas, "Detalhes do pedido urgente", margin, y, titlePaint, maxWidth, lineHeight)
    y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
    y += sectionSpacing

    drawSection("Pedido")
    drawLine("ID: ${pedido.id}")
    drawLine("Estado: $estadoLabel")
    drawLine("Submetido: $dataSub")
    drawLine("Decisao: $dataDec")
    if (!pedido.cestaId.isNullOrBlank()) {
        drawLine("Cesta ID: ${pedido.cestaId}")
    }
    y += sectionSpacing

    drawSection("Apoiado")
    drawLine("Numero mecanografico: $numero")
    y += sectionSpacing

    drawSection("Descricao")
    drawLine(descricao, mutedPaint)

    document.finishPage(page)

    try {
        val safeId = pedido.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "pedido_urgente_${safeId}_${System.currentTimeMillis()}.pdf"
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
