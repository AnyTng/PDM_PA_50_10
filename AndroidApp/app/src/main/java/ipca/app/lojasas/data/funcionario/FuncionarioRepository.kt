package ipca.app.lojasas.data.funcionario

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuncionarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun fetchFuncionarioIdByUid(
        uid: String,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        firestore.collection("funcionarios")
            .whereEqualTo("uid", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val id = snapshot.documents.firstOrNull()?.id
                onSuccess(id)
            }
            .addOnFailureListener { onError(it) }
    }
}
