package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import ipca.app.lojasas.ui.funcionario.stock.components.StockGroupUi

data class ProductsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val groups: List<StockGroupUi> = emptyList()
)

class ProductsViewModel(
    private val repository: ProductsRepository = ProductsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductsUiState())
    val uiState: State<ProductsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allGroups: List<StockGroupUi> = emptyList()

    init {
        listener = repository.listenAllProducts(
            onSuccess = { products ->
                allGroups = productsToGroups(products)
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

    private fun applyFilter() {
        val q = _uiState.value.searchQuery.trim()
        val filtered = if (q.isBlank()) {
            allGroups
        } else {
            allGroups.filter { it.name.contains(q, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            groups = filtered
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}

private fun productsToGroups(products: List<Product>): List<StockGroupUi> {
    return products
        .groupBy { it.subCategoria.trim() }
        .map { (subCategoria, groupProducts) ->
            StockGroupUi(
                name = subCategoria.ifBlank { "—" },
                availableCount = groupProducts.count { it.isAvailable() }
            )
        }
        .sortedBy { it.name.lowercase() }
}

private fun Product.isAvailable(): Boolean {
    val status = estadoProduto?.trim()?.lowercase()
    if (status.isNullOrBlank()) return true
    return status.startsWith("dispon")
}

