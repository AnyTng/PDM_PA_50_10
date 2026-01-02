package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Locale

data class ProductDetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val subCategoria: String = "",
    val searchQuery: String = "",
    val groups: List<ProductGroupUi> = emptyList(),
    val availableBrands: List<String> = emptyList(),
    val selectedBrand: String = BRAND_ALL,
    val availableCampaigns: List<CampaignFilterOption> = emptyList(),
    val selectedCampaign: String = CAMPAIGN_ALL,
    val sortOption: ProductSortOption = ProductSortOption.EXPIRY_ASC
)

data class ProductGroupUi(
    val product: Product,
    val quantity: Int,
    val productIds: List<String>
)

data class CampaignFilterOption(
    val id: String,
    val label: String
)

enum class ProductSortOption {
    EXPIRY_ASC,
    EXPIRY_DESC,
    SIZE_ASC,
    SIZE_DESC
}

const val BRAND_ALL = "Todas as marcas"
const val CAMPAIGN_ALL = "__ALL__"
const val CAMPAIGN_NONE = "__NONE__"

class ProductDetailsViewModel(
    private val repository: ProductsRepository = ProductsRepository(),
    private val campaignRepository: CampaignRepository = CampaignRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductDetailsUiState())
    val uiState: State<ProductDetailsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allGroups: List<ProductGroupUi> = emptyList()
    private var currentSubCategoria: String? = null
    private var campaignsById: Map<String, Campaign> = emptyMap()

    init {
        campaignRepository.listenCampaigns(
            onSuccess = { campaigns ->
                campaignsById = campaigns.associateBy { it.id }
                updateAvailableFilters()
            },
            onError = { }
        )
    }

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
                updateAvailableFilters()
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

    fun onBrandSelected(value: String) {
        _uiState.value = _uiState.value.copy(selectedBrand = value)
        applyFilter()
    }

    fun onCampaignSelected(value: String) {
        _uiState.value = _uiState.value.copy(selectedCampaign = value)
        applyFilter()
    }

    fun onSortSelected(option: ProductSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilter()
    }

    private fun updateAvailableFilters() {
        if (allGroups.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                availableBrands = listOf(BRAND_ALL),
                selectedBrand = BRAND_ALL,
                availableCampaigns = listOf(CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")),
                selectedCampaign = CAMPAIGN_ALL
            )
            return
        }

        val brands = allGroups
            .mapNotNull { it.product.marca?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val campaignIds = allGroups
            .mapNotNull { it.product.campanha?.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val campaignOptions = mutableListOf(
            CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")
        )
        if (allGroups.any { it.product.campanha.isNullOrBlank() }) {
            campaignOptions.add(CampaignFilterOption(CAMPAIGN_NONE, "Sem campanha"))
        }
        campaignIds
            .map { id ->
                val label = campaignsById[id]?.nomeCampanha?.trim().takeUnless { it.isNullOrBlank() } ?: id
                CampaignFilterOption(id, label)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .forEach { campaignOptions.add(it) }

        val selectedBrand = _uiState.value.selectedBrand
        val resolvedBrand = if (selectedBrand != BRAND_ALL && !brands.contains(selectedBrand)) {
            BRAND_ALL
        } else {
            selectedBrand
        }

        val selectedCampaign = _uiState.value.selectedCampaign
        val resolvedCampaign = if (selectedCampaign != CAMPAIGN_ALL &&
            !campaignOptions.any { it.id == selectedCampaign }) {
            CAMPAIGN_ALL
        } else {
            selectedCampaign
        }

        _uiState.value = _uiState.value.copy(
            availableBrands = listOf(BRAND_ALL) + brands,
            selectedBrand = resolvedBrand,
            availableCampaigns = campaignOptions,
            selectedCampaign = resolvedCampaign
        )
    }

    private fun applyFilter() {
        val state = _uiState.value
        val q = state.searchQuery.trim()

        var filtered = allGroups
        if (state.selectedBrand != BRAND_ALL) {
            filtered = filtered.filter {
                it.product.marca?.trim().equals(state.selectedBrand, ignoreCase = true)
            }
        }
        when (state.selectedCampaign) {
            CAMPAIGN_NONE -> {
                filtered = filtered.filter { it.product.campanha.isNullOrBlank() }
            }
            CAMPAIGN_ALL -> Unit
            else -> {
                filtered = filtered.filter { it.product.campanha?.trim() == state.selectedCampaign }
            }
        }

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
