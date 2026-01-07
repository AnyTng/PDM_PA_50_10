package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.cestas.ApoiadoInfo
import ipca.app.lojasas.data.cestas.CestaDetails
import ipca.app.lojasas.data.cestas.CestasRepository
import ipca.app.lojasas.data.products.Product
import javax.inject.Inject

data class CestaDetailsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cesta: CestaDetails? = null,
    val apoiado: ApoiadoInfo? = null,
    val apoiadoError: String? = null,
    val isLoadingProdutos: Boolean = false,
    val produtos: List<Product> = emptyList(),
    val produtosError: String? = null,
    val produtosMissingIds: List<String> = emptyList()
)

@HiltViewModel
class CestaDetailsViewModel @Inject constructor(
    private val repository: CestasRepository
) : ViewModel() {

    private var cestaListener: ListenerHandle? = null
    private var apoiadoListener: ListenerHandle? = null
    private var currentCestaId: String? = null
    private var currentApoiadoId: String? = null
    private var currentProdutoIds: List<String> = emptyList()
    private var produtosRequestToken: Int = 0

    var uiState = mutableStateOf(CestaDetailsState())
        private set

    fun observeCesta(cestaId: String) {
        val normalized = cestaId.trim()
        if (normalized.isBlank()) {
            uiState.value = CestaDetailsState(isLoading = false, error = "Cesta invalida.")
            return
        }
        if (currentCestaId == normalized) return

        currentCestaId = normalized
        uiState.value = uiState.value.copy(
            isLoading = true,
            error = null,
            cesta = null,
            apoiado = null,
            apoiadoError = null,
            isLoadingProdutos = false,
            produtos = emptyList(),
            produtosError = null,
            produtosMissingIds = emptyList()
        )

        cestaListener?.remove()
        cestaListener = repository.listenCestaDetails(
            cestaId = normalized,
            onSuccess = { cesta ->
                if (cesta == null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Cesta nao encontrada."
                    )
                    return@listenCestaDetails
                }

                uiState.value = uiState.value.copy(isLoading = false, error = null, cesta = cesta)

                fetchProdutos(cesta.produtoIds)

                val apoiadoId = cesta.apoiadoId
                if (apoiadoId.isBlank()) {
                    currentApoiadoId = null
                    apoiadoListener?.remove()
                    uiState.value = uiState.value.copy(
                        apoiado = null,
                        apoiadoError = "Sem apoiado associado."
                    )
                } else {
                    observeApoiado(apoiadoId)
                }
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar cesta."
                )
            }
        )
    }

    private fun observeApoiado(apoiadoId: String) {
        if (currentApoiadoId == apoiadoId) return
        currentApoiadoId = apoiadoId
        apoiadoListener?.remove()
        uiState.value = uiState.value.copy(apoiado = null, apoiadoError = null)

        apoiadoListener = repository.listenApoiadoInfo(
            apoiadoId = apoiadoId,
            onSuccess = { apoiado ->
                if (apoiado == null) {
                    uiState.value = uiState.value.copy(apoiadoError = "Apoiado nao encontrado.")
                    return@listenApoiadoInfo
                }
                uiState.value = uiState.value.copy(apoiado = apoiado, apoiadoError = null)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    apoiadoError = e.message ?: "Erro ao carregar apoiado."
                )
            }
        )
    }

    private fun fetchProdutos(produtoIds: List<String>) {
        val normalized = produtoIds.map { it.trim() }.filter { it.isNotBlank() }
        if (currentProdutoIds == normalized) return
        currentProdutoIds = normalized

        val requestToken = ++produtosRequestToken

        if (normalized.isEmpty()) {
            uiState.value = uiState.value.copy(
                isLoadingProdutos = false,
                produtos = emptyList(),
                produtosError = null,
                produtosMissingIds = emptyList()
            )
            return
        }

        uiState.value = uiState.value.copy(
            isLoadingProdutos = true,
            produtos = emptyList(),
            produtosError = null,
            produtosMissingIds = emptyList()
        )

        repository.fetchProdutosByIds(normalized) { products, missingIds, errorMessage ->
            if (requestToken != produtosRequestToken) return@fetchProdutosByIds
            uiState.value = uiState.value.copy(
                isLoadingProdutos = false,
                produtos = products,
                produtosError = errorMessage,
                produtosMissingIds = missingIds
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cestaListener?.remove()
        apoiadoListener?.remove()
        cestaListener = null
        apoiadoListener = null
    }
}
