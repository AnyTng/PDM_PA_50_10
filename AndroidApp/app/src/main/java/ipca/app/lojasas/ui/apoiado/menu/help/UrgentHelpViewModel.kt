package ipca.app.lojasas.ui.apoiado.menu.help


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.requests.UrgentRequestsRepository
import javax.inject.Inject

@HiltViewModel
class UrgentHelpViewModel @Inject constructor(
    private val repository: UrgentRequestsRepository
) : ViewModel() {

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var success = mutableStateOf(false)

    // Função para submeter o pedido
    fun submitUrgentRequest(numeroMecanografico: String, descricao: String, onSuccess: () -> Unit) {
        if (descricao.isBlank()) {
            error.value = "A descrição não pode estar vazia."
            return
        }

        isLoading.value = true

        repository.submitUrgentRequest(
            numeroMecanografico = numeroMecanografico,
            descricao = descricao,
            onSuccess = {
                isLoading.value = false
                success.value = true
                onSuccess()
            },
            onError = { e ->
                isLoading.value = false
                error.value = "Erro ao enviar: ${e.message}"
            }
        )
    }
}
