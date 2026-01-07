// Ficheiro: lojasas/ui/apoiado/formulario/document/DocumentSubmissionViewModel.kt

package ipca.app.lojasas.ui.apoiado.menu.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoDocumentsRepository
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.apoiado.UploadedFile
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.common.ListenerHandle
import javax.inject.Inject

// ... (Data Classes DocumentItem e SubmissionState mantêm-se iguais) ...
data class DocumentItem(
    val id: String,
    val title: String,
    val description: String,
    val isMandatory: Boolean = false
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

@HiltViewModel
class DocumentSubmissionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val documentsRepository: ApoiadoDocumentsRepository
) : ViewModel() {

    var uiState = mutableStateOf(SubmissionState())
        private set

    private var filesListener: ListenerHandle? = null

    fun loadSubmissionStatus() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) return
        uiState.value = uiState.value.copy(isLoading = true)

        apoiadoRepository.fetchApoiadoIdByUid(
            uid = uid,
            onSuccess = { apoiadoId ->
                if (apoiadoId.isNullOrBlank()) {
                    uiState.value = uiState.value.copy(isLoading = false)
                    return@fetchApoiadoIdByUid
                }
                calculateDeliveryNumber(apoiadoId)
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        )
    }

    // LÓGICA DE ENTREGA: Baseada no nº de negações
    private fun calculateDeliveryNumber(numMecanografico: String) {
        documentsRepository.fetchDeliveryNumber(
            apoiadoId = numMecanografico,
            onSuccess = { currentDelivery ->
                uiState.value = uiState.value.copy(currentDeliveryNumber = currentDelivery)
                listenToCurrentFiles(numMecanografico, currentDelivery)
            },
            onError = {
                uiState.value = uiState.value.copy(currentDeliveryNumber = 1, isLoading = false)
            }
        )
    }

    private fun listenToCurrentFiles(numMecanografico: String, deliveryNumber: Int) {
        filesListener?.remove()
        filesListener = documentsRepository.listenSubmissionFiles(
            apoiadoId = numMecanografico,
            deliveryNumber = deliveryNumber,
            onSuccess = { files ->
                uiState.value = uiState.value.copy(uploadedFiles = files, isLoading = false)
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        )
    }

    fun hasAllMandatoryFiles(): Boolean {
        val state = uiState.value
        val mandatoryTypes = state.docTypes.filter { it.isMandatory }.map { it.id }
        return mandatoryTypes.all { mandatoryId ->
            state.uploadedFiles.any { it.typeId == mandatoryId }
        }
    }

    fun uploadDocument(context: Context, uri: Uri, docType: DocumentItem, customDescription: String? = null) {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) return
        uiState.value = uiState.value.copy(uploadProgress = true, error = null)

        apoiadoRepository.fetchApoiadoIdByUid(
            uid = uid,
            onSuccess = { apoiadoId ->
                if (apoiadoId.isNullOrBlank()) {
                    uiState.value = uiState.value.copy(uploadProgress = false)
                    return@fetchApoiadoIdByUid
                }
                performUpload(context, uri, docType, apoiadoId, customDescription)
            },
            onError = {
                uiState.value = uiState.value.copy(uploadProgress = false)
            }
        )
    }

    private fun performUpload(context: Context, uri: Uri, docItem: DocumentItem, numMecanografico: String, customDesc: String?) {
        val originalName = getFileName(context, uri) ?: "doc_${System.currentTimeMillis()}"
        val currentDelivery = uiState.value.currentDeliveryNumber
        documentsRepository.uploadDocument(
            apoiadoId = numMecanografico,
            deliveryNumber = currentDelivery,
            docTypeId = docItem.id,
            docTypeTitle = docItem.title,
            originalName = originalName,
            uri = uri,
            customDescription = customDesc,
            onSuccess = {
                uiState.value = uiState.value.copy(uploadProgress = false)
            },
            onError = { message ->
                uiState.value = uiState.value.copy(uploadProgress = false, error = "Erro Upload: $message")
            }
        )
    }

    // FINALIZAR: Fecha a entrega e notifica o funcionário
    fun finalizeSubmission(onSuccess: () -> Unit) {
        val currentDelivery = uiState.value.currentDeliveryNumber

        uiState.value = uiState.value.copy(isLoading = true)

        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) {
            uiState.value = uiState.value.copy(isLoading = false)
            return
        }

        apoiadoRepository.fetchApoiadoIdByUid(
            uid = uid,
            onSuccess = { docId ->
                if (docId.isNullOrBlank()) {
                    uiState.value = uiState.value.copy(isLoading = false)
                    return@fetchApoiadoIdByUid
                }
                documentsRepository.finalizeSubmission(
                    apoiadoId = docId,
                    deliveryNumber = currentDelivery,
                    onSuccess = {
                        uiState.value = uiState.value.copy(isLoading = false, submissionSuccess = true)
                        onSuccess()
                    },
                    onError = {
                        uiState.value = uiState.value.copy(isLoading = false)
                    }
                )
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        )
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
        documentsRepository.getFileUrl(storagePath, onResult)
    }

    override fun onCleared() {
        filesListener?.remove()
        filesListener = null
        super.onCleared()
    }
}
