package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.cestas.CestasRepository
import ipca.app.lojasas.data.products.Product
import javax.inject.Inject

data class EditCestaProdutosState(
    val isLoadingCesta: Boolean = true,
    val isLoadingDisponiveis: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val cestaId: String = "",
    val selectedProdutoIds: List<String> = emptyList(),
    val originalProdutoIds: List<String> = emptyList(),
    val produtosSelecionados: List<Product> = emptyList(),
    val produtosOriginais: List<Product> = emptyList(),
    val missingProdutoIds: List<String> = emptyList(),
    val produtosDisponiveis: List<Product> = emptyList()
)

@HiltViewModel
class EditCestaProdutosViewModel @Inject constructor(
    private val repository: CestasRepository
) : ViewModel() {

    var uiState = mutableStateOf(EditCestaProdutosState())
        private set

    private var produtosListener: ListenerHandle? = null
    private var produtosRequestToken: Int = 0

    fun start(cestaId: String) {
        val normalized = cestaId.trim()
        if (normalized.isBlank()) {
            uiState.value = EditCestaProdutosState(
                isLoadingCesta = false,
                isLoadingDisponiveis = false,
                error = "Cesta invalida."
            )
            return
        }

        uiState.value = EditCestaProdutosState(
            isLoadingCesta = true,
            isLoadingDisponiveis = true,
            cestaId = normalized
        )

        listenProdutosDisponiveis()
        loadCestaProdutos(normalized)
    }

    override fun onCleared() {
        super.onCleared()
        produtosListener?.remove()
    }

    private fun listenProdutosDisponiveis() {
        produtosListener?.remove()
        produtosListener = repository.listenProdutosDisponiveis(
            onSuccess = { produtos ->
                uiState.value = uiState.value.copy(
                    isLoadingDisponiveis = false,
                    produtosDisponiveis = produtos
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoadingDisponiveis = false,
                    error = e.message ?: "Erro ao carregar produtos."
                )
            }
        )
    }

    private fun loadCestaProdutos(cestaId: String) {
        uiState.value = uiState.value.copy(isLoadingCesta = true, error = null)
        repository.fetchCestaProdutoIds(
            cestaId = cestaId,
            onSuccess = { ids ->
                val normalizedIds = ids
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (normalizedIds.isEmpty()) {
                    uiState.value = uiState.value.copy(
                        isLoadingCesta = false,
                        selectedProdutoIds = emptyList(),
                        originalProdutoIds = emptyList(),
                        produtosSelecionados = emptyList(),
                        produtosOriginais = emptyList(),
                        missingProdutoIds = emptyList()
                    )
                    return@fetchCestaProdutoIds
                }

                fetchProdutos(normalizedIds)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoadingCesta = false,
                    error = e.message ?: "Erro ao carregar cesta."
                )
            }
        )
    }

    private fun fetchProdutos(produtoIds: List<String>) {
        val requestToken = ++produtosRequestToken
        repository.fetchProdutosByIds(produtoIds) { products, missingIds, errorMessage ->
            if (requestToken != produtosRequestToken) return@fetchProdutosByIds
            uiState.value = uiState.value.copy(
                isLoadingCesta = false,
                selectedProdutoIds = produtoIds,
                originalProdutoIds = produtoIds,
                produtosSelecionados = products,
                produtosOriginais = products,
                missingProdutoIds = missingIds,
                error = errorMessage
            )
        }
    }

    fun addProduto(prod: Product) {
        val current = uiState.value
        if (current.selectedProdutoIds.contains(prod.id)) return
        uiState.value = current.copy(
            selectedProdutoIds = current.selectedProdutoIds + prod.id,
            produtosSelecionados = current.produtosSelecionados + prod,
            missingProdutoIds = current.missingProdutoIds.filterNot { it == prod.id },
            error = null
        )
    }

    fun removeProduto(productId: String) {
        val current = uiState.value
        uiState.value = current.copy(
            selectedProdutoIds = current.selectedProdutoIds.filterNot { it == productId },
            produtosSelecionados = current.produtosSelecionados.filterNot { it.id == productId },
            missingProdutoIds = current.missingProdutoIds.filterNot { it == productId },
            error = null
        )
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.isSaving) return
        if (state.selectedProdutoIds.isEmpty()) {
            uiState.value = state.copy(error = "Selecione pelo menos 1 produto.")
            return
        }
        if (state.selectedProdutoIds == state.originalProdutoIds) {
            onSuccess()
            return
        }

        uiState.value = state.copy(isSaving = true, error = null)
        repository.updateCestaProdutos(
            cestaId = state.cestaId,
            novoProdutoIds = state.selectedProdutoIds,
            onSuccess = {
                uiState.value = uiState.value.copy(
                    isSaving = false,
                    originalProdutoIds = state.selectedProdutoIds
                )
                onSuccess()
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Erro ao guardar produtos."
                )
            }
        )
    }
}
