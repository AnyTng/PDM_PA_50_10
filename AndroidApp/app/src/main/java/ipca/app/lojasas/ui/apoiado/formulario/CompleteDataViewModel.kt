package ipca.app.lojasas.ui.apoiado.formulario

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoFormData
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.utils.AccountValidity
import ipca.app.lojasas.utils.Validators
import java.util.Date
import javax.inject.Inject

data class CompleteDataState(
    var relacaoIPCA: String = "", // "Estudante", "Funcionário", "Docente"
    var curso: String = "",
    var graoEnsino: String = "", // "CTeSP", "Licenciatura", "Mestrado"
    var apoioEmergencia: Boolean = false,
    var bolsaEstudos: Boolean = false,
    var valorBolsa: String = "",
    var dataNascimento: Long? = null,
    var nacionalidade: String = "",
    var necessidades: List<String> = emptyList(),

    // Lista de sugestões
    var availableNationalities: List<String> = emptyList(),

    var isLoading: Boolean = false,
    var error: String? = null
)

@HiltViewModel
class CompleteDataViewModel @Inject constructor(
    private val repository: ApoiadoRepository
) : ViewModel() {

    var uiState = mutableStateOf(CompleteDataState())
        private set

    init {
        fetchNationalities()
    }

    // --- FUNÇÕES DE UPDATE ---

    // Ao mudar a relação, se deixar de ser Estudante, limpamos os dados específicos
    fun onRelacaoChange(value: String) {
        val isStudent = (value == "Estudante")
        uiState.value = uiState.value.copy(
            relacaoIPCA = value,
            // Limpa campos se não for estudante
            curso = if (isStudent) uiState.value.curso else "",
            graoEnsino = if (isStudent) uiState.value.graoEnsino else "",
            bolsaEstudos = if (isStudent) uiState.value.bolsaEstudos else false,
            valorBolsa = if (isStudent) uiState.value.valorBolsa else ""
        )
    }

    fun onCursoChange(value: String) { uiState.value = uiState.value.copy(curso = value) }
    fun onGraoChange(value: String) { uiState.value = uiState.value.copy(graoEnsino = value) }
    fun onApoioEmergenciaChange(value: Boolean) { uiState.value = uiState.value.copy(apoioEmergencia = value) }
    fun onBolsaChange(value: Boolean) { uiState.value = uiState.value.copy(bolsaEstudos = value) }
    fun onValorBolsaChange(value: String) { uiState.value = uiState.value.copy(valorBolsa = value) }
    fun onDataNascimentoChange(value: Long?) { uiState.value = uiState.value.copy(dataNascimento = value) }
    fun onNacionalidadeChange(value: String) { uiState.value = uiState.value.copy(nacionalidade = value) }

    fun toggleNecessidade(item: String) {
        val currentList = uiState.value.necessidades.toMutableList()
        if (currentList.contains(item)) {
            currentList.remove(item)
        } else {
            currentList.add(item)
        }
        uiState.value = uiState.value.copy(necessidades = currentList)
    }

    // --- CARREGAMENTO DE DADOS ---

    private fun fetchNationalities() {
        repository.fetchNationalities(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(availableNationalities = list)
            },
            onError = { }
        )
    }

    fun loadData(docId: String) {
        uiState.value = uiState.value.copy(isLoading = true)
        repository.fetchApoiadoFormData(
            docId = docId,
            onSuccess = { form ->
                if (form == null) {
                    uiState.value = uiState.value.copy(isLoading = false)
                    return@fetchApoiadoFormData
                }

                applyFormData(form)
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
        )
    }

    fun submitData(docId: String, onSuccess: () -> Unit) {
        val state = uiState.value

        // --- Validações (defesa extra no ViewModel) ---
        if (state.nacionalidade.isBlank()) {
            uiState.value = state.copy(error = "A nacionalidade é obrigatória.")
            return
        }
        val birth = state.dataNascimento
        if (birth == null) {
            uiState.value = state.copy(error = "A data de nascimento é obrigatória.")
            return
        }
        if (!Validators.isAgeAtLeast(birth, Validators.MIN_AGE_YEARS)) {
            uiState.value = state.copy(error = "Deve ter pelo menos ${Validators.MIN_AGE_YEARS} anos.")
            return
        }
        if (state.relacaoIPCA.isBlank()) {
            uiState.value = state.copy(error = "Selecione a relação com o IPCA.")
            return
        }
        if (state.necessidades.isEmpty()) {
            uiState.value = state.copy(error = "Selecione pelo menos uma necessidade.")
            return
        }

        if (state.relacaoIPCA == "Estudante") {
            if (state.curso.isBlank()) {
                uiState.value = state.copy(error = "O curso é obrigatório para estudantes.")
                return
            }
            if (state.graoEnsino.isBlank()) {
                uiState.value = state.copy(error = "O grau de ensino é obrigatório para estudantes.")
                return
            }
            if (state.bolsaEstudos) {
                val parsed = state.valorBolsa.trim().replace(',', '.').toDoubleOrNull()
                if (parsed == null || parsed <= 0) {
                    uiState.value = state.copy(error = "O valor da bolsa deve ser um número válido (> 0).")
                    return
                }
            }
        }

        uiState.value = state.copy(isLoading = true, error = null)

        val precisaDocumentos = !state.apoioEmergencia

        // ✅ A validade da conta passa a ser atribuída no momento da submissão do formulário.
        val validadeConta = AccountValidity.nextSeptembem30()

        val updateMap = hashMapOf<String, Any>(
            "relacaoIPCA" to state.relacaoIPCA,
            "apoioEmergenciaSocial" to state.apoioEmergencia,
            "bolsaEstudos" to state.bolsaEstudos,
            "nacionalidade" to state.nacionalidade.trim(),
            "necessidade" to state.necessidades,
            "dataNascimento" to Date(birth),
            "estadoConta" to if (precisaDocumentos) "Falta_Documentos" else "Analise",
            "dadosIncompletos" to false,
            "faltaDocumentos" to precisaDocumentos,
            "validadeConta" to validadeConta,
            "notifContaExpirada" to false
        )

        // Campos opcionais (mas validados acima quando aplicável)
        if (state.curso.isNotBlank()) updateMap["curso"] = state.curso.trim()
        if (state.graoEnsino.isNotBlank()) updateMap["graoEnsino"] = state.graoEnsino.trim()
        if (state.valorBolsa.isNotBlank()) updateMap["valorBolsa"] = state.valorBolsa.trim()

        repository.updateApoiadoFormData(
            docId = docId,
            data = updateMap,
            onSuccess = {
                uiState.value = state.copy(isLoading = false)
                onSuccess()
            },
            onError = { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao guardar: ${e.message}")
            }
        )
    }

    private fun applyFormData(form: ApoiadoFormData) {
        uiState.value = uiState.value.copy(
            relacaoIPCA = form.relacaoIPCA,
            curso = form.curso,
            graoEnsino = form.graoEnsino,
            apoioEmergencia = form.apoioEmergencia,
            bolsaEstudos = form.bolsaEstudos,
            valorBolsa = form.valorBolsa,
            nacionalidade = form.nacionalidade,
            necessidades = form.necessidades,
            dataNascimento = form.dataNascimento?.time,
            isLoading = false
        )
    }
}
