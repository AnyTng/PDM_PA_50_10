package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Locale
import javax.inject.Inject

data class ProductViewUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val product: Product? = null,
    val quantityInGroup: Int = 1,
    val isDeleting: Boolean = false,
    val isUpdatingBarcode: Boolean = false,
    val campaignLabel: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductsRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductViewUiState())
    val uiState: State<ProductViewUiState> = _uiState

    private var listener: ListenerHandle? = null
    private var subCategoryListener: ListenerHandle? = null
    private var currentProductId: String? = null
    private var currentIdentity: ProductIdentity? = null
    private var currentSubCategoria: String? = null
    private var currentCampaignId: String? = null
    private var campaignLabelById: Map<String, String> = emptyMap()
    private var campaignLabelByName: Map<String, String> = emptyMap()

    init {
        campaignRepository.listenCampaigns(
            onSuccess = { campaigns ->
                val byId = mutableMapOf<String, String>()
                val byName = mutableMapOf<String, String>()
                campaigns.forEach { campaign ->
                    val label = campaign.nomeCampanha.trim()
                    if (label.isNotBlank()) {
                        byId[campaign.id] = label
                        byName[label.lowercase(Locale.getDefault())] = label
                    }
                }
                campaignLabelById = byId
                campaignLabelByName = byName
                updateCampaignLabelFromCache()
            },
            onError = { }
        )
    }

    fun observeProduct(productId: String) {
        val normalized = productId.trim()
        if (normalized.isBlank()) return
        if (currentProductId == normalized) return
        currentProductId = normalized
        currentCampaignId = null

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            quantityInGroup = 1,
            campaignLabel = null
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
                if (product != null) {
                    observeDuplicateCount(product)
                    resolveCampaignLabel(product.campanha)
                } else {
                    _uiState.value = _uiState.value.copy(campaignLabel = null)
                }
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

    private fun resolveCampaignLabel(campaignId: String?) {
        val normalized = campaignId?.trim().orEmpty()
        if (normalized.isBlank()) {
            currentCampaignId = null
            _uiState.value = _uiState.value.copy(campaignLabel = null)
            return
        }
        if (normalized == currentCampaignId && _uiState.value.campaignLabel != null) return
        currentCampaignId = normalized

        val cachedLabel = resolveCampaignLabelFromCache(normalized)
        if (!cachedLabel.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(campaignLabel = cachedLabel)
            return
        }

        campaignRepository.getCampaignByIdOrName(
            campaignValue = normalized,
            onSuccess = { campaign ->
                if (currentCampaignId == normalized) {
                    val name = campaign?.nomeCampanha?.trim()
                    val label = if (name.isNullOrBlank()) normalized else name
                    _uiState.value = _uiState.value.copy(campaignLabel = label)
                }
            },
            onError = {
                if (currentCampaignId == normalized) {
                    _uiState.value = _uiState.value.copy(campaignLabel = normalized)
                }
            }
        )
    }

    private fun updateCampaignLabelFromCache() {
        val currentValue = currentCampaignId?.trim().orEmpty()
        if (currentValue.isBlank()) return
        val label = resolveCampaignLabelFromCache(currentValue) ?: return
        _uiState.value = _uiState.value.copy(campaignLabel = label)
    }

    private fun resolveCampaignLabelFromCache(value: String): String? {
        val byId = campaignLabelById[value]?.trim()
        if (!byId.isNullOrBlank()) return byId

        val byName = campaignLabelByName[value.lowercase(Locale.getDefault())]?.trim()
        if (!byName.isNullOrBlank()) return byName

        return null
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
