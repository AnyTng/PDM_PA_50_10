package ipca.app.lojasas.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class FuncionarioInfo(
    val id: String,
    val nome: String,
    val email: String,
    val role: String
)

object AuditLogger {
    private const val TAG = "AuditLogger"
    private const val FUNCIONARIOS_COLLECTION = "funcionarios"
    private const val HISTORY_COLLECTION = "historico"

    private val firestore = FirebaseFirestore.getInstance()

    private var cachedUid: String? = null
    private var cachedInfo: FuncionarioInfo? = null
    private var isResolving = false
    private val pendingCallbacks = mutableListOf<(FuncionarioInfo?) -> Unit>()

    fun logAction(
        action: String,
        entity: String,
        entityId: String? = null,
        details: String? = null
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        resolveFuncionario(user.uid) { info ->
            if (info == null) return@resolveFuncionario

            val data = mutableMapOf<String, Any>(
                "action" to action,
                "entity" to entity,
                "timestamp" to FieldValue.serverTimestamp(),
                "funcionarioId" to info.id,
                "funcionarioNome" to info.nome,
                "funcionarioEmail" to info.email,
                "funcionarioRole" to info.role,
                "uid" to user.uid
            )

            if (!entityId.isNullOrBlank()) {
                data["entityId"] = entityId
            }
            if (!details.isNullOrBlank()) {
                data["details"] = details
            }

            firestore.collection(FUNCIONARIOS_COLLECTION)
                .document(info.id)
                .collection(HISTORY_COLLECTION)
                .add(data)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao gravar historico", e)
                }
        }
    }

    private fun resolveFuncionario(uid: String, onResult: (FuncionarioInfo?) -> Unit) {
        val cached = cachedInfo
        if (cached != null && cachedUid == uid) {
            onResult(cached)
            return
        }

        pendingCallbacks.add(onResult)
        if (isResolving) return
        isResolving = true

        firestore.collection(FUNCIONARIOS_COLLECTION)
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                val info = doc?.let {
                    FuncionarioInfo(
                        id = it.id,
                        nome = it.getString("nome") ?: "",
                        email = it.getString("email") ?: "",
                        role = it.getString("role") ?: "Funcionario"
                    )
                }

                if (info != null) {
                    cachedUid = uid
                    cachedInfo = info
                }

                val callbacks = pendingCallbacks.toList()
                pendingCallbacks.clear()
                isResolving = false
                callbacks.forEach { it(info) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao resolver funcionario", e)
                val callbacks = pendingCallbacks.toList()
                pendingCallbacks.clear()
                isResolving = false
                callbacks.forEach { it(null) }
            }
    }
}
