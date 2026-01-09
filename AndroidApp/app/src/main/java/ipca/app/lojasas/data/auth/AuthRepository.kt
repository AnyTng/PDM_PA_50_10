package ipca.app.lojasas.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import ipca.app.lojasas.widget.CestasWidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val appContext: Context
) {
    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Atualiza o widget automaticamente após login.
                    CestasWidgetUpdater.requestRefresh(appContext)
                    onSuccess()
                } else {
                    onError(task.exception)
                }
            }
    }

    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess() else onError(task.exception)
            }
    }

    fun createUser(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (Exception?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) onSuccess(uid)
                else onError(IllegalStateException("Missing user id"))
            }
            .addOnFailureListener { onError(it) }
    }

    fun createUserInSecondaryApp(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (Exception?) -> Unit
    ) {
        val secondaryAppName = "SecondaryAuthApp"
        val secondaryApp = try {
            FirebaseApp.getInstance(secondaryAppName)
        } catch (e: IllegalStateException) {
            val currentApp = FirebaseApp.getInstance()
            FirebaseApp.initializeApp(
                currentApp.applicationContext,
                currentApp.options,
                secondaryAppName
            )
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        secondaryAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    secondaryAuth.signOut()
                    onSuccess(uid)
                } else {
                    onError(IllegalStateException("Missing user id"))
                }
            }
            .addOnFailureListener { onError(it) }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email?.trim().orEmpty()
        if (user == null || email.isBlank()) {
            onError("Utilizador nao autenticado.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Erro ao atualizar palavra-passe.")
                    }
            }
            .addOnFailureListener {
                onError("Senha antiga incorreta.")
            }
    }

    fun deleteCurrentUser(
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError(IllegalStateException("Missing user"))
            return
        }

        user.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // ✅ Mantém a tua função original (importantíssima para admin)
    fun deleteUserByUid(
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing user id"))
            return
        }

        functions
            .getHttpsCallable("deleteAuthUser")
            .call(mapOf("uid" to normalized))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun signOut() {
        clearRoleCache()
        auth.signOut()

        // Atualiza o widget automaticamente após logout.
        CestasWidgetUpdater.requestRefresh(appContext)
    }

    fun currentUserEmail(): String? = auth.currentUser?.email?.trim()
    fun currentUserId(): String? = auth.currentUser?.uid

    private fun clearRoleCache() {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isBlank()) return
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("role_${email}").apply()
    }
}
