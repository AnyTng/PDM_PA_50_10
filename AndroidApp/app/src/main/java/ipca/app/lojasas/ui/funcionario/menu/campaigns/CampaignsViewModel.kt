package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import java.util.Date
import javax.inject.Inject

data class CampaignsState(
    val futureCampaigns: List<Campaign> = emptyList(), // Novas campanhas agendadas
    val activeCampaigns: List<Campaign> = emptyList(),
    val inactiveCampaigns: List<Campaign> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CampaignsViewModel @Inject constructor(
    private val repo: CampaignRepository
) : ViewModel() {
    var uiState = mutableStateOf(CampaignsState())
        private set

    init {
        loadCampaigns()
    }

    private fun loadCampaigns() {
        repo.listenCampaigns(
            onSuccess = { all ->
                val now = Date()

                // 1. Futuras: Data de início é depois de agora
                val future = all.filter { it.dataInicio.after(now) }

                // 2. Ativas: Já começaram (início <= agora) E ainda não acabaram (fim >= agora)
                val active = all.filter { !it.dataInicio.after(now) && it.dataFim.after(now) }

                // 3. Histórico: Já acabaram (fim < agora)
                val inactive = all.filter { it.dataFim.before(now) }

                uiState.value = uiState.value.copy(
                    futureCampaigns = future,
                    activeCampaigns = active,
                    inactiveCampaigns = inactive,
                    isLoading = false
                )
            },
            onError = { uiState.value = uiState.value.copy(error = it, isLoading = false) }
        )
    }

    fun updateCampaign(campaign: Campaign, onSuccess: () -> Unit) {
        // Validação simples para garantir consistência
        if (campaign.dataInicio.after(campaign.dataFim)) {
            // Se o utilizador puser o fim antes do início, não atualiza
            return
        }
        repo.updateCampaign(campaign, onSuccess) { /* Handle error */ }
    }

    // --- NOVA FUNÇÃO ---
    fun deleteCampaign(campaign: Campaign, onSuccess: () -> Unit, onError: (String) -> Unit) {
        repo.deleteCampaign(campaign, onSuccess, onError)
    }
}
