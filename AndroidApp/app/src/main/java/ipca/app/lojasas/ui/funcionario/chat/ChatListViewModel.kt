package ipca.app.lojasas.ui.funcionario.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.chat.ChatRepository
import ipca.app.lojasas.data.chat.ChatThreadSummary
import ipca.app.lojasas.data.common.ListenerHandle
import javax.inject.Inject

data class ChatListUiState(
    val isLoading: Boolean = true,
    val threads: List<ChatThreadSummary> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    var uiState = mutableStateOf(ChatListUiState())
        private set

    private var listener: ListenerHandle? = null

    init {
        listen()
    }

    private fun listen() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        listener?.remove()
        listener = chatRepository.listenChatThreadsForStaff(
            onSuccess = { list ->
                uiState.value = uiState.value.copy(isLoading = false, threads = list, error = null)
            },
            onError = { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message ?: "Erro ao carregar chats")
            }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
