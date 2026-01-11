package ipca.app.lojasas.ui.funcionario.stock.expired

import ipca.app.lojasas.ui.theme.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PdfCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.R
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.donations.ExpiredDonationEntry
import ipca.app.lojasas.data.donations.ExpiredDonationsRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import ipca.app.lojasas.ui.funcionario.stock.ProductGroupUi
import ipca.app.lojasas.ui.funcionario.stock.ProductSortOption
import ipca.app.lojasas.ui.funcionario.stock.identity
import ipca.app.lojasas.ui.funcionario.stock.isExpiredVisible
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class ExpiredDonationProductItem(
    val name: String,
    val brand: String,
    val category: String,
    val subCategory: String,
    val sizeLabel: String,
    val count: Int,
    val idLabel: String? = null,
    val isMissing: Boolean = false
)

data class ExpiredProductsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val groups: List<ProductGroupUi> = emptyList(),
    val sortOption: ProductSortOption = ProductSortOption.EXPIRY_ASC,
    val selectedIds: Set<String> = emptySet(),
    val isDonating: Boolean = false,
    val donationError: String? = null,
    val historyEntries: List<ExpiredDonationEntry> = emptyList(),
    val isHistoryLoading: Boolean = true,
    val historyError: String? = null,
    val detailsEntry: ExpiredDonationEntry? = null,
    val detailsProducts: List<ExpiredDonationProductItem> = emptyList(),
    val isDetailsLoading: Boolean = false,
    val detailsError: String? = null,
    val exportingDonationId: String? = null
)

@HiltViewModel
class ExpiredProductsViewModel @Inject constructor(
    private val repository: ProductsRepository,
    private val donationsRepository: ExpiredDonationsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ExpiredProductsUiState())
    val uiState: State<ExpiredProductsUiState> = _uiState

    private var listener: ListenerHandle? = null
    private var historyListener: ListenerHandle? = null
    private var allGroups: List<ProductGroupUi> = emptyList()
    private val donationProductsCache = mutableMapOf<String, List<ExpiredDonationProductItem>>()

    init {
        listener = repository.listenAllProducts(
            onSuccess = { products ->
                val reference = Date()
                val expired = products.filter { product ->
                    product.isExpiredVisible(reference)
                }
                allGroups = groupIdenticalProducts(expired)
                pruneSelection()
                applyFilter()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar produtos.",
                    groups = emptyList()
                )
            }
        )
        listenDonationHistory()
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
        applyFilter()
    }

    fun onSortSelected(option: ProductSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilter()
    }

    fun toggleSelection(group: ProductGroupUi) {
        if (_uiState.value.isDonating) return
        val ids = group.productIds
        if (ids.isEmpty()) return
        val current = _uiState.value.selectedIds
        val allSelected = ids.all { current.contains(it) }
        val updated = if (allSelected) {
            current - ids
        } else {
            current + ids
        }
        _uiState.value = _uiState.value.copy(selectedIds = updated, donationError = null)
    }

    fun clearSelection() {
        if (_uiState.value.isDonating) return
        _uiState.value = _uiState.value.copy(selectedIds = emptySet(), donationError = null)
    }

    fun donateSelected(associationName: String, associationContact: String) {
        val state = _uiState.value
        if (state.isDonating) return
        if (state.selectedIds.isEmpty()) {
            _uiState.value = state.copy(donationError = "Selecione produtos para doar.")
            return
        }
        val userId = authRepository.currentUserId().orEmpty()
        if (userId.isBlank()) {
            _uiState.value = state.copy(donationError = "Sem utilizador autenticado.")
            return
        }

        val reference = Date()
        val validIds = allGroups
            .filter { it.product.isExpiredVisible(reference) }
            .flatMap { it.productIds }
            .toSet()
        val idsToDonate = state.selectedIds.intersect(validIds).toList()

        if (idsToDonate.isEmpty()) {
            _uiState.value = state.copy(donationError = "Selecao invalida para doar.")
            return
        }

        _uiState.value = state.copy(isDonating = true, donationError = null)
        donationsRepository.donateExpiredProducts(
            productIds = idsToDonate,
            associationName = associationName,
            associationContact = associationContact,
            employeeId = userId,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isDonating = false,
                    selectedIds = emptySet(),
                    donationError = null
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isDonating = false,
                    donationError = e.message ?: "Erro ao doar produtos."
                )
            }
        )
    }

    fun openDonationDetails(entry: ExpiredDonationEntry) {
        val cached = donationProductsCache[entry.id]
        if (cached != null) {
            _uiState.value = _uiState.value.copy(
                detailsEntry = entry,
                detailsProducts = cached,
                isDetailsLoading = false,
                detailsError = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            detailsEntry = entry,
            detailsProducts = emptyList(),
            isDetailsLoading = true,
            detailsError = null
        )

        if (entry.productIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(isDetailsLoading = false)
            return
        }

        donationsRepository.fetchDonationProducts(
            productIds = entry.productIds,
            onSuccess = { products ->
                val items = buildDonationProductItems(entry.productIds, products)
                donationProductsCache[entry.id] = items
                _uiState.value = _uiState.value.copy(
                    isDetailsLoading = false,
                    detailsProducts = items,
                    detailsError = null
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isDetailsLoading = false,
                    detailsError = e.message ?: "Erro ao carregar detalhes."
                )
            }
        )
    }

    fun closeDonationDetails() {
        _uiState.value = _uiState.value.copy(
            detailsEntry = null,
            detailsProducts = emptyList(),
            isDetailsLoading = false,
            detailsError = null
        )
    }

    fun exportDonationPdf(context: Context, entry: ExpiredDonationEntry) {
        val state = _uiState.value
        if (state.exportingDonationId != null) return
        if (entry.productIds.isEmpty()) {
            Toast.makeText(context, "Sem produtos para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.value = state.copy(exportingDonationId = entry.id)
        val cached = donationProductsCache[entry.id]
        if (cached != null) {
            createDonationPdf(context, entry, cached)
            _uiState.value = _uiState.value.copy(exportingDonationId = null)
            return
        }

        donationsRepository.fetchDonationProducts(
            productIds = entry.productIds,
            onSuccess = { products ->
                val items = buildDonationProductItems(entry.productIds, products)
                donationProductsCache[entry.id] = items
                createDonationPdf(context, entry, items)
                _uiState.value = _uiState.value.copy(exportingDonationId = null)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(exportingDonationId = null)
                Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun listenDonationHistory() {
        _uiState.value = _uiState.value.copy(isHistoryLoading = true, historyError = null)
        historyListener?.remove()
        historyListener = donationsRepository.listenExpiredDonations(
            onSuccess = { entries ->
                _uiState.value = _uiState.value.copy(
                    isHistoryLoading = false,
                    historyError = null,
                    historyEntries = entries
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isHistoryLoading = false,
                    historyError = e.message ?: "Erro ao carregar historico.",
                    historyEntries = emptyList()
                )
            }
        )
    }

    private fun buildDonationProductItems(
        productIds: List<String>,
        products: List<Product>
    ): List<ExpiredDonationProductItem> {
        val grouped = products.groupBy { it.identity() }
        val items = grouped.values.map { list ->
            val product = list.first()
            ExpiredDonationProductItem(
                name = product.nomeProduto.ifBlank { product.id },
                brand = safeLabel(product.marca),
                category = safeLabel(product.categoria),
                subCategory = safeLabel(product.subCategoria),
                sizeLabel = formatDonationSize(product.tamanhoValor, product.tamanhoUnidade),
                count = list.size
            )
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }

        val knownIds = products.map { it.id }.toSet()
        val missingIds = productIds.filter { it !in knownIds }
        val missingItems = missingIds.map { id ->
            ExpiredDonationProductItem(
                name = "Produto indisponivel",
                brand = "-",
                category = "-",
                subCategory = "-",
                sizeLabel = "-",
                count = 1,
                idLabel = id,
                isMissing = true
            )
        }

        return items + missingItems
    }

    private fun safeLabel(value: String?): String {
        return value?.takeIf { it.isNotBlank() } ?: "-"
    }

    private fun formatDonationSize(value: Double?, unit: String?): String {
        if (value == null) return "-"
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val base = nf.format(value)
        return if (unit.isNullOrBlank()) base else "$base $unit"
    }

    private fun createDonationPdf(
        context: Context,
        entry: ExpiredDonationEntry,
        items: List<ExpiredDonationProductItem>
    ) {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val exportDate = dateFormatter.format(Date())
        val donationDate = entry.donationDate?.let { dateFormatter.format(it) } ?: "-"
        val totalProducts = items.sumOf { it.count }

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
        val ipcaGreen = AndroidGreenSas
        val branding = createPdfBranding(context, ipcaGreen, headerHeight, pageHeight)

        val titlePaint = Paint().apply {
            color = AndroidBlack
            textSize = 16f
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

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = contentTop

        fun drawContentHeader() {
            y = drawTextLine(canvas, "Doacao fora de validade", margin, y, titlePaint, maxWidth, lineHeight)
            y = drawTextLine(
                canvas,
                "Associacao: ${entry.associationName.ifBlank { "-" }}",
                margin,
                y,
                textPaint,
                maxWidth,
                lineHeight
            )
            y = drawTextLine(
                canvas,
                "Contacto: ${entry.associationContact.ifBlank { "-" }}",
                margin,
                y,
                textPaint,
                maxWidth,
                lineHeight
            )
            y = drawTextLine(canvas, "Data da doacao: $donationDate", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Data de exportacao: $exportDate", margin, y, mutedPaint, maxWidth, lineHeight)
            y = drawTextLine(canvas, "Produtos: $totalProducts", margin, y, textPaint, maxWidth, lineHeight)
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

        if (items.isEmpty()) {
            ensureSpace(1, blockSpacing)
            y = drawTextLine(canvas, "Sem produtos associados.", margin, y, textPaint, maxWidth, lineHeight)
        } else {
            items.forEach { item ->
                val lines = if (item.idLabel != null) 3 else 2
                ensureSpace(lines, blockSpacing)
                y = drawTextLine(canvas, "- ${item.name}", margin, y, sectionPaint, maxWidth, lineHeight)
                val detail = "Qtd: ${item.count} | Categoria: ${item.category} | Sub: ${item.subCategory} | Marca: ${item.brand} | Tam: ${item.sizeLabel}"
                y = drawTextLine(canvas, "  $detail", margin, y, textPaint, maxWidth, lineHeight)
                if (item.idLabel != null) {
                    y = drawTextLine(canvas, "  ID: ${item.idLabel}", margin, y, mutedPaint, maxWidth, lineHeight)
                }
                y += blockSpacing
            }
        }

        document.finishPage(page)

        try {
            val fileName = "doacao_fora_validade_${System.currentTimeMillis()}.pdf"
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

    private fun applyFilter() {
        val state = _uiState.value
        val q = state.searchQuery.trim()

        var filtered = allGroups
        if (q.isNotBlank()) {
            filtered = filtered.filter {
                it.product.nomeProduto.contains(q, ignoreCase = true) ||
                    (it.product.codBarras?.contains(q, ignoreCase = true) == true) ||
                    (it.product.marca?.contains(q, ignoreCase = true) == true)
            }
        }

        val sorted = when (state.sortOption) {
            ProductSortOption.EXPIRY_ASC -> filtered.sortedWith(expiryComparator())
            ProductSortOption.EXPIRY_DESC -> filtered.sortedWith(expiryComparator().reversed())
            ProductSortOption.SIZE_ASC -> filtered.sortedWith(sizeComparator())
            ProductSortOption.SIZE_DESC -> filtered.sortedWith(sizeComparator().reversed())
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            groups = sorted
        )
    }

    private fun pruneSelection() {
        val selected = _uiState.value.selectedIds
        if (selected.isEmpty()) return
        val validIds = allGroups.flatMap { it.productIds }.toSet()
        val pruned = selected.intersect(validIds)
        if (pruned.size != selected.size) {
            _uiState.value = _uiState.value.copy(selectedIds = pruned)
        }
    }

    override fun onCleared() {
        listener?.remove()
        historyListener?.remove()
        listener = null
        historyListener = null
        super.onCleared()
    }
}

private fun expiryComparator(): Comparator<ProductGroupUi> {
    return Comparator<ProductGroupUi> { a, b ->
        val aTime = a.product.validade?.time
        val bTime = b.product.validade?.time
        when {
            aTime == null && bTime == null -> 0
            aTime == null -> 1
            bTime == null -> -1
            else -> aTime.compareTo(bTime)
        }
    }.thenBy { it.product.nomeProduto.lowercase(Locale.getDefault()) }
}

private fun sizeComparator(): Comparator<ProductGroupUi> {
    return Comparator<ProductGroupUi> { a, b ->
        val aSize = sizeInBaseUnits(a.product)
        val bSize = sizeInBaseUnits(b.product)
        when {
            aSize == null && bSize == null -> 0
            aSize == null -> 1
            bSize == null -> -1
            else -> aSize.compareTo(bSize)
        }
    }.thenBy { it.product.nomeProduto.lowercase(Locale.getDefault()) }
}

private fun sizeInBaseUnits(product: Product): Double? {
    val value = product.tamanhoValor ?: return null
    val unitRaw = product.tamanhoUnidade?.trim()?.lowercase(Locale.getDefault()) ?: return null
    val unit = unitRaw.replace(" ", "")
    val multiplier = when (unit) {
        "g", "gr", "grama", "gramas" -> 1.0
        "kg", "kgs", "quilo", "quilos", "kilogram", "kilograms" -> 1000.0
        "mg", "miligram", "miligrama", "miligramas" -> 0.001
        "ml", "mililitro", "mililitros" -> 1.0
        "cl" -> 10.0
        "l", "lt", "litro", "litros" -> 1000.0
        "un", "uni", "unid", "unidade", "unidades" -> 1.0
        else -> return null
    }
    return value * multiplier
}

private fun groupIdenticalProducts(products: List<Product>): List<ProductGroupUi> {
    return products
        .map { product ->
            ProductGroupUi(
                product = product,
                quantity = 1,
                productIds = listOf(product.id)
            )
        }
        .sortedWith(
            compareBy<ProductGroupUi> { it.product.nomeProduto.lowercase() }
                .thenBy { it.product.marca.orEmpty().lowercase() }
        )
}
