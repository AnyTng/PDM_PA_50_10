package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.cestas.ApoiadoOption
import ipca.app.lojasas.data.cestas.CestasRepository
import ipca.app.lojasas.data.products.Product
import java.util.Date
import javax.inject.Inject

data class CreateCestaState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,

    val funcionarioId: String = "",

    // Contexto (manual vs vindo de pedido urgente)
    val fromUrgent: Boolean = false,
    val pedidoId: String? = null,
    val pedidoDescricao: String? = null,

    // Beneficiário
    val apoiadoSelecionado: ApoiadoOption? = null,
    val apoiados: List<ApoiadoOption> = emptyList(),

    // Produtos
    val produtos: List<Product> = emptyList(),
    val produtosSelecionados: List<Product> = emptyList(),

    // Agendamento
    val usarAgora: Boolean = true,
    val dataAgendada: Date? = null,

    // Recorrência
    val recorrente: Boolean = false,
    val recorrenciaDias: String = "",

    // Observações
    val obs: String = ""
)

@HiltViewModel
class CreateCestaViewModel @Inject constructor(
    private val repository: CestasRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private companion object {
        const val RECORRENCIA_DIAS_DEFAULT = 30
    }

    var uiState = mutableStateOf(CreateCestaState())
        private set

    private var initialized = false

    private var produtosListener: ListenerHandle? = null
    private var apoiadosListener: ListenerHandle? = null

    override fun onCleared() {
        super.onCleared()
        produtosListener?.remove()
        apoiadosListener?.remove()
    }

    /**
     * Chamar uma vez no Composable (via LaunchedEffect) para configurar o ecrã.
     */
    fun start(fromUrgent: Boolean, pedidoId: String?, apoiadoId: String?) {
        if (initialized) return
        initialized = true

        uiState.value = uiState.value.copy(
            isLoading = true,
            fromUrgent = fromUrgent,
            pedidoId = pedidoId
        )

        loadFuncionarioId()
        listenProdutosDisponiveis()
        listenApoiados()

        if (fromUrgent) {
            // Força "única" quando vem de pedido urgente
            uiState.value = uiState.value.copy(recorrente = false, recorrenciaDias = "")
            if (!pedidoId.isNullOrBlank()) loadPedido(pedidoId)
            if (!apoiadoId.isNullOrBlank()) loadApoiado(apoiadoId)
        }
    }

    private fun loadFuncionarioId() {
        val uid = authRepository.currentUserId()
        if (uid.isNullOrBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            return
        }

        repository.fetchFuncionarioIdByUid(
            uid = uid,
            onSuccess = { id ->
                uiState.value = uiState.value.copy(funcionarioId = id.orEmpty())
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
        )
    }

    private fun listenProdutosDisponiveis() {
        produtosListener?.remove()
        produtosListener = repository.listenProdutosDisponiveis(
            onSuccess = { produtos ->
                uiState.value = uiState.value.copy(isLoading = false, produtos = produtos)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
        )
    }

    private fun listenApoiados() {
        apoiadosListener?.remove()
        apoiadosListener = repository.listenApoiados(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(apoiados = list)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
        )
    }

    private fun loadPedido(pedidoId: String) {
        repository.fetchPedidoDescricao(
            pedidoId = pedidoId,
            onSuccess = { desc ->
                uiState.value = uiState.value.copy(
                    pedidoDescricao = desc,
                    // Prefill da nota com a descrição (pode ser editada)
                    obs = uiState.value.obs.ifBlank { desc.orEmpty() }
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
        )
    }

    private fun loadApoiado(apoiadoId: String) {
        repository.fetchApoiadoOption(
            apoiadoId = apoiadoId,
            onSuccess = { option ->
                if (option != null) {
                    uiState.value = uiState.value.copy(apoiadoSelecionado = option)
                }
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
        )
    }

    fun selecionarApoiado(option: ApoiadoOption) {
        uiState.value = uiState.value.copy(apoiadoSelecionado = option)
    }

    fun setObs(value: String) {
        uiState.value = uiState.value.copy(obs = value)
    }

    fun setUsarAgora(value: Boolean) {
        uiState.value = uiState.value.copy(
            usarAgora = value,
            dataAgendada = if (value) Date() else uiState.value.dataAgendada
        )
    }

    fun setDataAgendada(date: Date) {
        uiState.value = uiState.value.copy(usarAgora = false, dataAgendada = date)
    }

    fun setRecorrente(value: Boolean) {
        // Quando vem de pedido urgente é sempre única
        if (uiState.value.fromUrgent) return

        uiState.value = uiState.value.copy(
            recorrente = value,
            recorrenciaDias = if (value) RECORRENCIA_DIAS_DEFAULT.toString() else ""
        )
    }

    fun setRecorrenciaDias(value: String) {
        uiState.value = uiState.value.copy(recorrenciaDias = value)
    }

    fun addProduto(prod: Product) {
        val current = uiState.value
        if (current.produtosSelecionados.any { it.id == prod.id }) return
        uiState.value = current.copy(produtosSelecionados = current.produtosSelecionados + prod)
    }

    fun removeProduto(productId: String) {
        val current = uiState.value
        uiState.value = current.copy(produtosSelecionados = current.produtosSelecionados.filterNot { it.id == productId })
    }

    fun submitCesta(onSuccess: () -> Unit) {
        val s = uiState.value

        val funcId = s.funcionarioId.trim()
        if (funcId.isBlank()) {
            uiState.value = s.copy(error = "Não foi possível identificar o funcionário.")
            return
        }

        val apoiado = s.apoiadoSelecionado
        if (apoiado == null) {
            uiState.value = s.copy(error = "Selecione um beneficiário.")
            return
        }

        if (s.produtosSelecionados.isEmpty()) {
            uiState.value = s.copy(error = "Selecione pelo menos 1 produto.")
            return
        }

        val agendada = if (s.usarAgora) Date() else s.dataAgendada
        if (agendada == null) {
            uiState.value = s.copy(error = "Selecione a data de entrega.")
            return
        }

        val recorrente = if (s.fromUrgent) false else s.recorrente
        val recDias: Int? = if (recorrente) RECORRENCIA_DIAS_DEFAULT else null

        uiState.value = s.copy(isSubmitting = true, error = null)

        val produtoIds = s.produtosSelecionados.map { it.id }
        repository.createCesta(
            funcionarioId = funcId,
            apoiadoId = apoiado.id,
            produtoIds = produtoIds,
            dataAgendada = agendada,
            observacoes = s.obs,
            fromUrgent = s.fromUrgent,
            pedidoId = s.pedidoId,
            recorrente = recorrente,
            recorrenciaDias = recDias,
            onSuccess = {
                uiState.value = uiState.value.copy(isSubmitting = false)
                onSuccess()
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isSubmitting = false, error = e.message)
            }
        )
    }
}
