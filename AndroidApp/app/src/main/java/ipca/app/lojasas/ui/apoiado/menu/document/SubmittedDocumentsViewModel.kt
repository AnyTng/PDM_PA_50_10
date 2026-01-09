package ipca.app.lojasas.ui.apoiado.menu.document

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.apoiado.ApoiadoDocumentsRepository
import ipca.app.lojasas.data.apoiado.ApoiadoRepository
import ipca.app.lojasas.data.apoiado.SubmittedFile
import ipca.app.lojasas.data.auth.AuthRepository
import javax.inject.Inject

data class SubmittedDocumentsState(
    // Mapa onde a Chave é o numero da entrega e o Valor é a lista de ficheiros
    val groupedDocuments: Map<Int, List<SubmittedFile>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SubmittedDocumentsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apoiadoRepository: ApoiadoRepository,
    private val documentsRepository: ApoiadoDocumentsRepository
) : ViewModel() {

    var uiState = mutableStateOf(SubmittedDocumentsState())
        private set

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        val uid = authRepository.currentUserId().orEmpty()
        if (uid.isBlank()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        // 1. Obter o ID do Apoiado (numMecanografico) através do UID
        apoiadoRepository.fetchApoiadoIdByUid(
            uid = uid,
            onSuccess = { docId ->
                if (!docId.isNullOrBlank()) {
                    fetchSubmissions(docId)
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Perfil não encontrado")
                }
            },
            onError = {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
        )
    }

    private fun fetchSubmissions(apoiadoId: String) {
        documentsRepository.fetchSubmittedDocuments(
            apoiadoId = apoiadoId,
            onSuccess = { filesList ->
                val grouped = filesList
                    .sortedByDescending { it.date }
                    .groupBy { it.numeroEntrega }
                    .toSortedMap(compareByDescending { it })

                uiState.value = uiState.value.copy(
                    groupedDocuments = grouped,
                    isLoading = false
                )
            },
            onError = {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar documentos: ${it.message}"
                )
            }
        )
    }

    fun getFileUri(path: String, onResult: (Uri?) -> Unit) {
        documentsRepository.getFileUrl(path, onResult)
    }
}
