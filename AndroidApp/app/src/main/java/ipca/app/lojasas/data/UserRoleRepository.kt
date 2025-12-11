package ipca.app.lojasas.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.UserRole.APOIADO
import ipca.app.lojasas.data.UserRole.FUNCIONARIO

enum class UserRole {
    FUNCIONARIO,
    APOIADO
}

object UserRoleRepository {

    private const val TAG = "UserRoleRepository"
    private const val FUNCIONARIOS_COLLECTION = "funcionarios"
    private const val APOIADOS_COLLECTION = "apoiados"
    private const val FUNCIONARIO_EMAIL_FIELD = "email"
    private const val APOIADO_EMAIL_FIELD = "emailApoiado"

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun fetchUserRoleByEmail(
        email: String,
        onSuccess: (UserRole) -> Unit,
        onNotFound: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalizedEmail = email.trim()

        // 1) Check funcionários
        firestore.collection(FUNCIONARIOS_COLLECTION)
            .whereEqualTo(FUNCIONARIO_EMAIL_FIELD, normalizedEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onSuccess(FUNCIONARIO)
                    return@addOnSuccessListener
                }

                // 2) If not found, check apoiados
                firestore.collection(APOIADOS_COLLECTION)
                    .whereEqualTo(APOIADO_EMAIL_FIELD, normalizedEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { apoiadoSnapshot ->
                        if (!apoiadoSnapshot.isEmpty) {
                            onSuccess(APOIADO)
                        } else {
                            onNotFound()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error checking apoiados", exception)
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error checking funcionarios", exception)
                onError(exception)
            }
    }
}

fun UserRole.destination(): String = when (this) {
    FUNCIONARIO -> "funcionarioHome"
    APOIADO -> "apoiadoHome"
}
