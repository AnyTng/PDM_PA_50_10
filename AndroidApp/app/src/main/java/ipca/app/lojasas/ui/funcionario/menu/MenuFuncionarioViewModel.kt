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
    private val funcionarioRepository: FuncionarioRepository
) : ViewModel() {
    var isAdmin = mutableStateOf(false)
        private set

    init {
        refreshRole()
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
}
