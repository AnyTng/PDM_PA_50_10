package ipca.app.lojasas.ui.funcionario.menu.apoiados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PdfCanvas
import android.graphics.Color as PdfColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.R
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.utils.AccountValidity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class SortOrder {
    NOME_ASC, NOME_DESC,
    ID_ASC, ID_DESC,
    STATUS_ASC, STATUS_DESC
}

data class ApoiadoListState(
    val apoiados: List<ApoiadoItem> = emptyList(),
    val filteredApoiados: List<ApoiadoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentFilter: String = "Todos",
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NOME_ASC,
    val selectedApoiado: ApoiadoItem? = null
)

data class ApoiadoItem(
    val id: String,
    val nome: String,
    val email: String,
    val rawStatus: String, // Estado real na BD
    val displayStatus: String, // Estado visual (ex: "Apoio Pausado")
    val contacto: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val morada: String = "",
    val nacionalidade: String = "",
    val dataNascimento: java.util.Date? = null
)

data class ApoiadoPdfDetails(
    val id: String,
    val nome: String,
    val email: String,
    val emailApoiado: String,
    val contacto: String,
    val documentType: String,
    val documentNumber: String,
    val nacionalidade: String,
    val dataNascimento: Date?,
    val morada: String,
    val codPostal: String,
    val relacaoIPCA: String,
    val curso: String,
    val graoEnsino: String,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String,
    val necessidades: List<String>,
    val estadoConta: String,
    val dadosIncompletos: Boolean,
    val faltaDocumentos: Boolean,
    val mudarPass: Boolean,
    val uid: String,
    val extraFields: Map<String, Any?>
)

class ApoiadosListViewModel : ViewModel() {

    var uiState = mutableStateOf(ApoiadoListState())
        private set

    private val db = Firebase.firestore

    init {
        loadApoiados()
    }

    fun loadApoiados() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = value?.documents?.map { doc ->
                    val rawStatus = doc.getString("estadoConta") ?: ""

                    // --- VALIDADE DA CONTA ---
                    // Se a validade já passou (e não estiver Bloqueado/Negado), deve aparecer como "Conta Expirada".
                    val validadeTs = doc.getTimestamp("validadeConta") ?: doc.getTimestamp("validade")
                    val validadeDate = validadeTs?.toDate() ?: (doc.get("validadeConta") as? Date ?: doc.get("validade") as? Date)
                    val isExpired = validadeDate?.let { AccountValidity.isExpired(it) } ?: false

                    // --- MAPEAMENTO DE ESTADOS ---
                    val displayStatus = if (isExpired &&
                        !rawStatus.equals("Bloqueado", ignoreCase = true) &&
                        !rawStatus.equals("Negado", ignoreCase = true)
                    ) {
                        "Conta Expirada"
                    } else {
                        // --- MAPEAMENTO DE ESTADOS ---
                        when (rawStatus) {
                            "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
                            "Suspenso" -> "Apoio Pausado"
                            else -> rawStatus
                        }
                    }

                    ApoiadoItem(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sem Nome",
                        email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "Sem Email",
                        rawStatus = rawStatus,
                        displayStatus = displayStatus,
                        contacto = doc.getString("contacto") ?: "",
                        documentType = doc.getString("documentType") ?: "Doc",
                        documentNumber = doc.getString("documentNumber") ?: "",
                        morada = "${doc.getString("morada")}, ${doc.getString("codPostal")}",
                        nacionalidade = doc.getString("nacionalidade") ?: "",
                        dataNascimento = doc.getTimestamp("dataNascimento")?.toDate()
                    )
                } ?: emptyList()

                uiState.value = uiState.value.copy(apoiados = list, isLoading = false)
                applyFiltersAndSort()
            }
    }

    // --- FILTROS E PESQUISA ---

    fun onSearchQueryChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        applyFiltersAndSort()
    }

    fun applyFilter(filter: String) {
        uiState.value = uiState.value.copy(currentFilter = filter)
        applyFiltersAndSort()
    }

    fun applySort(order: SortOrder) {
        uiState.value = uiState.value.copy(sortOrder = order)
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = uiState.value
        var result = state.apoiados

        // 1. Filtrar por Estado (Dropdown)
        if (state.currentFilter != "Todos") {
            result = result.filter { it.displayStatus.equals(state.currentFilter, ignoreCase = true) }
        }

        // 2. Filtrar por Pesquisa
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            result = result.filter {
                it.nome.lowercase().contains(q) ||
                        it.id.lowercase().contains(q) ||
                        it.displayStatus.lowercase().contains(q)
            }
        }

        // 3. Ordenar
        result = when (state.sortOrder) {
            SortOrder.NOME_ASC -> result.sortedBy { it.nome.lowercase() }
            SortOrder.NOME_DESC -> result.sortedByDescending { it.nome.lowercase() }
            SortOrder.ID_ASC -> result.sortedBy { it.id.lowercase() }
            SortOrder.ID_DESC -> result.sortedByDescending { it.id.lowercase() }
            SortOrder.STATUS_ASC -> result.sortedBy { it.displayStatus.lowercase() }
            SortOrder.STATUS_DESC -> result.sortedByDescending { it.displayStatus.lowercase() }
        }

        uiState.value = state.copy(filteredApoiados = result)
    }

    fun selectApoiado(item: ApoiadoItem?) { uiState.value = uiState.value.copy(selectedApoiado = item) }

    // --- AÇÕES DE ESTADO ---

    fun unblockApoiado(id: String) {
        updateStatus(
            id,
            mapOf(
                "estadoConta" to "Correcao_Dados",
                "dadosIncompletos" to true,
                "bloqueadoPor" to null
            ),
            "Desbloqueou beneficiario"
        )
    }

    fun blockApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Bloqueado"), "Bloqueou beneficiario")
    }

    // Mantém na BD como "Suspenso", mas na UI mostra "Apoio Pausado"
    fun suspendApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Suspenso"), "Suspendeu beneficiario")
    }

    fun reactivateApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Aprovado"), "Reativou beneficiario")
    }

    private fun updateStatus(id: String, updates: Map<String, Any?>, action: String) {
        db.collection("apoiados").document(id).update(updates)
            .addOnSuccessListener {
                AuditLogger.logAction(action, "apoiado", id)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    fun exportToCSV(context: Context) {
        val data = uiState.value.filteredApoiados
        val csvHeader = "ID,Nome,Email,Status,Contacto,Nacionalidade\n"
        val csvBody = data.joinToString("\n") {
            "${it.id},\"${it.nome}\",${it.email},${it.displayStatus},${it.contacto},${it.nacionalidade}"
        }
        val csvContent = csvHeader + csvBody

        try {
            val fileName = "apoiados_export_${System.currentTimeMillis()}.csv"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, fileName)
            FileOutputStream(file).use { it.write(csvContent.toByteArray()) }
            Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPDF(context: Context) {
        val state = uiState.value
        val data = state.filteredApoiados
        if (data.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        createApoiadosListPdf(context, state)
    }

    fun exportApoiadoPdf(context: Context, apoiadoId: String) {
        db.collection("apoiados").document(apoiadoId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(context, "Apoiado nao encontrado.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val details = buildApoiadoPdfDetails(doc)
                createApoiadoPdf(context, details)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao carregar apoiado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

private fun buildApoiadoPdfDetails(doc: DocumentSnapshot): ApoiadoPdfDetails {
    val dataNascimento = doc.getTimestamp("dataNascimento")?.toDate()
        ?: (doc.get("dataNascimento") as? Date)
    val necessidades = (doc.get("necessidade") as? List<*>)?.mapNotNull { it as? String }
        ?: emptyList()
    val knownKeys = setOf(
        "uid",
        "email",
        "emailApoiado",
        "nome",
        "contacto",
        "documentNumber",
        "documentType",
        "nacionalidade",
        "dataNascimento",
        "morada",
        "codPostal",
        "relacaoIPCA",
        "curso",
        "graoEnsino",
        "apoioEmergenciaSocial",
        "bolsaEstudos",
        "valorBolsa",
        "necessidade",
        "estadoConta",
        "faltaDocumentos",
        "dadosIncompletos",
        "mudarPass"
    )
    val extraFields = doc.data?.filterKeys { key -> key !in knownKeys }.orEmpty()

    return ApoiadoPdfDetails(
        id = doc.id,
        nome = doc.getString("nome").orEmpty(),
        email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "",
        emailApoiado = doc.getString("emailApoiado").orEmpty(),
        contacto = doc.getString("contacto").orEmpty(),
        documentType = doc.getString("documentType") ?: "Doc",
        documentNumber = doc.getString("documentNumber").orEmpty(),
        nacionalidade = doc.getString("nacionalidade").orEmpty(),
        dataNascimento = dataNascimento,
        morada = doc.getString("morada").orEmpty(),
        codPostal = doc.getString("codPostal").orEmpty(),
        relacaoIPCA = doc.getString("relacaoIPCA").orEmpty(),
        curso = doc.getString("curso").orEmpty(),
        graoEnsino = doc.getString("graoEnsino").orEmpty(),
        apoioEmergencia = doc.getBoolean("apoioEmergenciaSocial") ?: false,
        bolsaEstudos = doc.getBoolean("bolsaEstudos") ?: false,
        valorBolsa = doc.getString("valorBolsa").orEmpty(),
        necessidades = necessidades,
        estadoConta = doc.getString("estadoConta").orEmpty(),
        dadosIncompletos = doc.getBoolean("dadosIncompletos") ?: false,
        faltaDocumentos = doc.getBoolean("faltaDocumentos") ?: false,
        mudarPass = doc.getBoolean("mudarPass") ?: false,
        uid = doc.getString("uid").orEmpty(),
        extraFields = extraFields
    )
}

private fun createApoiadosListPdf(context: Context, state: ApoiadoListState) {
    val data = state.filteredApoiados
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
    val ipcaGreen = PdfColor.parseColor("#094E33")
    val branding = createPdfBranding(context, ipcaGreen, headerHeight, pageHeight)

    val titlePaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 16f
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

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = contentTop

    fun drawContentHeader() {
        y = drawTextLine(canvas, "Lista de apoiados", margin, y, titlePaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Total: ${data.size}", margin, y, textPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "Filtro: ${state.currentFilter}", margin, y, mutedPaint, maxWidth, lineHeight)
        y = drawTextLine(
            canvas,
            "Pesquisa: ${state.searchQuery.ifBlank { "-" }} | Ordenacao: ${sortOrderLabel(state.sortOrder)}",
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

    data.forEach { apoiado ->
        ensureSpace(3, blockSpacing)
        val nome = apoiado.nome.ifBlank { apoiado.id }
        val email = apoiado.email.ifBlank { "-" }
        val contacto = apoiado.contacto.ifBlank { "-" }
        val nacionalidade = apoiado.nacionalidade.ifBlank { "-" }
        y = drawTextLine(canvas, "- $nome (ID: ${apoiado.id})", margin, y, sectionPaint, maxWidth, lineHeight)
        y = drawTextLine(
            canvas,
            "  Email: $email | Status: ${apoiado.displayStatus}",
            margin,
            y,
            textPaint,
            maxWidth,
            lineHeight
        )
        y = drawTextLine(
            canvas,
            "  Contacto: $contacto | Nacionalidade: $nacionalidade",
            margin,
            y,
            mutedPaint,
            maxWidth,
            lineHeight
        )
        y += blockSpacing
    }

    document.finishPage(page)

    try {
        val fileName = "apoiados_export_${System.currentTimeMillis()}.pdf"
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

private fun createApoiadoPdf(context: Context, details: ApoiadoPdfDetails) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateFormatterShort = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
    val ipcaGreen = PdfColor.parseColor("#094E33")
    val branding = createPdfBranding(context, ipcaGreen, headerHeight, pageHeight)

    val titlePaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 16f
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

    y = drawTextLine(canvas, "Detalhes do apoiado", margin, y, titlePaint, maxWidth, lineHeight)
    y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
    y += sectionSpacing

    drawSection("Conta")
    drawLine("ID: ${details.id}")
    if (details.uid.isNotBlank()) drawLine("UID: ${details.uid}")
    drawLine("Nome: ${details.nome.ifBlank { "-" }}")
    drawLine("Estado: ${details.estadoConta.ifBlank { "-" }}")
    drawLine("Email: ${details.email.ifBlank { "-" }}")
    if (details.emailApoiado.isNotBlank() && details.emailApoiado != details.email) {
        drawLine("Email apoiado: ${details.emailApoiado}")
    }
    drawLine("Contacto: ${details.contacto.ifBlank { "-" }}")
    y += sectionSpacing

    drawSection("Identificacao")
    drawLine("${details.documentType.ifBlank { "Documento" }}: ${details.documentNumber.ifBlank { "-" }}")
    drawLine("Nacionalidade: ${details.nacionalidade.ifBlank { "-" }}")
    drawLine("Data nascimento: ${details.dataNascimento?.let { dateFormatterShort.format(it) } ?: "-"}")
    drawLine("Morada: ${details.morada.ifBlank { "-" }}")
    drawLine("Codigo postal: ${details.codPostal.ifBlank { "-" }}")
    y += sectionSpacing

    drawSection("Relacao IPCA")
    drawLine("Relacao: ${details.relacaoIPCA.ifBlank { "-" }}")
    drawLine("Curso: ${details.curso.ifBlank { "-" }}")
    drawLine("Grau ensino: ${details.graoEnsino.ifBlank { "-" }}")
    y += sectionSpacing

    drawSection("Apoio social")
    drawLine("Apoio emergencia: ${formatBoolean(details.apoioEmergencia)}")
    drawLine("Bolsa estudos: ${formatBoolean(details.bolsaEstudos)}")
    if (details.bolsaEstudos) {
        drawLine("Valor bolsa: ${details.valorBolsa.ifBlank { "-" }}")
    }
    y += sectionSpacing

    drawSection("Necessidades")
    if (details.necessidades.isEmpty()) {
        drawLine("Sem necessidades registadas.")
    } else {
        details.necessidades.forEach { necessidade ->
            drawLine("- $necessidade", mutedPaint)
        }
    }
    y += sectionSpacing

    drawSection("Estado da conta")
    drawLine("Dados incompletos: ${formatBoolean(details.dadosIncompletos)}")
    drawLine("Falta documentos: ${formatBoolean(details.faltaDocumentos)}")
    drawLine("Mudar password: ${formatBoolean(details.mudarPass)}")

    if (details.extraFields.isNotEmpty()) {
        y += sectionSpacing
        drawSection("Outros campos")
        details.extraFields.toSortedMap().forEach { (key, value) ->
            val rendered = formatExtraValue(value, dateFormatterShort)
            drawLine("$key: $rendered", mutedPaint)
        }
    }

    document.finishPage(page)

    try {
        val safeId = details.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "apoiado_${safeId}_${System.currentTimeMillis()}.pdf"
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

private fun formatBoolean(value: Boolean): String = if (value) "Sim" else "Nao"

private fun formatExtraValue(value: Any?, formatter: SimpleDateFormat): String {
    return when (value) {
        null -> "-"
        is Boolean -> formatBoolean(value)
        is com.google.firebase.Timestamp -> formatter.format(value.toDate())
        is Date -> formatter.format(value)
        is List<*> -> value.joinToString(", ") { item -> item?.toString().orEmpty() }
        else -> value.toString()
    }
}

private fun sortOrderLabel(order: SortOrder): String {
    return when (order) {
        SortOrder.NOME_ASC -> "Nome (A-Z)"
        SortOrder.NOME_DESC -> "Nome (Z-A)"
        SortOrder.ID_ASC -> "ID (A-Z)"
        SortOrder.ID_DESC -> "ID (Z-A)"
        SortOrder.STATUS_ASC -> "Status (A-Z)"
        SortOrder.STATUS_DESC -> "Status (Z-A)"
    }
}
