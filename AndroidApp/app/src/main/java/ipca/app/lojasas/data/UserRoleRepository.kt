package ipca.app.lojasas.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.core.navigation.Screen

enum class UserRole {
    FUNCIONARIO,
    APOIADO,
    ADMIN // Adicionado o novo papel
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

        // 1) Verificar na coleção de funcionários (inclui Admins)
        firestore.collection(FUNCIONARIOS_COLLECTION)
            .whereEqualTo(FUNCIONARIO_EMAIL_FIELD, normalizedEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    // Verificar o campo 'role' no documento
                    val roleString = document.getString("role")

                    if (roleString != null && roleString.equals("Admin", ignoreCase = true)) {
                        onSuccess(UserRole.ADMIN)
                    } else {
                        onSuccess(UserRole.FUNCIONARIO)
                    }
                    return@addOnSuccessListener
                }

                // 2) Se não encontrar, verificar na coleção de apoiados
                firestore.collection(APOIADOS_COLLECTION)
                    .whereEqualTo(APOIADO_EMAIL_FIELD, normalizedEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { apoiadoSnapshot ->
                        if (!apoiadoSnapshot.isEmpty) {
                            onSuccess(UserRole.APOIADO)
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

// Define para onde cada utilizador vai após o login
fun UserRole.destination(): String = when (this) {
    UserRole.FUNCIONARIO -> Screen.FuncionarioHome.route
    UserRole.ADMIN -> Screen.FuncionarioHome.route // Admin também vai para a home (calendário), mas terá menu diferente
    UserRole.APOIADO -> Screen.ApoiadoHome.route
}
