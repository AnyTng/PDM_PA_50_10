package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.campaigns.CampaignStats
import ipca.app.lojasas.data.products.Product
import javax.inject.Inject

enum class CampaignResultsMessageDuration {
    Short,
    Long
}

data class CampaignResultsMessage(
    val text: String,
    val duration: CampaignResultsMessageDuration = CampaignResultsMessageDuration.Short
)

data class CampaignResultsState(
    val campaignName: String = "",
    val isLoading: Boolean = true,
    val totalProducts: Int = 0,
    val categoryPercentages: Map<String, Float> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val isExportEnabled: Boolean = false,
    val exportHint: String? = null,
    val message: CampaignResultsMessage? = null
)

@HiltViewModel
class CampaignResultsViewModel @Inject constructor(
    private val repository: CampaignRepository,
    private val pdfExporter: CampaignResultsPdfExporter
) : ViewModel() {
    var uiState = mutableStateOf(CampaignResultsState())
        private set

    private var currentCampaignId: String = ""
    private var campaignName: String = ""
    private var stats: CampaignStats? = null
    private var products: List<Product> = emptyList()
    private var isLoadingStats: Boolean = false
    private var isLoadingProducts: Boolean = false
    private var productsError: String? = null
    private var requestToken: Int = 0

    fun loadCampaign(campaignId: String) {
        val normalizedId = campaignId.trim()
        if (normalizedId == currentCampaignId && !isLoadingStats && !isLoadingProducts) {
            return
        }

        currentCampaignId = normalizedId
        campaignName = ""
        stats = null
        products = emptyList()
        productsError = null
        isLoadingStats = true
        isLoadingProducts = true
        requestToken += 1
        val token = requestToken
        updateUiState()

        if (normalizedId.isBlank()) {
            stats = CampaignStats(0, emptyMap(), emptyMap())
            isLoadingStats = false
            isLoadingProducts = false
            updateUiState()
            return
        }

        repository.getCampaignById(
            campaignId = normalizedId,
            onSuccess = { campaign ->
                if (token != requestToken) return@getCampaignById
                campaignName = campaign?.nomeCampanha?.trim().orEmpty()
                updateUiState()
            },
            onError = { _ -> }
        )

        repository.getCampaignStats(normalizedId) { result ->
            if (token != requestToken) return@getCampaignStats
            stats = result
            isLoadingStats = false
            updateUiState()
        }

        repository.getCampaignProducts(
            campaignId = normalizedId,
            onSuccess = { list ->
                if (token != requestToken) return@getCampaignProducts
                products = list
                isLoadingProducts = false
                updateUiState()
            },
            onError = { error ->
                if (token != requestToken) return@getCampaignProducts
                productsError = error.message ?: "Erro ao carregar produtos."
                isLoadingProducts = false
                updateUiState()
            }
        )
    }

    fun exportPdf() {
        val currentStats = stats
        when {
            currentStats == null -> {
                pushMessage("Dados indisponiveis.")
                return
            }
            productsError != null -> {
                pushMessage("Erro ao carregar produtos.")
                return
            }
            isLoadingProducts -> {
                pushMessage("A carregar dados para exportacao...")
                return
            }
        }

        val displayName = campaignName.takeIf { it.isNotBlank() } ?: currentCampaignId
        val result = pdfExporter.export(
            campaignName = displayName.ifBlank { "campanha" },
            stats = currentStats,
            products = products
        )

        when (result) {
            is CampaignResultsPdfResult.Success -> {
                pushMessage(
                    text = "Guardado em Downloads: ${result.fileName}",
                    duration = CampaignResultsMessageDuration.Long
                )
            }
            is CampaignResultsPdfResult.Error -> {
                pushMessage(result.message)
            }
        }
    }

    fun consumeMessage() {
        if (uiState.value.message != null) {
            uiState.value = uiState.value.copy(message = null)
        }
    }

    private fun updateUiState() {
        val displayName = campaignName.takeIf { it.isNotBlank() } ?: currentCampaignId
        val resolvedName = displayName.ifBlank { "Campanha" }
        val currentStats = stats
        uiState.value = uiState.value.copy(
            campaignName = resolvedName,
            isLoading = isLoadingStats || currentStats == null,
            totalProducts = currentStats?.totalProducts ?: 0,
            categoryPercentages = currentStats?.categoryPercentages ?: emptyMap(),
            categoryCounts = currentStats?.categoryCounts ?: emptyMap(),
            isExportEnabled = currentStats != null && !isLoadingProducts && productsError == null,
            exportHint = when {
                isLoadingProducts -> "A carregar dados para exportacao..."
                productsError != null -> "Erro ao carregar dados para exportacao."
                else -> null
            }
        )
    }

    private fun pushMessage(
        text: String,
        duration: CampaignResultsMessageDuration = CampaignResultsMessageDuration.Short
    ) {
        uiState.value = uiState.value.copy(message = CampaignResultsMessage(text, duration))
    }
}
