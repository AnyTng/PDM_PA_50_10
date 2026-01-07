package ipca.app.lojasas.ui.funcionario.menu.validate

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.apoiado.ApoiadoDetails
import ipca.app.lojasas.data.apoiado.ApoiadoDocumentsRepository
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.apoiado.ApoiadoSummary
import ipca.app.lojasas.data.apoiado.DocumentSummary
import ipca.app.lojasas.data.apoiado.SubmittedFile
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.funcionario.FuncionarioRepository
import java.util.Date
import javax.inject.Inject

// Estado da UI
data class ValidateState(
    val pendingAccounts: List<ApoiadoSummary> = emptyList(),
    val selectedApoiadoDetails: ApoiadoDetails? = null,
    val apoaidoDocuments: List<DocumentSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ValidateAccountsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val documentsRepository: ApoiadoDocumentsRepository
) : ViewModel() {

    var uiState = mutableStateOf(ValidateState())
        private set

    init {
        loadPendingAccounts()
    }

    fun loadPendingAccounts() {
        uiState.value = uiState.value.copy(isLoading = true)

        apoiadoRepository.fetchPendingApoiados(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(pendingAccounts = list, isLoading = false)
            },
            onError = { error ->
                uiState.value = uiState.value.copy(isLoading = false, error = error.message)
            }
        )
    }

    fun selectApoiado(apoiadoId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        apoiadoRepository.fetchApoiadoValidationDetails(
            apoiadoId = apoiadoId,
            onSuccess = { details ->
                if (details == null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Apoiado nao encontrado."
                    )
                    return@fetchApoiadoValidationDetails
                }

                documentsRepository.fetchSubmittedDocuments(
                    apoiadoId = apoiadoId,
                    onSuccess = { files ->
                        val docsList = mapDocumentSummaries(files)
                        uiState.value = uiState.value.copy(
                            selectedApoiadoDetails = details,
                            apoaidoDocuments = docsList,
                            isLoading = false
                        )
                    },
                    onError = { error ->
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar documentos: ${error.message}"
                        )
                    }
                )
            },
            onError = { error ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar perfil: ${error.message}"
                )
            }
        )
    }

    fun clearSelection() {
        uiState.value = uiState.value.copy(selectedApoiadoDetails = null, apoaidoDocuments = emptyList())
    }

    // --- AÇÕES DE VALIDAÇÃO ---

    fun approveAccount(apoiadoId: String, onSuccess: () -> Unit) {
        resolveFuncionarioId { funcionarioId ->
            uiState.value = uiState.value.copy(isLoading = true)
            apoiadoRepository.approveApoiadoAccount(
                apoiadoId = apoiadoId,
                funcionarioId = funcionarioId,
                onSuccess = {
                    loadPendingAccounts()
                    uiState.value = uiState.value.copy(selectedApoiadoDetails = null)
                    AuditLogger.logAction("Aprovou beneficiario", "apoiado", apoiadoId)
                    onSuccess()
                },
                onError = { error ->
                    uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${error.message}")
                }
            )
        }
    }

    fun denyAccount(apoiadoId: String, reason: String, onSuccess: () -> Unit) {
        resolveFuncionarioId { funcionarioId ->
            uiState.value = uiState.value.copy(isLoading = true)
            apoiadoRepository.denyApoiadoAccount(
                apoiadoId = apoiadoId,
                funcionarioId = funcionarioId,
                reason = reason,
                onSuccess = {
                    loadPendingAccounts()
                    uiState.value = uiState.value.copy(selectedApoiadoDetails = null)
                    AuditLogger.logAction(
                        action = "Negou beneficiario",
                        entity = "apoiado",
                        entityId = apoiadoId,
                        details = "Motivo: $reason"
                    )
                    onSuccess()
                },
                onError = { error ->
                    uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${error.message}")
                }
            )
        }
    }

    fun blockAccount(apoiadoId: String, onSuccess: () -> Unit) {
        resolveFuncionarioId { funcionarioId ->
            uiState.value = uiState.value.copy(isLoading = true)
            apoiadoRepository.blockApoiadoAccount(
                apoiadoId = apoiadoId,
                funcionarioId = funcionarioId,
                onSuccess = {
                    loadPendingAccounts()
                    uiState.value = uiState.value.copy(selectedApoiadoDetails = null)
                    AuditLogger.logAction("Bloqueou beneficiario", "apoiado", apoiadoId)
                    onSuccess()
                },
                onError = { error ->
                    uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${error.message}")
                }
            )
        }
    }

    fun getFileUri(path: String, onResult: (android.net.Uri?) -> Unit) {
        documentsRepository.getFileUrl(path, onResult)
    }

    private fun resolveFuncionarioId(onResult: (String) -> Unit) {
        val uid = authRepository.currentUserId()
        if (uid.isNullOrBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            return
        }

        funcionarioRepository.fetchFuncionarioIdByUid(
            uid = uid,
            onSuccess = { id -> onResult(id ?: "unknown_staff") },
            onError = { error ->
                uiState.value = uiState.value.copy(isLoading = false, error = error.message)
            }
        )
    }

    private fun mapDocumentSummaries(files: List<SubmittedFile>): List<DocumentSummary> {
        return files.map { file ->
            DocumentSummary(
                typeTitle = file.title,
                fileName = file.fileName,
                url = file.storagePath,
                date = file.date,
                entrega = file.numeroEntrega
            )
        }.sortedWith(compareByDescending<DocumentSummary> { it.entrega }.thenByDescending { it.date ?: Date(0) })
    }
}
