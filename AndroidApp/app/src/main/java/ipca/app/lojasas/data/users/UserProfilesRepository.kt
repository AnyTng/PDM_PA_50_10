package ipca.app.lojasas.data.users

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.UserRole
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfilesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun isNumMecanograficoAvailable(
        numMecanografico: String,
        onResult: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = numMecanografico.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing numMecanografico"))
            return
        }

        firestore.collection("funcionarios")
            .document(normalized)
            .get()
            .addOnSuccessListener { funcionarioDoc ->
                if (funcionarioDoc.exists()) {
                    onResult(false)
                    return@addOnSuccessListener
                }

                firestore.collection("apoiados")
                    .document(normalized)
                    .get()
                    .addOnSuccessListener { apoiadoDoc ->
                        onResult(!apoiadoDoc.exists())
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun createProfile(
        input: UserProfileInput,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
            "role" to input.role.name,
            "mudarPass" to false
        )

        val collectionName = if (input.role == UserRole.APOIADO) "apoiados" else "funcionarios"
        if (input.role == UserRole.APOIADO) {
            userMap["emailApoiado"] = input.email
            userMap["dadosIncompletos"] = true
        }

        firestore.collection(collectionName)
            .document(input.numMecanografico)
            .set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun fetchProfileByUid(
        uid: String,
        onSuccess: (UserProfile?) -> Unit,
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
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                if (doc != null) {
                    val roleString = doc.getString("role") ?: ""
                    val isAdmin = roleString.equals("Admin", ignoreCase = true)
                    val role = if (isAdmin) UserRole.ADMIN else UserRole.FUNCIONARIO
                    onSuccess(mapProfile(doc, role, isAdmin))
                    return@addOnSuccessListener
                }

                firestore.collection("apoiados")
                    .whereEqualTo("uid", normalized)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { apoiadoDocs ->
                        val apoiadoDoc = apoiadoDocs.documents.firstOrNull()
                        if (apoiadoDoc != null) {
                            onSuccess(mapProfile(apoiadoDoc, UserRole.APOIADO, false))
                        } else {
                            onSuccess(null)
                        }
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun fetchFuncionarioProfileByUid(
        uid: String,
        onSuccess: (UserProfile?) -> Unit,
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
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                if (doc == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                val roleString = doc.getString("role") ?: ""
                val isAdmin = roleString.equals("Admin", ignoreCase = true)
                val role = if (isAdmin) UserRole.ADMIN else UserRole.FUNCIONARIO
                onSuccess(mapProfile(doc, role, isAdmin))
            }
            .addOnFailureListener { onError(it) }
    }

    fun fetchApoiadoProfileByUid(
        uid: String,
        onSuccess: (UserProfile?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        firestore.collection("apoiados")
            .whereEqualTo("uid", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                if (doc == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                onSuccess(mapProfile(doc, UserRole.APOIADO, false))
            }
            .addOnFailureListener { onError(it) }
    }

    fun updateProfile(
        role: UserRole,
        docId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing docId"))
            return
        }

        val collectionName = if (role == UserRole.APOIADO) "apoiados" else "funcionarios"
        firestore.collection(collectionName)
            .document(normalized)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun deleteProfile(
        role: UserRole,
        docId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing docId"))
            return
        }

        val collectionName = if (role == UserRole.APOIADO) "apoiados" else "funcionarios"
        firestore.collection(collectionName)
            .document(normalized)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    private fun mapProfile(doc: DocumentSnapshot, role: UserRole, isAdmin: Boolean): UserProfile {
        val email = doc.getString("email") ?: doc.getString("emailApoiado") ?: ""
        val necessidades = (doc.get("necessidade") as? List<*>)?.mapNotNull { it as? String }
            ?: emptyList()

        return UserProfile(
            docId = doc.id,
            role = role,
            isAdmin = isAdmin,
            nome = doc.getString("nome") ?: "",
            contacto = doc.getString("contacto") ?: "",
            email = email,
            documentNumber = doc.getString("documentNumber") ?: "",
            documentType = doc.getString("documentType") ?: "NIF",
            morada = doc.getString("morada") ?: "",
            codPostal = doc.getString("codPostal") ?: "",
            nacionalidade = doc.getString("nacionalidade") ?: "",
            dataNascimento = snapshotDate(doc, "dataNascimento"),
            relacaoIPCA = doc.getString("relacaoIPCA") ?: "",
            curso = doc.getString("curso") ?: "",
            graoEnsino = doc.getString("graoEnsino") ?: "",
            apoioEmergencia = doc.getBoolean("apoioEmergenciaSocial") ?: false,
            bolsaEstudos = doc.getBoolean("bolsaEstudos") ?: false,
            valorBolsa = doc.getString("valorBolsa") ?: "",
            necessidades = necessidades,
            validadeConta = snapshotDate(doc, "validadeConta") ?: snapshotDate(doc, "validade")
        )
    }

    private fun snapshotDate(doc: DocumentSnapshot, field: String): Date? {
        return doc.getTimestamp(field)?.toDate()
            ?: (doc.get(field) as? Date)
    }
}
