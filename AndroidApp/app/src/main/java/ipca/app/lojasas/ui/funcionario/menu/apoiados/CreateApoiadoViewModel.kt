package ipca.app.lojasas.ui.funcionario.menu.apoiados

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.apoiado.ApoiadoCreationInput
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.users.UserProfilesRepository
import ipca.app.lojasas.utils.Validators
import java.util.Date
import javax.inject.Inject

data class CreateApoiadoState(
    // Dados de Conta
    var numMecanografico: String = "",
    var nome: String = "",
    var email: String = "",
    var password: String = "",

    // Identificação e Contactos
    var contacto: String = "",
    var documentNumber: String = "", // NIF ou Passaporte
    var documentType: String = "NIF",
    var nacionalidade: String = "",
    var dataNascimento: Long? = null,
    var morada: String = "",
    var codPostal: String = "",

    // Dados Sócio-Económicos
    var relacaoIPCA: String = "Estudante",
    var curso: String = "",
    var graoEnsino: String = "Licenciatura",
    var apoioEmergencia: Boolean = false,
    var bolsaEstudos: Boolean = false,
    var valorBolsa: String = "",
    var necessidades: List<String> = emptyList(),
    var availableNationalities: List<String> = emptyList(),

    // Estado da UI
    var isLoading: Boolean = false,
    var isSuccess: Boolean = false,
    var error: String? = null
)

@HiltViewModel
class CreateApoiadoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val profilesRepository: UserProfilesRepository
) : ViewModel() {

    var uiState = mutableStateOf(CreateApoiadoState())
        private set

    init {
        fetchNationalities()
    }

    // --- Setters para a View ---
    // Nota UX: limpamos o erro ao editar para evitar “não acontece nada” em tentativas repetidas.
    private fun clearError() {
        val s = uiState.value
        if (s.error != null) uiState.value = s.copy(error = null)
    }

    fun onNumMecanograficoChange(v: String) { clearError(); uiState.value = uiState.value.copy(numMecanografico = v) }
    fun onNomeChange(v: String) { clearError(); uiState.value = uiState.value.copy(nome = v) }
    fun onEmailChange(v: String) { clearError(); uiState.value = uiState.value.copy(email = v) }
    fun onPasswordChange(v: String) { clearError(); uiState.value = uiState.value.copy(password = v) }

    fun onContactoChange(v: String) { clearError(); uiState.value = uiState.value.copy(contacto = v) }
    fun onDocumentNumberChange(v: String) { clearError(); uiState.value = uiState.value.copy(documentNumber = v) }
    fun onDocumentTypeChange(v: String) { clearError(); uiState.value = uiState.value.copy(documentType = v) }
    fun onNacionalidadeChange(v: String) { clearError(); uiState.value = uiState.value.copy(nacionalidade = v) }
    fun onDataNascimentoChange(v: Long?) { clearError(); uiState.value = uiState.value.copy(dataNascimento = v) }
    fun onMoradaChange(v: String) { clearError(); uiState.value = uiState.value.copy(morada = v) }
    fun onCodPostalChange(v: String) { clearError(); uiState.value = uiState.value.copy(codPostal = v) }

    fun onRelacaoChange(v: String) { clearError(); uiState.value = uiState.value.copy(relacaoIPCA = v) }
    fun onCursoChange(v: String) { clearError(); uiState.value = uiState.value.copy(curso = v) }
    fun onGraoChange(v: String) { clearError(); uiState.value = uiState.value.copy(graoEnsino = v) }
    fun onApoioEmergenciaChange(v: Boolean) { clearError(); uiState.value = uiState.value.copy(apoioEmergencia = v) }
    fun onBolsaChange(v: Boolean) { clearError(); uiState.value = uiState.value.copy(bolsaEstudos = v) }
    fun onValorBolsaChange(v: String) { clearError(); uiState.value = uiState.value.copy(valorBolsa = v) }

    fun toggleNecessidade(item: String) {
        clearError()
        val currentList = uiState.value.necessidades.toMutableList()
        if (currentList.contains(item)) currentList.remove(item) else currentList.add(item)
        uiState.value = uiState.value.copy(necessidades = currentList)
    }

    private fun fetchNationalities() {
        apoiadoRepository.fetchNationalities(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(availableNationalities = list)
            },
            onError = {
                uiState.value = uiState.value.copy(error = "Erro ao carregar países.")
            }
        )
    }

    fun createApoiado() {
        val s = uiState.value

        // --- Validações (campos obrigatórios + regras de formato) ---
        val email = s.email.trim()
        val nome = s.nome.trim()
        val numMec = s.numMecanografico.trim()
        val morada = s.morada.trim()
        val nacionalidadeInput = s.nacionalidade.trim()

        if (email.isBlank() || s.password.isBlank() || nome.isBlank() || numMec.isBlank()) {
            uiState.value = s.copy(error = "Preencha os campos obrigatórios (Email, Senha, Nome, Nº Mec.).")
            return
        }

        if (!Validators.isValidEmail(email)) {
            uiState.value = s.copy(error = "Email inválido.")
            return
        }

        if (s.password.length < 6) {
            uiState.value = s.copy(error = "A password deve ter pelo menos 6 caracteres.")
            return
        }

        if (!Validators.isValidMecanografico(numMec)) {
            uiState.value = s.copy(error = "O Nº Mecanográfico deve começar com uma letra seguida de números (ex: a12345).")
            return
        }

        val birthMillis = s.dataNascimento
        if (birthMillis == null) {
            uiState.value = s.copy(error = "A data de nascimento é obrigatória.")
            return
        }
        if (!Validators.isAgeAtLeast(birthMillis, Validators.MIN_AGE_YEARS)) {
            uiState.value = s.copy(error = "O apoiado deve ter pelo menos ${Validators.MIN_AGE_YEARS} anos.")
            return
        }

        if (nacionalidadeInput.isBlank()) {
            uiState.value = s.copy(error = "A nacionalidade é obrigatória.")
            return
        }
        val matchedNationality = s.availableNationalities.firstOrNull {
            it.equals(nacionalidadeInput, ignoreCase = true)
        }
        if (matchedNationality == null) {
            val message = if (s.availableNationalities.isEmpty()) {
                "Lista de países indisponível. Tente novamente."
            } else {
                "Selecione uma nacionalidade da lista."
            }
            uiState.value = s.copy(error = message)
            return
        }

        val contactoNorm = Validators.normalizePhonePT(s.contacto)
        if (contactoNorm == null) {
            uiState.value = s.copy(error = "Contacto inválido. Use 9 dígitos (ex: 912345678) ou +351...")
            return
        }

        if (morada.isBlank()) {
            uiState.value = s.copy(error = "A morada é obrigatória.")
            return
        }

        val codPostalNorm = Validators.normalizePostalCodePT(s.codPostal)
        if (codPostalNorm == null) {
            uiState.value = s.copy(error = "Código Postal inválido. Formato esperado: 1234-567")
            return
        }

        val documentNumberTrim = s.documentNumber.trim()
        if (documentNumberTrim.isBlank()) {
            uiState.value = s.copy(error = "O número de documento é obrigatório.")
            return
        }

        val normalizedDocNumber = if (s.documentType == "NIF") {
            if (!Validators.isValidNif(documentNumberTrim)) {
                uiState.value = s.copy(error = "NIF inválido.")
                return
            }
            Validators.normalizeNif(documentNumberTrim)!!
        } else {
            // Passaporte: regra simples (não vazio e tamanho mínimo)
            if (documentNumberTrim.length < 5) {
                uiState.value = s.copy(error = "Nº Passaporte inválido.")
                return
            }
            documentNumberTrim
        }

        if (s.relacaoIPCA == "Estudante" && s.curso.trim().isBlank()) {
            uiState.value = s.copy(error = "O curso é obrigatório para estudantes.")
            return
        }

        if (s.bolsaEstudos) {
            val bolsa = s.valorBolsa.trim().replace(',', '.').toDoubleOrNull()
            if (bolsa == null || bolsa <= 0) {
                uiState.value = s.copy(error = "O valor da bolsa deve ser um número válido (> 0).")
                return
            }
        }

        if (s.necessidades.isEmpty()) {
            uiState.value = s.copy(error = "Selecione pelo menos uma necessidade.")
            return
        }

        // Guarda valores normalizados no state (para persistência coerente)
        val normalizedState = s.copy(
            email = email,
            nome = nome,
            numMecanografico = numMec,
            contacto = contactoNorm,
            documentNumber = normalizedDocNumber,
            nacionalidade = matchedNationality,
            morada = morada,
            codPostal = codPostalNorm,
            valorBolsa = s.valorBolsa.trim()
        )

        uiState.value = normalizedState.copy(isLoading = true, error = null)

        profilesRepository.isNumMecanograficoAvailable(
            numMecanografico = normalizedState.numMecanografico,
            onResult = { available ->
                if (!available) {
                    uiState.value = normalizedState.copy(
                        isLoading = false,
                        error = "Já existe um utilizador com esse Nº Mecanográfico."
                    )
                    return@isNumMecanograficoAvailable
                }

                authRepository.createUserInSecondaryApp(
                    email = normalizedState.email,
                    password = normalizedState.password,
                    onSuccess = { uid ->
                        val input = ApoiadoCreationInput(
                            uid = uid,
                            numMecanografico = normalizedState.numMecanografico,
                            nome = normalizedState.nome,
                            email = normalizedState.email,
                            contacto = normalizedState.contacto,
                            documentNumber = normalizedState.documentNumber,
                            documentType = normalizedState.documentType,
                            nacionalidade = normalizedState.nacionalidade,
                            dataNascimento = Date(normalizedState.dataNascimento!!),
                            morada = normalizedState.morada,
                            codPostal = normalizedState.codPostal,
                            relacaoIPCA = normalizedState.relacaoIPCA,
                            curso = normalizedState.curso,
                            graoEnsino = normalizedState.graoEnsino,
                            apoioEmergencia = normalizedState.apoioEmergencia,
                            bolsaEstudos = normalizedState.bolsaEstudos,
                            valorBolsa = normalizedState.valorBolsa,
                            necessidades = normalizedState.necessidades
                        )

                        apoiadoRepository.createApoiadoProfile(
                            input = input,
                            onSuccess = {
                                val nome = normalizedState.nome.trim()
                                val details = buildString {
                                    if (nome.isNotBlank()) append("Nome: ").append(nome)
                                    val email = normalizedState.email.trim()
                                    if (email.isNotBlank()) {
                                        if (isNotEmpty()) append(" | ")
                                        append("Email: ").append(email)
                                    }
                                }.takeIf { it.isNotBlank() }
                                AuditLogger.logAction(
                                    action = "Criou beneficiario",
                                    entity = "apoiado",
                                    entityId = normalizedState.numMecanografico,
                                    details = details
                                )
                                uiState.value = normalizedState.copy(isLoading = false, isSuccess = true, error = null)
                            },
                            onError = { e ->
                                uiState.value = normalizedState.copy(isLoading = false, error = "Erro BD: ${e.message}")
                            }
                        )
                    },
                    onError = { e ->
                        uiState.value = normalizedState.copy(isLoading = false, error = "Erro Auth: ${e?.message}")
                    }
                )
            },
            onError = { e ->
                uiState.value = normalizedState.copy(
                    isLoading = false,
                    error = "Erro ao validar Nº Mecanográfico: ${e.message}"
                )
            }
        )
    }
}
