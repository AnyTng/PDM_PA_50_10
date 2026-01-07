package ipca.app.lojasas.data.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
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
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception)
                }
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
                if (uid != null) {
                    onSuccess(uid)
                } else {
                    onError(IllegalStateException("Missing user id"))
                }
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

    fun currentUserEmail(): String? = auth.currentUser?.email?.trim()

    fun currentUserId(): String? = auth.currentUser?.uid
}
