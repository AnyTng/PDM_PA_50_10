package ipca.app.lojasas.ui.apoiado.formulario.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.storage
import java.util.UUID

// ... (DocumentItem e UploadedFile mantêm-se iguais) ...
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
    val customDescription: String? = null
)

// ... (SubmissionState mantém-se igual) ...
data class SubmissionState(
    val docTypes: List<DocumentItem> = listOf(
        DocumentItem("despesas", "Despesas Permanentes", "Habitação, saúde, transportes.", isMandatory = true),
        DocumentItem("rendimentos", "Rendimentos", "Recibos de vencimento/pensões.", isMandatory = true),
        DocumentItem("extratos", "Extratos Bancários", "Extratos dos últimos 3 meses.", isMandatory = true),
        DocumentItem("internacional", "Estatuto Internacional/PALOP", "Apenas se aplicável.", isMandatory = false),
        DocumentItem("outros", "Outros Documentos", "Outros comprovativos relevantes.", isMandatory = false)
    ),
    val uploadedFiles: List<UploadedFile> = emptyList(),
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

    // ... (loadSubmissionStatus e listenToUploadedFiles mantêm-se iguais) ...
    fun loadSubmissionStatus() {
        val user = auth.currentUser ?: return
        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val numMecanografico = documents.documents[0].id
                    listenToUploadedFiles(numMecanografico)
                }
            }
    }

    private fun listenToUploadedFiles(numMecanografico: String) {
        db.collection("apoiados").document(numMecanografico)
            .collection("Submissoes").document("Entrega1")
            .collection("ficheiros")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    uiState.value = uiState.value.copy(error = "Erro ao carregar: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val files = snapshot.documents.mapNotNull { doc ->
                        try {
                            UploadedFile(
                                id = doc.id,
                                typeId = doc.getString("typeId") ?: "",
                                typeTitle = doc.getString("typeTitle") ?: "Desconhecido",
                                fileName = doc.getString("fileName") ?: "Sem nome",
                                storagePath = doc.getString("storagePath") ?: "",
                                date = doc.getLong("date") ?: 0L,
                                customDescription = doc.getString("customDescription")
                            )
                        } catch (e: Exception) { null }
                    }
                    uiState.value = uiState.value.copy(uploadedFiles = files)
                }
            }
    }

    fun hasAllMandatoryFiles(): Boolean {
        val state = uiState.value
        val mandatoryTypes = state.docTypes.filter { it.isMandatory }.map { it.id }
        return mandatoryTypes.all { mandatoryId ->
            state.uploadedFiles.any { it.typeId == mandatoryId }
        }
    }

    // ... (uploadDocument e performUpload mantêm-se iguais) ...
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
            .addOnFailureListener {
                uiState.value = uiState.value.copy(uploadProgress = false, error = "Erro ao buscar perfil.")
            }
    }

    private fun performUpload(context: Context, uri: Uri, docItem: DocumentItem, numMecanografico: String, customDesc: String?) {
        val originalName = getFileName(context, uri) ?: "doc_${System.currentTimeMillis()}"
        val uniqueName = "${UUID.randomUUID()}_$originalName"
        val storageRef = storage.reference.child("$numMecanografico/${docItem.id}/$uniqueName")

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
                    "date" to System.currentTimeMillis()
                )

                db.collection("apoiados").document(numMecanografico)
                    .collection("Submissoes").document("Entrega1")
                    .collection("ficheiros")
                    .add(fileData)
                    .addOnSuccessListener {
                        uiState.value = uiState.value.copy(uploadProgress = false)
                    }
                    .addOnFailureListener { e ->
                        uiState.value = uiState.value.copy(uploadProgress = false, error = "Erro BD: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(uploadProgress = false, error = "Erro Upload: ${e.message}")
            }
    }

    // --- NOVA FUNÇÃO PARA FINALIZAR A SUBMISSÃO ---
    fun finalizeSubmission(onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return

        // Ativa o loading
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id

                    // Atualiza os campos na BD:
                    // 1. faltaDocumentos -> false (já não falta nada)
                    // 2. estadoConta -> "Analise" (pedido explícito)
                    val updates = hashMapOf<String, Any>(
                        "faltaDocumentos" to false,
                        "estadoConta" to "Analise"
                    )

                    db.collection("apoiados").document(docId)
                        .update(updates)
                        .addOnSuccessListener {
                            uiState.value = uiState.value.copy(isLoading = false, submissionSuccess = true)
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao finalizar: ${e.message}")
                        }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não encontrado")
                }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro de rede: ${it.message}")
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