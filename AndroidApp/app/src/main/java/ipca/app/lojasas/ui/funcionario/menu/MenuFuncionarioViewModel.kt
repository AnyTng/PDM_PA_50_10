package ipca.app.lojasas.ui.funcionario.menu

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.funcionario.FuncionarioRepository
import javax.inject.Inject

@HiltViewModel
class MenuFuncionarioViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val chatRepository: ipca.app.lojasas.data.chat.ChatRepository
) : ViewModel() {
    var isAdmin = mutableStateOf(false)
        private set

    // Badge/aviso no botão de mensagens (quando existem chats com mensagens novas)
    var hasNewMessages = mutableStateOf(false)
        private set

    private var unreadListener: ipca.app.lojasas.data.common.ListenerHandle? = null

    init {
        refreshRole()
        listenUnreadChats()
    }

    private fun listenUnreadChats() {
        unreadListener?.remove()
        unreadListener = chatRepository.listenHasUnreadForStaff(
            onSuccess = { hasNewMessages.value = it },
            onError = { }
        )
    }

    fun refreshRole() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) {
            isAdmin.value = false
            return
        }

        funcionarioRepository.fetchIsAdminByUid(
            uid = uid,
            onSuccess = { isAdmin.value = it },
            onError = { }
        )
    }

    fun signOut() {
        authRepository.signOut()
    }

    override fun onCleared() {
        unreadListener?.remove()
        unreadListener = null
        super.onCleared()
    }
}
