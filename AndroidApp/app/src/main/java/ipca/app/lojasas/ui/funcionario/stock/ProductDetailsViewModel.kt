package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository

data class ProductDetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val subCategoria: String = "",
    val searchQuery: String = "",
    val groups: List<ProductGroupUi> = emptyList()
)

data class ProductGroupUi(
    val product: Product,
    val quantity: Int,
    val productIds: List<String>
)

class ProductDetailsViewModel(
    private val repository: ProductsRepository = ProductsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductDetailsUiState())
    val uiState: State<ProductDetailsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allGroups: List<ProductGroupUi> = emptyList()
    private var currentSubCategoria: String? = null

    fun observeSubCategoria(subCategoria: String) {
        val normalized = subCategoria.trim()
        if (normalized.isBlank()) return
        if (currentSubCategoria == normalized) return

        currentSubCategoria = normalized
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, subCategoria = normalized)

        listener?.remove()
        listener = repository.listenProductsBySubCategoria(
            subCategoria = normalized,
            onSuccess = { products ->
                allGroups = groupIdenticalProducts(products)
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
            allGroups.filter {
                it.product.nomeProduto.contains(q, ignoreCase = true) ||
                        (it.product.codBarras?.contains(q, ignoreCase = true) == true) ||
                        (it.product.marca?.contains(q, ignoreCase = true) == true)
            }
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