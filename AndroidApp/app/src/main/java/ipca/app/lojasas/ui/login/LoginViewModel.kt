package ipca.app.lojasas.ui.login

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.data.UserRoleRepository
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.notifications.NotificationRepository
import javax.inject.Inject


const val TAG = "LojaSocial"

data class LoginState (
    var email : String? = null,
    var password : String? = null,
    var error : String? = null,
    var isLoading : Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var uiState = mutableStateOf(LoginState())
        private set

    fun updateEmail(email : String) {
        uiState.value = uiState.value.copy(email = email)
    }

    fun updatePassword(password : String) {
        uiState.value = uiState.value.copy(password = password)
    }

    fun login(onLoginSuccess:(UserRole)->Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (uiState.value.email.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Email is required")
            return
        }

        if (uiState.value.password.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Password is required")
            return
        }

        val email = uiState.value.email!!.trim()
        authRepository.signIn(
            email = email,
            password = uiState.value.password!!,
            onSuccess = {
                Log.d(TAG, "signInWithEmail:success")
                resolveRoleAndProceed(email, onLoginSuccess)
            },
            onError = { error ->
                Log.w(TAG, "signInWithEmail:failure", error)
                uiState.value = uiState.value.copy(isLoading = false, error = "Credenciais inválidas ou sem internet.")
            }
        )
    }

    // --- ESTA É A FUNÇÃO QUE FALTAVA ---
    fun recoverPassword(onResult: (String) -> Unit) {
        val email = uiState.value.email
        if (email.isNullOrEmpty()) {
            onResult("Por favor, preencha o campo de email primeiro.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        authRepository.sendPasswordReset(
            email = email,
            onSuccess = {
                uiState.value = uiState.value.copy(isLoading = false)
                onResult("Email de recuperação enviado! Verifique a sua caixa de correio.")
            },
            onError = { error ->
                uiState.value = uiState.value.copy(isLoading = false)
                onResult("Erro: ${error?.message}")
            }
        )
    }
    // -----------------------------------

    private fun resolveRoleAndProceed(email: String, onLoginSuccess: (UserRole) -> Unit) {
        userRoleRepository.fetchUserRoleByEmail(
            email = email,
            onSuccess = { role ->
                uiState.value = uiState.value.copy(isLoading = false, error = null)

                // 1) Tópicos (funcionarios + admin)
                notificationRepository.configureForRole(role)

                // 2) Se for Apoiado, guarda token para notificações "1 a 1"
                if (role == UserRole.APOIADO) {
                    notificationRepository.saveApoiadoToken(email)
                }

                onLoginSuccess(role)
            },
            onNotFound = {
                uiState.value = uiState.value.copy(isLoading = false, error = "Conta sem perfil associado.")
            },
            onError = { exception ->
                Log.e(TAG, "Erro ao obter perfil", exception)
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao obter tipo de utilizador.")
            }
        )
    }

}
