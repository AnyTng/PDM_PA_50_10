package ipca.app.lojasas.ui.funcionario.stock

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import ipca.app.lojasas.ui.funcionario.stock.components.StockGroupUi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale

data class ProductsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val groups: List<StockGroupUi> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val selectedCategory: String = CATEGORY_ALL,
    val sortOption: StockSortOption = StockSortOption.NAME_ASC
)

enum class StockSortOption {
    NAME_ASC,
    NAME_DESC,
    QTY_ASC,
    QTY_DESC
}

const val CATEGORY_ALL = "Todas as categorias"

class ProductsViewModel(
    private val repository: ProductsRepository = ProductsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductsUiState())
    val uiState: State<ProductsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allProducts: List<Product> = emptyList()

    init {
        listener = repository.listenAllProducts(
            onSuccess = { products ->
                allProducts = products
                val categories = products
                    .mapNotNull { it.categoria?.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                val selected = _uiState.value.selectedCategory
                val resolvedCategory = if (selected != CATEGORY_ALL && !categories.contains(selected)) {
                    CATEGORY_ALL
                } else {
                    selected
                }
                _uiState.value = _uiState.value.copy(
                    availableCategories = categories,
                    selectedCategory = resolvedCategory
                )
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
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
        applyFilter()
    }

    fun onCategorySelected(value: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = value)
        applyFilter()
    }

    fun onSortSelected(option: StockSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilter()
    }

    fun exportToCSV(context: Context) {
        val data = allProducts
        if (data.isEmpty()) {
            Toast.makeText(context, "Sem dados para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val header = listOf(
            "ID",
            "Nome",
            "Categoria",
            "SubCategoria",
            "Marca",
            "Campanha",
            "Doado",
            "CodigoBarras",
            "Validade",
            "TamanhoValor",
            "TamanhoUnidade",
            "Descricao",
            "Estado",
            "ParceiroExternoNome",
            "IdFunc"
        ).joinToString(",")

        val sorted = data.sortedWith(
            compareBy<Product> { it.nomeProduto.lowercase(Locale.getDefault()) }
                .thenBy { it.id }
        )
        val body = sorted.joinToString("\n") { product ->
            listOf(
                product.id,
                product.nomeProduto,
                product.categoria.orEmpty(),
                product.subCategoria,
                product.marca.orEmpty(),
                product.campanha.orEmpty(),
                product.doado.orEmpty(),
                product.codBarras.orEmpty(),
                product.validade?.let { dateFormatter.format(it) }.orEmpty(),
                product.tamanhoValor?.toString().orEmpty(),
                product.tamanhoUnidade.orEmpty(),
                product.descProduto.orEmpty(),
                product.estadoProduto.orEmpty(),
                product.parceiroExternoNome.orEmpty(),
                product.idFunc.orEmpty()
            ).joinToString(",") { csvValue(it) }
        }
        val csvContent = "$header\n$body"

        try {
            val fileName = "produtos_export_${System.currentTimeMillis()}.csv"
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

    private fun applyFilter() {
        val state = _uiState.value
        val q = state.searchQuery.trim()
        val category = state.selectedCategory

        val byCategory = if (category == CATEGORY_ALL) {
            allProducts
        } else {
            allProducts.filter { it.categoria?.trim().equals(category, ignoreCase = true) }
        }

        val groups = productsToGroups(byCategory)
        val searched = if (q.isBlank()) {
            groups
        } else {
            groups.filter { it.name.contains(q, ignoreCase = true) }
        }
        val sorted = when (state.sortOption) {
            StockSortOption.NAME_ASC -> searched.sortedBy { it.name.lowercase() }
            StockSortOption.NAME_DESC -> searched.sortedByDescending { it.name.lowercase() }
            StockSortOption.QTY_ASC -> searched.sortedWith(
                compareBy<StockGroupUi> { it.availableCount }
                    .thenBy { it.name.lowercase() }
            )
            StockSortOption.QTY_DESC -> searched.sortedWith(
                compareByDescending<StockGroupUi> { it.availableCount }
                    .thenBy { it.name.lowercase() }
            )
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            groups = sorted
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}

private fun csvValue(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    val needsQuotes = escaped.contains(',') || escaped.contains('"') ||
        escaped.contains('\n') || escaped.contains('\r')
    return if (needsQuotes) "\"$escaped\"" else escaped
}

private fun productsToGroups(products: List<Product>): List<StockGroupUi> {
    return products
        .groupBy { it.nomeProduto.trim() } // Agrupa pelo nomeProduto (ex: Arroz)
        .map { (nomeProduto, groupProducts) ->
            StockGroupUi(
                name = nomeProduto.ifBlank { "—" },
                availableCount = groupProducts.count { it.isAvailable() }
            )
        }
}

private fun Product.isAvailable(): Boolean {
    val status = estadoProduto?.trim()?.lowercase()
    if (status.isNullOrBlank()) return true
    return status.startsWith("dispon")
}
