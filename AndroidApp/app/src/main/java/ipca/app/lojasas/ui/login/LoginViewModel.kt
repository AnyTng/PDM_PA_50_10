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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


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

    private fun resolveRoleAndProceed(email: String, onLoginSuccess: (UserRole) -> Unit) {
        UserRoleRepository.fetchUserRoleByEmail(
            email = email,
            onSuccess = { role ->
                uiState.value = uiState.value.copy(isLoading = false, error = null)

                // 1) Tópicos (funcionarios + admin)
                configureTopicForRole(role)

                // 2) Se for Apoiado, guarda token para notificações "1 a 1"
                if (role.toString().equals("Apoiado", ignoreCase = true)) {
                    saveApoiadoTokenToFirestore()
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

//Para notificações
private fun saveApoiadoTokenToFirestore() {
    val email = FirebaseAuth.getInstance().currentUser?.email?.trim()
    if (email.isNullOrEmpty()) {
        Log.e(TAG, "FCM Apoiado: FirebaseAuth email está vazio ❌")
        return
    }

    Log.d(TAG, "FCM Apoiado: vou guardar token para email=$email")

    val db = FirebaseFirestore.getInstance()

    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
            Log.d(TAG, "FCM Apoiado: token obtido ✅ (tamanho=${token.length})")

            fun saveTokenOnDoc(docId: String) {
                Log.d(TAG, "FCM Apoiado: a guardar token no doc apoiados/$docId/fcmTokens/$token")

                db.collection("apoiados")
                    .document(docId)
                    .collection("fcmTokens")
                    .document(token)
                    .set(mapOf("updatedAt" to FieldValue.serverTimestamp()))
                    .addOnSuccessListener { Log.d(TAG, "FCM Apoiado: token guardado ✅") }
                    .addOnFailureListener { e -> Log.e(TAG, "FCM Apoiado: ERRO a guardar token ❌", e) }
            }

            // 1) tenta por emailApoiado
            db.collection("apoiados")
                .whereEqualTo("emailApoiado", email)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    val doc = snap.documents.firstOrNull()
                    if (doc != null) {
                        Log.d(TAG, "FCM Apoiado: encontrado por emailApoiado ✅ docId=${doc.id}")
                        saveTokenOnDoc(doc.id)
                    } else {
                        Log.w(TAG, "FCM Apoiado: não encontrou por emailApoiado, vou tentar por 'email'…")

                        // 2) fallback por email
                        db.collection("apoiados")
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { snap2 ->
                                val doc2 = snap2.documents.firstOrNull()
                                if (doc2 != null) {
                                    Log.d(TAG, "FCM Apoiado: encontrado por email ✅ docId=${doc2.id}")
                                    saveTokenOnDoc(doc2.id)
                                } else {
                                    Log.e(TAG, "FCM Apoiado: NÃO encontrou apoiado com email=$email ❌")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "FCM Apoiado: erro na query por 'email' ❌", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM Apoiado: erro na query por 'emailApoiado' ❌", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "FCM Apoiado: erro a obter token FCM ❌", e)
        }
}

