package ipca.app.lojasas.ui.funcionario.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.funcionario.CollaboratorItem
import ipca.app.lojasas.data.funcionario.FuncionarioRepository
import javax.inject.Inject

data class CollaboratorsListState(
    val collaborators: List<CollaboratorItem> = emptyList(),
    val filteredList: List<CollaboratorItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class CollaboratorsListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val funcionarioRepository: FuncionarioRepository
) : ViewModel() {
    var uiState = mutableStateOf(CollaboratorsListState())
        private set

    private var listener: ListenerHandle? = null

    init {
        val currentUid = authRepository.currentUserId().orEmpty()
        uiState.value = uiState.value.copy(currentUserId = currentUid)
        loadCollaborators()
    }

    private fun loadCollaborators() {
        uiState.value = uiState.value.copy(isLoading = true)
        listener?.remove()
        listener = funcionarioRepository.listenCollaborators(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(collaborators = list, isLoading = false)
                filterList()
            },
            onError = { error ->
                uiState.value = uiState.value.copy(isLoading = false, error = error.message)
            }
        )
    }

    fun onSearchChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        filterList()
    }

    private fun filterList() {
        val query = uiState.value.searchQuery.trim().lowercase()
        val currentList = uiState.value.collaborators

        if (query.isEmpty()) {
            uiState.value = uiState.value.copy(filteredList = currentList)
        } else {
            val filtered = currentList.filter {
                it.nome.lowercase().contains(query) ||
                    it.id.lowercase().contains(query) ||
                    (if (it.role.equals("Admin", ignoreCase = true)) "administrador" else "colaborador").contains(query)
            }
            uiState.value = uiState.value.copy(filteredList = filtered)
        }
    }

    fun deleteCollaborator(item: CollaboratorItem, onSuccess: () -> Unit) {
        if (item.uid == uiState.value.currentUserId) return

        funcionarioRepository.deleteCollaborator(
            collaboratorId = item.id,
            uid = item.uid,
            onSuccess = {
                val details = buildString {
                    if (item.nome.isNotBlank()) append("Nome: ").append(item.nome)
                    if (item.email.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Email: ").append(item.email)
                    }
                }.takeIf { it.isNotBlank() }
                AuditLogger.logAction(
                    action = "Removeu colaborador",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
                onSuccess()
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = "Erro ao apagar: ${e.message}")
            }
        )
    }

    fun promoteToAdmin(item: CollaboratorItem) {
        funcionarioRepository.updateCollaboratorRole(
            collaboratorId = item.id,
            role = "Admin",
            onSuccess = {
                val details = if (item.nome.isNotBlank()) "Nome: ${item.nome}" else null
                AuditLogger.logAction(
                    action = "Promoveu colaborador a Admin",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = "Erro ao promover: ${e.message}")
            }
        )
    }

    fun demoteToFuncionario(item: CollaboratorItem) {
        if (item.uid == uiState.value.currentUserId) {
            uiState.value = uiState.value.copy(error = "Não pode remover as suas próprias permissões de Admin.")
            return
        }

        funcionarioRepository.updateCollaboratorRole(
            collaboratorId = item.id,
            role = "Funcionario",
            onSuccess = {
                val details = if (item.nome.isNotBlank()) "Nome: ${item.nome}" else null
                AuditLogger.logAction(
                    action = "Despromoveu colaborador",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = "Erro ao despromover: ${e.message}")
            }
        )
    }

    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
        listener = null
    }
}
