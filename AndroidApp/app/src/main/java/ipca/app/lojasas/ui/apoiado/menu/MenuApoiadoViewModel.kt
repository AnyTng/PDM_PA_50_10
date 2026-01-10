package ipca.app.lojasas.ui.apoiado.menu

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import javax.inject.Inject

@HiltViewModel
class MenuApoiadoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val chatRepository: ipca.app.lojasas.data.chat.ChatRepository
) : ViewModel() {
    var isBlock = mutableStateOf(false)
        private set
    var isApproved = mutableStateOf(false)
        private set

    // 1. Adicionar esta variável de estado
    var numeroMecanografico = mutableStateOf("")
        private set

    // Badge/aviso no botão de mensagens
    var hasNewMessages = mutableStateOf(false)
        private set

    private var statusListener: ListenerHandle? = null
    private var chatUnreadListener: ListenerHandle? = null

    init {
        checkStatus()
    }

    private fun checkStatus() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) return

        statusListener?.remove()
        statusListener = apoiadoRepository.listenApoiadoStatus(
            uid = uid,
            onSuccess = { status ->
                val estado = status.estadoConta
                isBlock.value = (estado == "Bloqueado")
                isApproved.value = (estado == "Aprovado" || estado == "Suspenso")
                numeroMecanografico.value = status.numeroMecanografico

                // Inicia listener das mensagens não lidas quando já sabemos o id do apoiado.
                startChatUnreadListener(status.numeroMecanografico)
            },
            onError = { }
        )
    }

    private fun startChatUnreadListener(apoiadoId: String) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) return

        if (chatUnreadListener != null) return

        chatUnreadListener = chatRepository.listenHasUnreadForApoiado(
            apoiadoId = normalized,
            onSuccess = { hasNew -> hasNewMessages.value = hasNew },
            onError = { }
        )
    }

    override fun onCleared() {
        statusListener?.remove()
        statusListener = null
        chatUnreadListener?.remove()
        chatUnreadListener = null
        super.onCleared()
    }

    fun signOut() {
        authRepository.signOut()
    }
}
