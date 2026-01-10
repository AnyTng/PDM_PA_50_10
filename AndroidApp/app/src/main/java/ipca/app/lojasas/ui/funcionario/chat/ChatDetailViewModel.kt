package ipca.app.lojasas.ui.funcionario.chat

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.chat.ChatMessage
import ipca.app.lojasas.data.chat.ChatRepository
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.funcionario.FuncionarioRepository
import javax.inject.Inject

data class ChatDetailUiState(
    val isLoading: Boolean = true,
    val apoiadoId: String = "",
    val apoiadoNome: String = "",
    val staffName: String = "",
    val staffRole: String = ChatRepository.ROLE_FUNCIONARIO,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var uiState = mutableStateOf(ChatDetailUiState())
        private set

    private var messagesListener: ListenerHandle? = null

    private val apoiadoIdArg: String = savedStateHandle.get<String>("apoiadoId")
        ?.let { Uri.decode(it) }
        .orEmpty()

    init {
        load()
    }

    private fun load() {
        if (apoiadoIdArg.isBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Apoiado inválido")
            return
        }

        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador nao autenticado")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, apoiadoId = apoiadoIdArg, error = null)

        // 1) Buscar nome e role do staff
        funcionarioRepository.fetchFuncionarioNameAndRoleByUid(
            uid = uid,
            onSuccess = { name, roleStr ->
                val resolvedRole = if (roleStr.equals("Admin", ignoreCase = true)) {
                    ChatRepository.ROLE_ADMIN
                } else {
                    ChatRepository.ROLE_FUNCIONARIO
                }

                uiState.value = uiState.value.copy(staffName = name, staffRole = resolvedRole)

                // 2) Buscar nome do apoiado
                apoiadoRepository.fetchApoiadoPdfDetails(
                    apoiadoId = apoiadoIdArg,
                    onSuccess = { details ->
                        uiState.value = uiState.value.copy(
                            apoiadoNome = details?.nome ?: apoiadoIdArg,
                            isLoading = false
                        )
                        startListening()
                    },
                    onError = { e ->
                        uiState.value = uiState.value.copy(
                            apoiadoNome = apoiadoIdArg,
                            isLoading = false,
                            error = e.message
                        )
                        startListening()
                    }
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao obter dados do funcionário"
                )
            }
        )
    }

    private fun startListening() {
        val apoiadoId = uiState.value.apoiadoId
        if (apoiadoId.isBlank()) return

        messagesListener?.remove()
        messagesListener = chatRepository.listenChatMessages(
            apoiadoId = apoiadoId,
            onSuccess = { msgs ->
                uiState.value = uiState.value.copy(messages = msgs, isLoading = false)

                val hasUnseenIncoming = msgs.any {
                    it.senderRole.equals(ChatRepository.ROLE_APOIADO, ignoreCase = true) && it.seenByStaffAt == null
                }
                if (hasUnseenIncoming) {
                    chatRepository.markThreadAsSeen(
                        apoiadoId = apoiadoId,
                        viewerIsApoiado = false
                    )
                }
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message ?: "Erro a carregar mensagens")
            }
        )

        // Ao abrir o ecrã, fazer reset ao contador.
        chatRepository.markThreadAsSeen(
            apoiadoId = apoiadoId,
            viewerIsApoiado = false
        )
    }

    fun sendMessage(text: String) {
        val state = uiState.value
        if (state.apoiadoId.isBlank()) return

        chatRepository.sendMessage(
            apoiadoId = state.apoiadoId,
            text = text,
            senderName = state.staffName.ifBlank { "Funcionário" },
            senderRole = state.staffRole,
            onSuccess = { },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message ?: "Erro ao enviar mensagem")
            }
        )
    }

    override fun onCleared() {
        messagesListener?.remove()
        messagesListener = null
        super.onCleared()
    }
}
