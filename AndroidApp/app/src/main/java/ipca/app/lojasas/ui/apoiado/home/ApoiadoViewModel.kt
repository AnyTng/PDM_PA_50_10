package ipca.app.lojasas.ui.apoiado.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.cestas.ApoiadoCesta
import ipca.app.lojasas.data.cestas.CestasRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.requests.UrgentRequest
import ipca.app.lojasas.data.requests.UrgentRequestsRepository
import ipca.app.lojasas.utils.AccountValidity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CestaProdutosUi(
    val isLoading: Boolean = false,
    val produtos: List<String> = emptyList(),
    val error: String? = null,
    val missingCount: Int = 0
)

data class ApoiadoState(
    // --- Campos Originais (Necessários para a App funcionar) ---
    val dadosIncompletos: Boolean = false,
    val faltaDocumentos: Boolean = false,
    val estadoConta: String = "",
    val motivoNegacao: String? = null,
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "",
    val nome: String = "",
    val validadeConta: String? = null,
    val validade: Date? = null,
    // True quando o formulário está a ser exigido por expiração de validade (e não por primeira submissão, etc.)
    val contaExpirada: Boolean = false,
    val contaExpiradaEm: Date? = null,
    val numeroMecanografico: String = "",
    val urgentRequests: List<UrgentRequest> = emptyList(), // Usado para o cartão Azul

    // --- NOVOS CAMPOS (Para os cartões Verdes) ---
    val cestasPendentes: List<ApoiadoCesta> = emptyList(),
    val cestasRealizadas: List<ApoiadoCesta> = emptyList(),
    val cestasNaoLevantadas: List<ApoiadoCesta> = emptyList(),
    val cestasCanceladas: List<ApoiadoCesta> = emptyList(),
    val produtosByCesta: Map<String, CestaProdutosUi> = emptyMap()
)

@HiltViewModel
class ApoiadoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val cestasRepository: CestasRepository,
    private val urgentRequestsRepository: UrgentRequestsRepository
) : ViewModel() {

    var uiState = mutableStateOf(ApoiadoState())
        private set

    // Evita múltiplos listeners duplicados quando a Home faz refresh (ex: após mudar password)
    private var cestasListener: ListenerHandle? = null
    private var urgentRequestsListener: ListenerHandle? = null

    init {
        checkStatus()
    }

    fun checkStatus() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) return

        uiState.value = uiState.value.copy(isLoading = true)

        apoiadoRepository.fetchApoiadoProfileByUid(
            uid = uid,
            onSuccess = { profile ->
                if (profile == null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Utilizador não encontrado."
                    )
                    return@fetchApoiadoProfileByUid
                }

                val isIncomplete = profile.dadosIncompletos
                val mudarPass = profile.mudarPass
                val faltaDocs = profile.faltaDocumentos
                val estado = profile.estadoConta
                val nomeUser = profile.nome
                val numMec = profile.numeroMecanografico

                val validadeDate = profile.validade
                val validadeString = validadeDate?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                }

                val isBlocked = estado.equals("Bloqueado", ignoreCase = true)
                val isExpired = validadeDate?.let { AccountValidity.isExpired(it) } ?: false

                if (isExpired && !isBlocked) {
                    if (!isIncomplete || !estado.equals("Correcao_Dados", ignoreCase = true)) {
                        apoiadoRepository.markAccountExpired(profile.docId) { e ->
                            Log.w("ApoiadoHome", "Falha ao marcar conta expirada: ${e.message}")
                        }
                    }

                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        dadosIncompletos = true,
                        faltaDocumentos = false,
                        estadoConta = "Correcao_Dados",
                        contaExpirada = true,
                        contaExpiradaEm = validadeDate,
                        showMandatoryPasswordChange = mudarPass,
                        docId = profile.docId,
                        nome = nomeUser,
                        validade = validadeDate,
                        validadeConta = validadeString,
                        numeroMecanografico = numMec
                    )
                    return@fetchApoiadoProfileByUid
                }

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    dadosIncompletos = isIncomplete,
                    faltaDocumentos = faltaDocs,
                    estadoConta = estado,
                    contaExpirada = false,
                    contaExpiradaEm = null,
                    showMandatoryPasswordChange = mudarPass,
                    docId = profile.docId,
                    nome = nomeUser,
                    validade = validadeDate,
                    validadeConta = validadeString,
                    numeroMecanografico = numMec
                )

                fetchCestas(
                    apoiadoDocId = profile.docId,
                    numeroMecanografico = numMec
                )

                if (numMec.isNotEmpty()) {
                    fetchUrgentRequests(numMec)
                }

                if (estado == "Negado") {
                    fetchDenialReason(profile.docId)
                }
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
        )
    }

    // --- NOVA FUNÇÃO: Busca as cestas para os cartões verdes ---
    private fun fetchCestas(apoiadoDocId: String, numeroMecanografico: String) {
        // Remove listener anterior (evita duplicação)
        cestasListener?.remove()

        // Na BD (prints): campo é "apoiadoID". Alguns projetos também guardam o nº mecanográfico.
        val keys = listOf(apoiadoDocId, numeroMecanografico).filter { it.isNotBlank() }.distinct()

        if (keys.isEmpty()) {
            uiState.value = uiState.value.copy(
                cestasPendentes = emptyList(),
                cestasRealizadas = emptyList(),
                cestasNaoLevantadas = emptyList(),
                cestasCanceladas = emptyList()
            )
            return
        }

        cestasListener = cestasRepository.listenCestasForApoiado(
            apoiadoKeys = keys,
            onSuccess = { todasCestas ->
                val canceladas = todasCestas
                    .filter { isCancelled(it.estadoCesta) }
                    .sortedByDescending { it.dataRecolha }

                val cestasAtivas = todasCestas.filterNot { isCancelled(it.estadoCesta) }

                val naoLevantadas = cestasAtivas
                    .filter { isMissed(it.estadoCesta) }
                    .sortedByDescending { it.dataRecolha }

                val realizadas = cestasAtivas
                    .filter { isCompleted(it.estadoCesta) }
                    .sortedByDescending { it.dataRecolha }

                val pendentes = cestasAtivas
                    .filter { !isCompleted(it.estadoCesta) && !isMissed(it.estadoCesta) }
                    .sortedBy { it.dataRecolha }

                uiState.value = uiState.value.copy(
                    cestasPendentes = pendentes,
                    cestasRealizadas = realizadas,
                    cestasNaoLevantadas = naoLevantadas,
                    cestasCanceladas = canceladas
                )
            },
            onError = { e ->
                Log.e("ApoiadoHome", "Erro ao ler cestas", e)
            }
        )
    }

    fun loadProdutosForCesta(cestaId: String, produtoIds: List<String>) {
        val normalizedId = cestaId.trim()
        if (normalizedId.isBlank()) return

        val current = uiState.value
        val existing = current.produtosByCesta[normalizedId]
        if (existing != null && (existing.isLoading || existing.produtos.isNotEmpty() || existing.error != null)) {
            return
        }

        if (produtoIds.isEmpty()) {
            uiState.value = current.copy(
                produtosByCesta = current.produtosByCesta + (normalizedId to CestaProdutosUi())
            )
            return
        }

        uiState.value = current.copy(
            produtosByCesta = current.produtosByCesta + (normalizedId to CestaProdutosUi(isLoading = true))
        )

        cestasRepository.fetchProdutosByIds(produtoIds) { products, missingIds, errorMessage ->
            val labels = products.map { product ->
                val base = product.nomeProduto.ifBlank { product.id }
                val marca = product.marca?.takeIf { it.isNotBlank() }
                if (marca != null) "$base - $marca" else base
            }
            val updated = CestaProdutosUi(
                isLoading = false,
                produtos = labels,
                error = errorMessage,
                missingCount = missingIds.size
            )
            uiState.value = uiState.value.copy(
                produtosByCesta = uiState.value.produtosByCesta + (normalizedId to updated)
            )
        }
    }

    // --- FUNÇÕES ORIGINAIS MANTIDAS ---

    private fun fetchUrgentRequests(numMec: String) {
        urgentRequestsListener?.remove()
        urgentRequestsListener = urgentRequestsRepository.listenByNumeroMecanografico(
            numeroMecanografico = numMec,
            onSuccess = { requests ->
                uiState.value = uiState.value.copy(urgentRequests = requests)
            },
            onError = { }
        )
    }

    private fun fetchDenialReason(docId: String) {
        apoiadoRepository.fetchLatestDenialReason(
            docId = docId,
            onSuccess = { motivo ->
                uiState.value = uiState.value.copy(motivoNegacao = motivo)
            },
            onError = { }
        )
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun resetToTryAgain(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true)

            apoiadoRepository.resetToRetry(
                docId = state.docId,
                onSuccess = {
                    checkStatus()
                    onSuccess()
                },
                onError = {
                    uiState.value = state.copy(isLoading = false, error = "Erro ao reiniciar: ${it.message}")
                }
            )
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true, error = null)
            apoiadoRepository.changePassword(
                docId = state.docId,
                oldPassword = oldPass,
                newPassword = newPass,
                onSuccess = {
                    uiState.value = state.copy(isLoading = false, showMandatoryPasswordChange = false)
                    onSuccess()
                },
                onError = { message ->
                    uiState.value = state.copy(isLoading = false, error = message)
                }
            )
        }
    }

    private fun isCompleted(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        return eLower.contains("entreg") ||
            // Evitar falso positivo em "nao levantou".
            eLower.contains("levantad") ||
            eLower.contains("conclu") ||
            eLower.contains("finaliz")
    }

    private fun isMissed(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        val normalized = eLower.replace('_', ' ')
        return normalized.contains("não levant") ||
            normalized.contains("nao levant") ||
            normalized.contains("faltou")
    }

    private fun isCancelled(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        return eLower.contains("cancel")
    }

    override fun onCleared() {
        cestasListener?.remove()
        cestasListener = null
        urgentRequestsListener?.remove()
        urgentRequestsListener = null
        super.onCleared()
    }
}
