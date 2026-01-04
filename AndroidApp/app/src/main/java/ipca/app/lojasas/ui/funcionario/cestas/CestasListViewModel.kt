package ipca.app.lojasas.ui.funcionario.cestas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale

data class CestaItem(
    val id: String,
    val apoiadoId: String,
    val funcionarioId: String,
    val dataAgendada: Date? = null,
    val dataRecolha: Date? = null,
    val estado: String = "",
    val faltas: Int = 0,
    val origem: String? = null,
    val tipoApoio: String? = null
)

const val ESTADO_TODOS = "Todos"
const val ORIGEM_TODOS = "Todas"

data class CestasListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cestas: List<CestaItem> = emptyList(),
    val filteredCestas: List<CestaItem> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: String = ESTADO_TODOS,
    val selectedOrigem: String = ORIGEM_TODOS,
    val availableEstados: List<String> = listOf(ESTADO_TODOS),
    val availableOrigens: List<String> = listOf(ORIGEM_TODOS)
)

class CestasListViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var listener: ListenerRegistration? = null
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
        listener = db.collection("cestas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty().map { doc ->
                    val dataAgendada = doc.getTimestamp("dataAgendada")?.toDate() ?: (doc.get("dataAgendada") as? Date)
                    val dataRecolha = doc.getTimestamp("dataRecolha")?.toDate() ?: (doc.get("dataRecolha") as? Date)
                    val faltas = (doc.getLong("faltas") ?: 0L).toInt()
                    CestaItem(
                        id = doc.id,
                        apoiadoId = doc.getString("apoiadoID")?.trim().orEmpty(),
                        funcionarioId = doc.getString("funcionarioID")?.trim().orEmpty(),
                        dataAgendada = dataAgendada,
                        dataRecolha = dataRecolha,
                        estado = doc.getString("estadoCesta")?.trim().orEmpty(),
                        faltas = faltas,
                        origem = doc.getString("origem"),
                        tipoApoio = doc.getString("tipoApoio")
                    )
                }

                allCestas = list
                val estados = list
                    .map { normalizeEstado(it.estado) }
                    .distinct()
                val origens = list
                    .map { normalizeOrigem(it.origem) }
                    .distinct()
                val sortedEstados = estados.sortedWith(
                    compareBy<String> { estadoPriority[it] ?: 99 }
                        .thenBy { it.lowercase(Locale.getDefault()) }
                )
                val sortedOrigens = origens.sortedBy { it.lowercase(Locale.getDefault()) }
                val availableEstados = listOf(ESTADO_TODOS) + sortedEstados
                val availableOrigens = listOf(ORIGEM_TODOS) + sortedOrigens
                val currentEstado = uiState.value.selectedEstado
                val currentOrigem = uiState.value.selectedOrigem
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
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = null,
                    cestas = list,
                    selectedEstado = resolvedEstado,
                    selectedOrigem = resolvedOrigem,
                    availableEstados = availableEstados,
                    availableOrigens = availableOrigens
                )
                applyFilters()
            }
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

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val lineHeight = 12f
        val blockSpacing = 8f
        val maxWidth = pageWidth - margin * 2

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun drawHeader() {
            drawTextClipped(canvas, "Cestas export", margin, y, titlePaint, maxWidth)
            y += 16f
            drawTextClipped(canvas, "Total: ${data.size}", margin, y, textPaint, maxWidth)
            y += 16f
        }

        fun ensureSpace(linesNeeded: Int) {
            val needed = linesNeeded * lineHeight + blockSpacing
            if (y + needed > pageHeight - margin) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin
                drawHeader()
            }
        }

        drawHeader()

        data.forEach { cesta ->
            ensureSpace(5)
            y = drawTextLine(canvas, "Cesta: ${cesta.id}", margin, y, titlePaint, maxWidth, lineHeight)
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
        val now = Date()
        val cestaRef = db.collection("cestas").document(cestaId)

        // Ao cancelar: cesta -> Cancelada e produtos voltam a Disponível.
        db.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            // Libertar produtos reservados
            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                val prodSnap = txn.get(prodRef)
                if (prodSnap.exists()) {
                    val reservaId = prodSnap.getString("cestaReservaId")?.trim().orEmpty()
                    val estado = prodSnap.getString("estadoProduto")?.trim().orEmpty()

                    val podeLibertar = reservaId.isBlank() || reservaId == cestaId ||
                            estado.equals("Reservado", ignoreCase = true)

                    if (podeLibertar) {
                        txn.update(
                            prodRef,
                            mapOf(
                                "estadoProduto" to "Disponivel",
                                "cestaReservaId" to FieldValue.delete(),
                                "reservadoEm" to FieldValue.delete()
                            )
                        )
                    }
                }
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Cancelada",
                    "dataCancelada" to now
                )
            )

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
    }

    fun marcarEntregue(cesta: CestaItem) {
        val now = Date()
        val cestaRef = db.collection("cestas").document(cesta.id)

        // Ao marcar como entregue: cesta -> Entregue e produtos -> Entregue.
        db.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to "Entregue",
                        "cestaEntregaId" to cesta.id,
                        "entregueEm" to now,
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Entregue",
                    "dataRecolha" to now,
                    "dataEntregue" to now
                )
            )

            // Atualiza o último levantamento do apoiado (para ordenação na seleção)
            if (cesta.apoiadoId.isNotBlank()) {
                txn.update(db.collection("apoiados").document(cesta.apoiadoId), mapOf("ultimoLevantamento" to now))
            }

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
    }

    /**
     * Reagendar SEM contar como falta.
     */
    fun reagendarEntrega(cesta: CestaItem, novaData: Date) {
        db.collection("cestas").document(cesta.id)
            .update(
                mapOf(
                    "dataAgendada" to novaData,
                    "dataRecolha" to novaData,
                    "dataReagendada" to Date()
                )
            )
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    fun reagendarComFalta(cesta: CestaItem, novaData: Date) {
        val novasFaltas = cesta.faltas + 1
        val novoEstado = if (novasFaltas >= 3) "Nao_Levantou" else "Agendada"

        val updates = mutableMapOf<String, Any>(
            "faltas" to novasFaltas,
            "estadoCesta" to novoEstado,
            "dataAgendada" to novaData,
            // Mantemos também dataRecolha para compatibilidade com o Home do apoiado
            "dataRecolha" to novaData,
            "dataUltimaFalta" to Date()
        )

        db.collection("cestas").document(cesta.id)
            .update(updates)
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    /**
     * 3ª falta: não permite escolher dia, passa para Nao_Levantou.
     * Também devolve os produtos para Disponível para não ficarem presos em "Reservados".
     */
    fun registarTerceiraFaltaSemReagendar(cesta: CestaItem) {
        val now = Date()
        val cestaRef = db.collection("cestas").document(cesta.id)

        db.runTransaction { txn ->
            val snap = txn.get(cestaRef)
            val produtoIds = (snap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            val novasFaltas = cesta.faltas + 1
            txn.update(
                cestaRef,
                mapOf(
                    "faltas" to novasFaltas,
                    "estadoCesta" to "Nao_Levantou",
                    "dataUltimaFalta" to now
                )
            )

            // Libertar produtos
            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to "Disponivel",
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
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

private fun csvValue(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    val needsQuotes = escaped.contains(',') || escaped.contains('"') ||
        escaped.contains('\n') || escaped.contains('\r')
    return if (needsQuotes) "\"$escaped\"" else escaped
}
