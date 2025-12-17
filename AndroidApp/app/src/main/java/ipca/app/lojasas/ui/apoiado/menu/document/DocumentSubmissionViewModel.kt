// Ficheiro: lojasas/ui/apoiado/formulario/document/DocumentSubmissionViewModel.kt

package ipca.app.lojasas.ui.apoiado.menu.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.Date
import java.util.UUID

// ... (Data Classes DocumentItem, UploadedFile, SubmissionState mantêm-se iguais) ...
data class DocumentItem(
    val id: String,
    val title: String,
    val description: String,
    val isMandatory: Boolean = false
)

data class UploadedFile(
    val id: String,
    val typeId: String,
    val typeTitle: String,
    val fileName: String,
    val storagePath: String,
    val date: Long,
    val customDescription: String? = null,
    val numeroEntrega: Int,
    val submetido: Boolean
)

data class SubmissionState(
    val docTypes: List<DocumentItem> = listOf(
        DocumentItem("despesas", "Despesas Permanentes", "Recibos de habitação, etc.", true),
        DocumentItem("rendimentos", "Rendimentos", "Recibos de vencimento/pensões.", true),
        DocumentItem("extratos", "Extratos Bancários", "Extratos dos últimos 3 meses.", true),
        DocumentItem("internacional", "Estatuto Internacional/PALOP", "Apenas se aplicável.", false),
        DocumentItem("outros", "Outros Documentos", "Outros comprovativos relevantes.", false)
    ),
    val uploadedFiles: List<UploadedFile> = emptyList(),
    val currentDeliveryNumber: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Boolean = false,
    val submissionSuccess: Boolean = false
)

class DocumentSubmissionViewModel : ViewModel() {

    var uiState = mutableStateOf(SubmissionState())
        private set

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    fun loadSubmissionStatus() {
        val user = auth.currentUser ?: return
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val numMecanografico = documents.documents[0].id
                    calculateDeliveryNumber(numMecanografico)
                }
            }
    }

    // LÓGICA DE ENTREGA: Baseada no nº de negações
    private fun calculateDeliveryNumber(numMecanografico: String) {
        db.collection("apoiados").document(numMecanografico)
            .collection("JustificacoesNegacao")
            .get()
            .addOnSuccessListener { snapshot ->
                val denialCount = snapshot.size()
                val currentDelivery = denialCount + 1

                uiState.value = uiState.value.copy(currentDeliveryNumber = currentDelivery)
                listenToCurrentFiles(numMecanografico, currentDelivery)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(currentDeliveryNumber = 1, isLoading = false)
            }
    }

    private fun listenToCurrentFiles(numMecanografico: String, deliveryNumber: Int) {
        db.collection("apoiados").document(numMecanografico)
            .collection("Submissoes")
            .whereEqualTo("numeroEntrega", deliveryNumber)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    uiState.value = uiState.value.copy(isLoading = false)
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
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.date }

                uiState.value = uiState.value.copy(uploadedFiles = files, isLoading = false)
            }
    }

    fun hasAllMandatoryFiles(): Boolean {
        val state = uiState.value
        val mandatoryTypes = state.docTypes.filter { it.isMandatory }.map { it.id }
        return mandatoryTypes.all { mandatoryId ->
            state.uploadedFiles.any { it.typeId == mandatoryId }
        }
    }

    fun uploadDocument(context: Context, uri: Uri, docType: DocumentItem, customDescription: String? = null) {
        val user = auth.currentUser ?: return
        uiState.value = uiState.value.copy(uploadProgress = true, error = null)

        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val numMecanografico = documents.documents[0].id
                    performUpload(context, uri, docType, numMecanografico, customDescription)
                }
            }
    }

    private fun performUpload(context: Context, uri: Uri, docItem: DocumentItem, numMecanografico: String, customDesc: String?) {
        val originalName = getFileName(context, uri) ?: "doc_${System.currentTimeMillis()}"
        val uniqueName = "${UUID.randomUUID()}_$originalName"
        val currentDelivery = uiState.value.currentDeliveryNumber

        val storageRef = storage.reference.child("$numMecanografico/Entrega$currentDelivery/${docItem.id}/$uniqueName")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                val path = storageRef.path
                val displayTitle = if (!customDesc.isNullOrEmpty()) customDesc else docItem.title

                val fileData = hashMapOf(
                    "typeId" to docItem.id,
                    "typeTitle" to displayTitle,
                    "customDescription" to customDesc,
                    "fileName" to originalName,
                    "storagePath" to path,
                    "date" to System.currentTimeMillis(),
                    "numeroEntrega" to currentDelivery,
                    "submetido" to false // Rascunho até finalizar
                )

                db.collection("apoiados").document(numMecanografico)
                    .collection("Submissoes")
                    .add(fileData)
                    .addOnSuccessListener {
                        uiState.value = uiState.value.copy(uploadProgress = false)
                    }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(uploadProgress = false, error = "Erro Upload: ${e.message}")
            }
    }

    // FINALIZAR: Fecha a entrega e notifica o funcionário
    fun finalizeSubmission(onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        val currentDelivery = uiState.value.currentDeliveryNumber

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    val submissoesRef = db.collection("apoiados").document(docId).collection("Submissoes")

                    submissoesRef.whereEqualTo("numeroEntrega", currentDelivery).get()
                        .addOnSuccessListener { batchSnapshot ->
                            val batch = db.batch()

                            // Marca todos os ficheiros como submetidos
                            for (doc in batchSnapshot.documents) {
                                batch.update(doc.reference, "submetido", true)
                                batch.update(doc.reference, "dataSubmissao", Date())
                            }

                            // Atualiza estado do utilizador para "Analise"
                            val userRef = db.collection("apoiados").document(docId)
                            batch.update(userRef, "faltaDocumentos", false)
                            batch.update(userRef, "estadoConta", "Analise")

                            batch.commit().addOnSuccessListener {
                                uiState.value = uiState.value.copy(isLoading = false, submissionSuccess = true)
                                onSuccess()
                            }
                        }
                }
            }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    fun getFileUrl(storagePath: String, onResult: (Uri?) -> Unit) {
        storage.reference.child(storagePath).downloadUrl
            .addOnSuccessListener { uri -> onResult(uri) }
            .addOnFailureListener { onResult(null) }
    }
}