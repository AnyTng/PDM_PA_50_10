package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import java.util.Calendar
import java.util.Date

data class CampaignsState(
    val activeCampaigns: List<Campaign> = emptyList(),
    val inactiveCampaigns: List<Campaign> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class CampaignsViewModel : ViewModel() {
    var uiState = mutableStateOf(CampaignsState())
        private set

    private val repo = CampaignRepository()

    init {
        loadCampaigns()
    }

    private fun loadCampaigns() {
        repo.listenCampaigns(
            onSuccess = { all ->
                // Regra: Ativa se terminou há menos de 3 meses (ou no futuro)
                val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.time

                val (active, inactive) = all.partition { it.dataFim.after(threeMonthsAgo) }

                uiState.value = uiState.value.copy(
                    activeCampaigns = active,
                    inactiveCampaigns = inactive,
                    isLoading = false
                )
            },
            onError = { uiState.value = uiState.value.copy(error = it, isLoading = false) }
        )
    }

    fun updateCampaign(campaign: Campaign, onSuccess: () -> Unit) {
        repo.updateCampaign(campaign, onSuccess) { /* Handle error */ }
    }
}