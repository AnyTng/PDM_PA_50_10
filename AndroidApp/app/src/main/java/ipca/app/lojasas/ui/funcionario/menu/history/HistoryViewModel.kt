package ipca.app.lojasas.ui.funcionario.menu.history

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PdfCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HistoryEntry(
    val id: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String?,
    val funcionarioNome: String,
    val funcionarioId: String,
    val timestamp: Date?
)

data class FuncionarioOption(
    val id: String,
    val label: String
)

data class HistoryState(
    val entries: List<HistoryEntry> = emptyList(),
    val filteredEntries: List<HistoryEntry> = emptyList(),
    val availableActions: List<String> = emptyList(),
    val availableYears: List<String> = emptyList(),
    val availableFuncionarios: List<FuncionarioOption> = emptyList(),
    val selectedActions: Set<String> = emptySet(),
    val selectedYears: Set<String> = emptySet(),
    val selectedFuncionarios: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HistoryViewModel : ViewModel() {
    var uiState = mutableStateOf(HistoryState())
        private set

    private val db = Firebase.firestore
    private var listener: ListenerRegistration? = null

    init {
        listenHistory()
    }

    fun toggleAction(action: String) {
        val selected = toggleSet(uiState.value.selectedActions, action)
        applyFilters(selectedActions = selected)
    }

    fun toggleYear(year: String) {
        val selected = toggleSet(uiState.value.selectedYears, year)
        applyFilters(selectedYears = selected)
    }

    fun toggleFuncionario(funcionarioId: String) {
        val selected = toggleSet(uiState.value.selectedFuncionarios, funcionarioId)
        applyFilters(selectedFuncionarios = selected)
    }

    fun clearFilters() {
        applyFilters(emptySet(), emptySet(), emptySet())
    }

    fun exportToPDF(context: Context) {
        val state = uiState.value
        if (state.filteredEntries.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        createHistoryPdf(context, state)
    }

    private fun listenHistory() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)
        listener?.remove()
        listener = db.collectionGroup("historico")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents.orEmpty().map { doc ->
                    HistoryEntry(
                        id = doc.id,
                        action = doc.getString("action") ?: "",
                        entity = doc.getString("entity") ?: "",
                        entityId = doc.getString("entityId") ?: "",
                        details = doc.getString("details"),
                        funcionarioNome = doc.getString("funcionarioNome") ?: "",
                        funcionarioId = doc.getString("funcionarioId") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.toDate()
                    )
                }.sortedByDescending { it.timestamp ?: Date(0) }

                updateEntries(entries)
            }
    }

    private fun updateEntries(entries: List<HistoryEntry>) {
        val actions = entries.mapNotNull { it.action.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
        val years = entries.mapNotNull { entryYear(it) }
            .distinct()
            .sortedDescending()
        val funcionarios = entries.groupBy { it.funcionarioId }
            .mapNotNull { (id, list) ->
                if (id.isBlank()) return@mapNotNull null
                val name = list.mapNotNull { it.funcionarioNome.takeIf(String::isNotBlank) }
                    .distinct()
                    .firstOrNull()
                val label = if (!name.isNullOrBlank()) "$name ($id)" else id
                FuncionarioOption(id = id, label = label)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }

        val selectedActions = uiState.value.selectedActions.intersect(actions.toSet())
        val selectedYears = uiState.value.selectedYears.intersect(years.toSet())
        val selectedFuncionarios = uiState.value.selectedFuncionarios
            .intersect(funcionarios.map { it.id }.toSet())

        val filtered = filterEntries(entries, selectedActions, selectedYears, selectedFuncionarios)

        uiState.value = uiState.value.copy(
            entries = entries,
            filteredEntries = filtered,
            availableActions = actions,
            availableYears = years,
            availableFuncionarios = funcionarios,
            selectedActions = selectedActions,
            selectedYears = selectedYears,
            selectedFuncionarios = selectedFuncionarios,
            isLoading = false,
            error = null
        )
    }

    private fun applyFilters(
        selectedActions: Set<String> = uiState.value.selectedActions,
        selectedYears: Set<String> = uiState.value.selectedYears,
        selectedFuncionarios: Set<String> = uiState.value.selectedFuncionarios
    ) {
        val entries = uiState.value.entries
        val filtered = filterEntries(entries, selectedActions, selectedYears, selectedFuncionarios)
        uiState.value = uiState.value.copy(
            filteredEntries = filtered,
            selectedActions = selectedActions,
            selectedYears = selectedYears,
            selectedFuncionarios = selectedFuncionarios
        )
    }

    private fun filterEntries(
        entries: List<HistoryEntry>,
        selectedActions: Set<String>,
        selectedYears: Set<String>,
        selectedFuncionarios: Set<String>
    ): List<HistoryEntry> {
        return entries.filter { entry ->
            val actionOk = selectedActions.isEmpty() || entry.action in selectedActions
            val year = entryYear(entry)
            val yearOk = selectedYears.isEmpty() || (year != null && year in selectedYears)
            val funcOk = selectedFuncionarios.isEmpty() || entry.funcionarioId in selectedFuncionarios
            actionOk && yearOk && funcOk
        }.sortedByDescending { it.timestamp ?: Date(0) }
    }

    private fun entryYear(entry: HistoryEntry): String? {
        val ts = entry.timestamp ?: return null
        val cal = Calendar.getInstance()
        cal.time = ts
        return cal.get(Calendar.YEAR).toString()
    }

    private fun toggleSet(current: Set<String>, value: String): Set<String> {
        val next = current.toMutableSet()
        if (!next.add(value)) {
            next.remove(value)
        }
        return next
    }

    private fun createHistoryPdf(context: Context, state: HistoryState) {
        val data = state.filteredEntries
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
        val ipcaGreen = Color.parseColor("#094E33")
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

        fun filterLabel(label: String, values: Collection<String>): String {
            return if (values.isEmpty()) "$label: Todos" else "$label: ${values.joinToString(", ")}"
        }

        val funcionariosLabel = if (state.selectedFuncionarios.isEmpty()) {
            listOf("Todos")
        } else {
            state.availableFuncionarios.filter { it.id in state.selectedFuncionarios }
                .map { it.label }
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = contentTop

        fun drawContentHeader() {
            y = drawTextLine(canvas, "Historico de acoes", margin, y, titlePaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Total: ${data.size}", margin, y, textPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(
                canvas,
                filterLabel("Funcionarios", funcionariosLabel),
                margin,
                y,
                mutedPaint,
                maxWidth,
                lineHeight
            )
            y = drawTextLine(
                canvas,
                filterLabel("Anos", state.selectedYears),
                margin,
                y,
                mutedPaint,
                maxWidth,
                lineHeight
            )
            y = drawTextLine(
                canvas,
                filterLabel("Acoes", state.selectedActions),
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

        data.forEach { entry ->
            val whenText = entry.timestamp?.let { dateFormatter.format(it) } ?: "-"
            val actorName = entry.funcionarioNome.ifBlank { entry.funcionarioId }.ifBlank { "Funcionario" }
            val entityLabel = when {
                entry.entity.isNotBlank() && entry.entityId.isNotBlank() -> "${entry.entity} (${entry.entityId})"
                entry.entity.isNotBlank() -> entry.entity
                entry.entityId.isNotBlank() -> entry.entityId
                else -> "-"
            }

            ensureSpace(4, blockSpacing)
            y = drawTextLine(canvas, "- ${entry.action.ifBlank { "Acao" }}", margin, y, sectionPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "  Entidade: $entityLabel", margin, y, textPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "  Por: $actorName | Quando: $whenText", margin, y, textPaint, maxWidth, lineHeight)
            if (!entry.details.isNullOrBlank()) {
                y = drawTextLine(canvas, "  Detalhes: ${entry.details}", margin, y, mutedPaint, maxWidth, lineHeight)
            }
            y += blockSpacing
        }

        document.finishPage(page)

        try {
            val fileName = "historico_${System.currentTimeMillis()}.pdf"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, fileName)
            FileOutputStream(file).use { output -> document.writeTo(output) }
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
        canvas: PdfCanvas,
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
        canvas: PdfCanvas,
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

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
