package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import java.util.Date

class CampaignCreateViewModel : ViewModel() {
    var nome = mutableStateOf("")
    var desc = mutableStateOf("")
    var dataInicio = mutableStateOf<Date?>(null)
    var dataFim = mutableStateOf<Date?>(null)
    var isLoading = mutableStateOf(false)

    private val repo = CampaignRepository()

    fun save(onSuccess: () -> Unit) {
        if (nome.value.isEmpty() || dataInicio.value == null || dataFim.value == null) return

        isLoading.value = true
        val campaign = Campaign(
            nomeCampanha = nome.value,
            descCampanha = desc.value,
            dataInicio = dataInicio.value!!,
            dataFim = dataFim.value!!,
            tipo = "Interna" // Default
        )

        repo.addCampaign(campaign,
            onSuccess = { isLoading.value = false; onSuccess() },
            onError = { isLoading.value = false }
        )
    }
}