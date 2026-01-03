package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Date
import java.util.Locale

data class ExpiredProductsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val groups: List<ProductGroupUi> = emptyList(),
    val sortOption: ProductSortOption = ProductSortOption.EXPIRY_ASC
)

class ExpiredProductsViewModel(
    private val repository: ProductsRepository = ProductsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ExpiredProductsUiState())
    val uiState: State<ExpiredProductsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allGroups: List<ProductGroupUi> = emptyList()

    init {
        listener = repository.listenAllProducts(
            onSuccess = { products ->
                val reference = Date()
                val expired = products.filter { product ->
                    product.isExpiredVisible(reference) && product.doado.isNullOrBlank()
                }
                allGroups = groupIdenticalProducts(expired)
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

    fun onSortSelected(option: ProductSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilter()
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

    override fun onCleared() {
        listener?.remove()
        listener = null
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
        .groupBy { it.identity() }
        .values
        .map { groupProducts ->
            val representative = groupProducts.first()
            ProductGroupUi(
                product = representative,
                quantity = groupProducts.size,
                productIds = groupProducts.map { it.id }
            )
        }
        .sortedWith(
            compareBy<ProductGroupUi> { it.product.nomeProduto.lowercase() }
                .thenBy { it.product.marca.orEmpty().lowercase() }
        )
}
