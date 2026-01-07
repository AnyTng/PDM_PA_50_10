package ipca.app.lojasas.data.apoiado

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApoiadoDocumentsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val apoiadosCollection = firestore.collection("apoiados")

    fun fetchDeliveryNumber(
        apoiadoId: String,
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(1)
            return
        }

        apoiadosCollection.document(normalized)
            .collection("JustificacoesNegacao")
            .get()
            .addOnSuccessListener { snapshot ->
                val currentDelivery = snapshot.size() + 1
                onSuccess(currentDelivery)
            }
            .addOnFailureListener { onError(it) }
    }

    fun listenSubmissionFiles(
        apoiadoId: String,
        deliveryNumber: Int,
        onSuccess: (List<UploadedFile>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(emptyList())
            return ListenerHandle { }
        }

        val registration = apoiadosCollection.document(normalized)
            .collection("Submissoes")
            .whereEqualTo("numeroEntrega", deliveryNumber)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    if (error != null) onError(error)
                    return@addSnapshotListener
                }

                val files = snapshot.documents.mapNotNull { doc ->
                    try {
                        UploadedFile(
                            id = doc.id,
                            typeId = doc.getString("typeId") ?: "",
                            typeTitle = doc.getString("typeTitle") ?: "Desconhecido",
                            fileName = doc.getString("fileName") ?: "Sem nome",
                            storagePath = doc.getString("storagePath") ?: "",
                            date = doc.getLong("date") ?: 0L,
                            customDescription = doc.getString("customDescription"),
                            numeroEntrega = doc.getLong("numeroEntrega")?.toInt() ?: 1,
                            submetido = doc.getBoolean("submetido") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.date }

                onSuccess(files)
            }
        return registration.asListenerHandle()
    }

    fun uploadDocument(
        apoiadoId: String,
        deliveryNumber: Int,
        docTypeId: String,
        docTypeTitle: String,
        originalName: String,
        uri: Uri,
        customDescription: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onError("Perfil nao encontrado.")
            return
        }

        val safeName = originalName.ifBlank { "doc_${System.currentTimeMillis()}" }
        val uniqueName = "${UUID.randomUUID()}_$safeName"
        val storageRef = storage.reference.child(
            "$normalized/Entrega$deliveryNumber/$docTypeId/$uniqueName"
        )

        storageRef.putFile(uri)
            .addOnSuccessListener {
                val path = storageRef.path
                val displayTitle = if (!customDescription.isNullOrBlank()) {
                    customDescription
                } else {
                    docTypeTitle
                }

                val fileData = hashMapOf(
                    "typeId" to docTypeId,
                    "typeTitle" to displayTitle,
                    "customDescription" to customDescription,
                    "fileName" to safeName,
                    "storagePath" to path,
                    "date" to System.currentTimeMillis(),
                    "numeroEntrega" to deliveryNumber,
                    "submetido" to false
                )

                apoiadosCollection.document(normalized)
                    .collection("Submissoes")
                    .add(fileData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Erro ao guardar.")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Erro Upload.")
            }
    }

    fun finalizeSubmission(
        apoiadoId: String,
        deliveryNumber: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing apoiadoId"))
            return
        }

        val submissionsRef = apoiadosCollection.document(normalized).collection("Submissoes")
        submissionsRef.whereEqualTo("numeroEntrega", deliveryNumber).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()

                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "submetido", true)
                    batch.update(doc.reference, "dataSubmissao", Date())
                }

                val userRef = apoiadosCollection.document(normalized)
                batch.update(userRef, "faltaDocumentos", false)
                batch.update(userRef, "estadoConta", "Analise")

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun fetchSubmittedDocuments(
        apoiadoId: String,
        onSuccess: (List<SubmittedFile>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(emptyList())
            return
        }

        apoiadosCollection.document(normalized)
            .collection("Submissoes")
            .get()
            .addOnSuccessListener { result ->
                val files = result.documents.mapNotNull { doc ->
                    try {
                        SubmittedFile(
                            title = doc.getString("typeTitle")
                                ?: doc.getString("customDescription")
                                ?: "Documento",
                            fileName = doc.getString("fileName") ?: "",
                            date = doc.getLong("date")?.let { Date(it) } ?: Date(),
                            storagePath = doc.getString("storagePath") ?: "",
                            numeroEntrega = doc.getLong("numeroEntrega")?.toInt() ?: 1
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(files)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getFileUrl(path: String, onResult: (Uri?) -> Unit) {
        if (path.isBlank()) {
            onResult(null)
            return
        }
        storage.reference.child(path).downloadUrl
            .addOnSuccessListener { uri -> onResult(uri) }
            .addOnFailureListener { onResult(null) }
    }
}
