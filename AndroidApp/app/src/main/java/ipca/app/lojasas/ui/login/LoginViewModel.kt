package ipca.app.lojasas.ui.login

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.data.UserRoleRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

// Definição da TAG localmente para logs
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
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Email is required")
            return
        }

        if (uiState.value.password.isNullOrEmpty()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Password is required")
            return
        }

        val email = uiState.value.email!!.trim()
        val auth: FirebaseAuth = Firebase.auth
        auth.signInWithEmailAndPassword(
            email,
            uiState.value.password!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    resolveRoleAndProceed(email, onLoginSuccess)
                } else {
                    // If sign in fails
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Wrong password or no internet connection")
                }
            }
    }

    private fun resolveRoleAndProceed(
        email: String,
        onLoginSuccess:(UserRole)->Unit
    ) {
        UserRoleRepository.fetchUserRoleByEmail(
            email = email,
            onSuccess = { role ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = null
                )
                onLoginSuccess(role)
            },
            onNotFound = {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Conta sem perfil associado."
                )
            },
            onError = { exception ->
                Log.e(TAG, "Erro ao obter perfil do utilizador", exception)
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Não foi possível obter o tipo de utilizador."
                )
            }
        )
    }
}
