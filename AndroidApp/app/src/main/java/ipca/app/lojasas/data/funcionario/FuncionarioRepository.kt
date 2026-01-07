package ipca.app.lojasas.data.funcionario

import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
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

    fun listenCollaborators(
        onSuccess: (List<CollaboratorItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = firestore.collection("funcionarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    try {
                        CollaboratorItem(
                            id = doc.id,
                            uid = doc.getString("uid") ?: "",
                            nome = doc.getString("nome") ?: "Sem Nome",
                            email = doc.getString("email") ?: "",
                            role = doc.getString("role") ?: "Funcionario"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(list)
            }

        return registration.asListenerHandle()
    }

    fun createFuncionarioProfile(
        input: FuncionarioProfileInput,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = input.numMecanografico.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing numMecanografico"))
            return
        }

        val userMap = hashMapOf(
            "uid" to input.uid,
            "numMecanografico" to input.numMecanografico,
            "nome" to input.nome,
            "contacto" to input.contacto,
            "documentNumber" to input.documentNumber,
            "documentType" to input.documentType,
            "morada" to input.morada,
            "codPostal" to input.codPostal,
            "email" to input.email,
            "role" to input.role,
            "mudarPass" to input.mudarPass
        )

        firestore.collection("funcionarios")
            .document(normalized)
            .set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateCollaboratorRole(
        collaboratorId: String,
        role: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = collaboratorId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing collaboratorId"))
            return
        }

        firestore.collection("funcionarios")
            .document(normalized)
            .update("role", role)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteCollaborator(
        collaboratorId: String,
        uid: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = collaboratorId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing collaboratorId"))
            return
        }

        firestore.collection("funcionarios")
            .document(normalized)
            .delete()
            .addOnSuccessListener {
                if (!uid.isNullOrBlank()) {
                    firestore.collection("users").document(uid).delete()
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }
}
