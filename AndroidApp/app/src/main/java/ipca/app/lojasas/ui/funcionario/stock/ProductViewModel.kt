package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import javax.inject.Inject

data class ProductViewUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val product: Product? = null,
    val quantityInGroup: Int = 1,
    val isDeleting: Boolean = false,
    val isUpdatingBarcode: Boolean = false
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductsRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductViewUiState())
    val uiState: State<ProductViewUiState> = _uiState

    private var listener: ListenerHandle? = null
    private var subCategoryListener: ListenerHandle? = null
    private var currentProductId: String? = null
    private var currentIdentity: ProductIdentity? = null
    private var currentSubCategoria: String? = null

    fun observeProduct(productId: String) {
        val normalized = productId.trim()
        if (normalized.isBlank()) return
        if (currentProductId == normalized) return
        currentProductId = normalized

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            quantityInGroup = 1
        )
        listener?.remove()
        listener = repository.listenProduct(
            productId = normalized,
            onSuccess = { product ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    product = product
                )
                if (product != null) observeDuplicateCount(product)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar produto.",
                    product = null
                )
            }
        )
    }

    fun updateBarcode(
        codBarras: String
    ) {
        val id = currentProductId ?: return
        val normalized = codBarras.trim()
        if (normalized.isBlank()) return

        _uiState.value = _uiState.value.copy(isUpdatingBarcode = true, error = null)
        repository.updateBarcode(
            productId = id,
            codBarras = normalized,
            onSuccess = { _uiState.value = _uiState.value.copy(isUpdatingBarcode = false) },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isUpdatingBarcode = false,
                    error = e.message ?: "Erro ao atualizar código de barras."
                )
            }
        )
    }

    fun deleteCurrentProduct(
        onSuccess: () -> Unit
    ) {
        val id = currentProductId ?: return
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
        repository.deleteProduct(
            productId = id,
            onSuccess = {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                onSuccess()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = e.message ?: "Erro ao apagar produto."
                )
            }
        )
    }

    private fun observeDuplicateCount(product: Product) {
        val identity = product.identity()
        if (currentSubCategoria == product.subCategoria && currentIdentity == identity) return
        currentSubCategoria = product.subCategoria
        currentIdentity = identity

        subCategoryListener?.remove()
        subCategoryListener = repository.listenProductsBySubCategoria(
            subCategoria = product.subCategoria,
            onSuccess = { products ->
                val count = products.count { it.identity() == identity }.coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(quantityInGroup = count)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao calcular quantidade."
                )
            }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        subCategoryListener?.remove()
        subCategoryListener = null
        super.onCleared()
    }
}
