package ipca.app.lojasas.ui.apoiado.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.chat.ChatMessage
import ipca.app.lojasas.data.chat.ChatRepository
import ipca.app.lojasas.data.common.ListenerHandle
import javax.inject.Inject

data class ApoiadoChatUiState(
    val isLoading: Boolean = true,
    val apoiadoId: String = "",
    val apoiadoNome: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ApoiadoChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    var uiState = mutableStateOf(ApoiadoChatUiState())
        private set

    private var messagesListener: ListenerHandle? = null

    init {
        loadChat()
    }

    private fun loadChat() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador nao autenticado.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        apoiadoRepository.fetchApoiadoProfileByUid(
            uid = uid,
            onSuccess = { profile ->
                if (profile == null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Perfil de apoiado nao encontrado."
                    )
                    return@fetchApoiadoProfileByUid
                }

                val apoiadoId = profile.docId
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    apoiadoId = apoiadoId,
                    apoiadoNome = profile.nome,
                    error = null
                )

                startListening(apoiadoId)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao obter perfil."
                )
            }
        )
    }

    private fun startListening(apoiadoId: String) {
        messagesListener?.remove()
        messagesListener = chatRepository.listenChatMessages(
            apoiadoId = apoiadoId,
            onSuccess = { msgs ->
                uiState.value = uiState.value.copy(messages = msgs, isLoading = false)

                // Se o chat está aberto, marcamos automaticamente como visto.
                val hasUnseenIncoming = msgs.any {
                    !it.senderRole.equals(ChatRepository.ROLE_APOIADO, ignoreCase = true) && it.seenByApoiadoAt == null
                }
                if (hasUnseenIncoming) {
                    chatRepository.markThreadAsSeen(
                        apoiadoId = apoiadoId,
                        viewerIsApoiado = true
                    )
                }
            },
            onError = { e ->
                uiState.value = uiState.value.copy(error = e.message ?: "Erro a carregar mensagens.")
            }
        )

        // Ao abrir o ecrã, fazer reset do contador de não lidas (mesmo que já estejam vistas).
        chatRepository.markThreadAsSeen(
            apoiadoId = apoiadoId,
            viewerIsApoiado = true
        )
    }

    fun sendMessage(text: String) {
        val state = uiState.value
        val apoiadoId = state.apoiadoId
        if (apoiadoId.isBlank()) return

        chatRepository.sendMessage(
            apoiadoId = apoiadoId,
            text = text,
            senderName = state.apoiadoNome.ifBlank { "Apoiado" },
            senderRole = ChatRepository.ROLE_APOIADO,
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
