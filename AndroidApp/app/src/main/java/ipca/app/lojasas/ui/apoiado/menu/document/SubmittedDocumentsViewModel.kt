package ipca.app.lojasas.ui.apoiado.menu.document

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.Date

// Modelo de dados local para esta vista
data class SubmittedFile(
    val title: String,
    val fileName: String,
    val date: Date,
    val storagePath: String,
    val numeroEntrega: Int
)

data class SubmittedDocumentsState(
    // Mapa onde a Chave é o numero da entrega e o Valor é a lista de ficheiros
    val groupedDocuments: Map<Int, List<SubmittedFile>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class SubmittedDocumentsViewModel : ViewModel() {

    var uiState = mutableStateOf(SubmittedDocumentsState())
        private set

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        val user = auth.currentUser
        if (user == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true)

        // 1. Obter o ID do Apoiado (numMecanografico) através do UID
        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    fetchSubmissions(docId)
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Perfil não encontrado")
                }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
    }

    private fun fetchSubmissions(apoiadoId: String) {
        db.collection("apoiados").document(apoiadoId)
            .collection("Submissoes")
            // Opcional: Ordenar por data se tiver index criado, senão ordenamos em memória
            .get()
            .addOnSuccessListener { result ->
                val filesList = result.documents.mapNotNull { doc ->
                    try {
                        SubmittedFile(
                            title = doc.getString("typeTitle") ?: doc.getString("customDescription") ?: "Documento",
                            fileName = doc.getString("fileName") ?: "",
                            date = doc.getLong("date")?.let { Date(it) } ?: Date(),
                            storagePath = doc.getString("storagePath") ?: "",
                            numeroEntrega = doc.getLong("numeroEntrega")?.toInt() ?: 1
                        )
                    } catch (e: Exception) { null }
                }

                // 2. Agrupar por entrega e ordenar (Entrega mais recente primeiro)
                val grouped = filesList
                    .sortedByDescending { it.date } // Ordena ficheiros por data
                    .groupBy { it.numeroEntrega }
                    .toSortedMap(compareByDescending { it }) // Ordena as chaves (Entregas) de forma decrescente

                uiState.value = uiState.value.copy(
                    groupedDocuments = grouped,
                    isLoading = false
                )
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar documentos: ${it.message}")
            }
    }

    fun getFileUri(path: String, onResult: (Uri?) -> Unit) {
        if (path.isEmpty()) {
            onResult(null)
            return
        }
        storage.reference.child(path).downloadUrl
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }
}