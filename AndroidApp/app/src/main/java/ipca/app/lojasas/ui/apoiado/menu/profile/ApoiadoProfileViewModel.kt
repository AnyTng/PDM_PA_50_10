package ipca.app.lojasas.ui.apoiado.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.users.UserProfile
import ipca.app.lojasas.data.users.UserProfilesRepository
import ipca.app.lojasas.ui.funcionario.menu.profile.ProfileState
import ipca.app.lojasas.utils.Validators
import javax.inject.Inject

@HiltViewModel
class ApoiadoProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profilesRepository: UserProfilesRepository
) : ViewModel() {

    var uiState = mutableStateOf(ProfileState())
        private set

    fun onNomeChange(newValue: String) { uiState.value = uiState.value.copy(nome = newValue) }
    fun onContactoChange(newValue: String) { uiState.value = uiState.value.copy(contacto = newValue) }
    fun onMoradaChange(newValue: String) { uiState.value = uiState.value.copy(morada = newValue) }
    fun onCodPostalChange(newValue: String) { uiState.value = uiState.value.copy(codPostal = newValue) }

    fun loadUserProfile() {
        val uid = authRepository.currentUserId()
        if (uid.isNullOrBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        profilesRepository.fetchApoiadoProfileByUid(
            uid = uid,
            onSuccess = { profile ->
                if (profile == null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Perfil não encontrado.")
                    return@fetchApoiadoProfileByUid
                }
                applyProfile(profile)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
        )
    }

    private fun applyProfile(profile: UserProfile) {
        uiState.value = ProfileState(
            numMecanografico = profile.docId,
            nome = profile.nome,
            contacto = profile.contacto,
            email = profile.email,
            documentNumber = profile.documentNumber,
            documentType = profile.documentType,
            morada = profile.morada,
            codPostal = profile.codPostal,
            role = profile.role,
            isAdmin = profile.isAdmin,
            nacionalidade = profile.nacionalidade,
            dataNascimento = profile.dataNascimento,
            relacaoIPCA = profile.relacaoIPCA,
            curso = profile.curso,
            graoEnsino = profile.graoEnsino,
            apoioEmergencia = profile.apoioEmergencia,
            bolsaEstudos = profile.bolsaEstudos,
            valorBolsa = profile.valorBolsa,
            necessidades = profile.necessidades,
            validadeConta = profile.validadeConta,
            isLoading = false,
            error = null,
            success = false
        )
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val state = uiState.value
        val role = state.role ?: UserRole.APOIADO
        if (state.numMecanografico.isEmpty()) {
            uiState.value = state.copy(error = "Perfil não encontrado.")
            return
        }

        val nome = state.nome.trim()
        if (nome.isEmpty()) {
            uiState.value = state.copy(error = "O nome não pode estar vazio.")
            return
        }

        val contactoTrim = state.contacto.trim()
        val contactoNorm = if (contactoTrim.isNotBlank()) Validators.normalizePhonePT(contactoTrim) else ""
        if (contactoTrim.isNotBlank() && contactoNorm == null) {
            uiState.value = state.copy(error = "Contacto inválido. Use 9 dígitos (ex: 912345678) ou +351...")
            return
        }

        val codPostalTrim = state.codPostal.trim()
        val codPostalNorm = if (codPostalTrim.isNotBlank()) Validators.normalizePostalCodePT(codPostalTrim) else ""
        if (codPostalTrim.isNotBlank() && codPostalNorm == null) {
            uiState.value = state.copy(error = "Código Postal inválido. Formato esperado: 1234-567")
            return
        }

        val morada = state.morada.trim()

        uiState.value = state.copy(
            isLoading = true,
            error = null,
            nome = nome,
            contacto = contactoNorm ?: "",
            morada = morada,
            codPostal = codPostalNorm ?: ""
        )

        val updates = hashMapOf<String, Any>(
            "nome" to nome,
            "contacto" to (contactoNorm ?: ""),
            "morada" to morada,
            "codPostal" to (codPostalNorm ?: "")
        )

        profilesRepository.updateProfile(
            role = role,
            docId = state.numMecanografico,
            updates = updates,
            onSuccess = {
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Atualizou perfil", "apoiado", state.numMecanografico, details)
                uiState.value = uiState.value.copy(isLoading = false, success = true)
                onSuccess()
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao atualizar: ${e.message}")
            }
        )
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val state = uiState.value
        val role = state.role ?: UserRole.APOIADO
        if (state.numMecanografico.isEmpty()) return

        uiState.value = state.copy(isLoading = true)

        profilesRepository.deleteProfile(
            role = role,
            docId = state.numMecanografico,
            onSuccess = {
                authRepository.deleteCurrentUser(
                    onSuccess = {
                        AuditLogger.logAction("Apagou conta", "apoiado", state.numMecanografico)
                        uiState.value = state.copy(isLoading = false)
                        onSuccess()
                    },
                    onError = { e ->
                        uiState.value = state.copy(isLoading = false, error = "Erro ao apagar conta Auth: ${e?.message}")
                    }
                )
            },
            onError = { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao apagar dados: ${e.message}")
            }
        )
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)
        authRepository.changePassword(
            oldPassword = oldPass,
            newPassword = newPass,
            onSuccess = {
                uiState.value = uiState.value.copy(isLoading = false)
                onSuccess()
            },
            onError = { message ->
                uiState.value = uiState.value.copy(isLoading = false)
                onError(message)
            }
        )
    }
}
