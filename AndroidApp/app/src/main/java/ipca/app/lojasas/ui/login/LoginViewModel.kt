package ipca.app.lojasas.ui.login

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.data.UserRoleRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging


const val TAG = "LojaSocial"

data class LoginState (
    var email : String? = null,
    var password : String? = null,
    var error : String? = null,
    var isLoading : Boolean = false
)

class LoginViewModel : ViewModel() {

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
        val auth: FirebaseAuth = Firebase.auth
        auth.signInWithEmailAndPassword(email, uiState.value.password!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    resolveRoleAndProceed(email, onLoginSuccess)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    uiState.value = uiState.value.copy(isLoading = false, error = "Credenciais inválidas ou sem internet.")
                }
            }
    }

    // --- ESTA É A FUNÇÃO QUE FALTAVA ---
    fun recoverPassword(onResult: (String) -> Unit) {
        val email = uiState.value.email
        if (email.isNullOrEmpty()) {
            onResult("Por favor, preencha o campo de email primeiro.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                uiState.value = uiState.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    onResult("Email de recuperação enviado! Verifique a sua caixa de correio.")
                } else {
                    onResult("Erro: ${task.exception?.message}")
                }
            }
    }
    // -----------------------------------

    private fun resolveRoleAndProceed(email: String, onLoginSuccess:(UserRole)->Unit) {
        UserRoleRepository.fetchUserRoleByEmail(
            email = email,
            onSuccess = { role ->
                uiState.value = uiState.value.copy(isLoading = false, error = null)

                configureTopicForRole(role)

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


//Para notificações
private fun configureTopicForRole(role: UserRole) {
    val messaging = FirebaseMessaging.getInstance()
    val roleStr = role.toString()

    val isFuncionario = roleStr.equals("Funcionario", ignoreCase = true)
    val isAdmin = roleStr.equals("Admin", ignoreCase = true) ||
            roleStr.equals("Administrador", ignoreCase = true)

    if (isFuncionario || isAdmin) {
        // ✅ ambos recebem as notificações que vão para "funcionarios"
        messaging.subscribeToTopic("funcionarios")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "FCM: Subscrito em funcionarios ✅")
                else Log.e(TAG, "FCM: Falhou subscribe funcionarios", task.exception)
            }
    } else {
        messaging.unsubscribeFromTopic("funcionarios")
    }
}

