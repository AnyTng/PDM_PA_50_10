package ipca.app.lojasas.ui.apoiado.formulario.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

// Modelo de UI para os tipos de documento
data class DocumentItem(
    val id: String,
    val title: String,
    val description: String,
    val isMandatory: Boolean = false
)

// Modelo de Dados para o Ficheiro (Atualizado para a nova estrutura)
data class UploadedFile(
    val id: String,
    val typeId: String,
    val typeTitle: String,
    val fileName: String,
    val storagePath: String,
    val date: Long,
    val customDescription: String? = null,
    val numeroEntrega: Int,    // NOVO: Número da entrega (1, 2, 3...)
    val submetido: Boolean     // NOVO: Se este ficheiro já foi finalizado
)

data class SubmissionState(
    val docTypes: List<DocumentItem> = listOf(
        DocumentItem("despesas", "Despesas Permanentes", "Habitação, saúde, transportes.", isMandatory = true),
        DocumentItem("rendimentos", "Rendimentos", "Recibos de vencimento/pensões.", isMandatory = true),
        DocumentItem("extratos", "Extratos Bancários", "Extratos dos últimos 3 meses.", isMandatory = true),
        DocumentItem("internacional", "Estatuto Internacional/PALOP", "Apenas se aplicável.", isMandatory = false),
        DocumentItem("outros", "Outros Documentos", "Outros comprovativos relevantes.", isMandatory = false)
    ),
    val uploadedFiles: List<UploadedFile> = emptyList(), // Ficheiros da entrega ATUAL
    val currentDeliveryNumber: Int = 1, // Guarda o número da entrega atual
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

    // 1. CARREGAR STATUS E CALCULAR NÚMERO DA ENTREGA
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

    private fun calculateDeliveryNumber(numMecanografico: String) {
        // Vai à coleção 'Submissoes' (agora direta) e ordena por numeroEntrega decrescente
        db.collection("apoiados").document(numMecanografico)
            .collection("Submissoes")
            .orderBy("numeroEntrega", Query.Direction.DESCENDING)
            .limit(1) // Só precisamos do maior
            .get()
            .addOnSuccessListener { result ->
                var nextDelivery = 1

                if (!result.isEmpty) {
                    val lastDoc = result.documents[0]
                    val lastNum = lastDoc.getLong("numeroEntrega")?.toInt() ?: 1
                    val isSubmitted = lastDoc.getBoolean("submetido") ?: false

                    if (isSubmitted) {
                        // Se a última entrega já foi fechada, começamos uma nova (+1)
                        nextDelivery = lastNum + 1
                    } else {
                        // Se ainda está aberta (rascunho), continuamos na mesma
                        nextDelivery = lastNum
                    }
                }

                uiState.value = uiState.value.copy(currentDeliveryNumber = nextDelivery)

                // Começa a ouvir os ficheiros DESTA entrega específica
                listenToCurrentFiles(numMecanografico, nextDelivery)
            }
            .addOnFailureListener {
                // Se der erro ou não existir coleção, assume entrega 1
                uiState.value = uiState.value.copy(currentDeliveryNumber = 1, isLoading = false)
            }
    }

    // Ouve apenas os ficheiros da entrega atual para mostrar na lista
    private fun listenToCurrentFiles(numMecanografico: String, deliveryNumber: Int) {
        db.collection("apoiados").document(numMecanografico)
            .collection("Submissoes")
            .whereEqualTo("numeroEntrega", deliveryNumber)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    uiState.value = uiState.value.copy(isLoading = false)
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
                                customDescription = doc.getString("customDescription"),
                                numeroEntrega = doc.getLong("numeroEntrega")?.toInt() ?: 1,
                                submetido = doc.getBoolean("submetido") ?: false
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.date } // Ordena por data (mais recente primeiro)

                    uiState.value = uiState.value.copy(uploadedFiles = files, isLoading = false)
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

        // Caminho no Storage: apoiados/{id}/EntregaX/{docType}/nome
        val storageRef = storage.reference.child("$numMecanografico/Entrega$currentDelivery/${docItem.id}/$uniqueName")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                val path = storageRef.path
                val displayTitle = if (!customDesc.isNullOrEmpty()) customDesc else docItem.title

                // DADOS PARA A COLEÇÃO 'Submissoes' (Nova Estrutura)
                val fileData = hashMapOf(
                    "typeId" to docItem.id,
                    "typeTitle" to displayTitle,
                    "customDescription" to customDesc,
                    "fileName" to originalName,
                    "storagePath" to path,
                    "date" to System.currentTimeMillis(),
                    "numeroEntrega" to currentDelivery, // Campo Chave
                    "submetido" to false                // Ainda é rascunho
                )

                db.collection("apoiados").document(numMecanografico)
                    .collection("Submissoes") // Grava direto na coleção
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

    // FINALIZAR: Atualiza todos os documentos desta entrega para submetido=true
    fun finalizeSubmission(onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        val currentDelivery = uiState.value.currentDeliveryNumber

        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados").whereEqualTo("uid", user.uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    val submissoesRef = db.collection("apoiados").document(docId).collection("Submissoes")

                    // 1. Encontrar todos os ficheiros da entrega atual
                    submissoesRef.whereEqualTo("numeroEntrega", currentDelivery).get()
                        .addOnSuccessListener { batchSnapshot ->

                            val batch = db.batch()

                            // 2. Adicionar update para 'submetido=true' e 'dataSubmissao' no batch
                            for (doc in batchSnapshot.documents) {
                                batch.update(doc.reference, "submetido", true)
                                batch.update(doc.reference, "dataSubmissao", java.util.Date())
                            }

                            // 3. Adicionar update do estado do utilizador no batch
                            val userRef = db.collection("apoiados").document(docId)
                            batch.update(userRef, "faltaDocumentos", false)
                            batch.update(userRef, "estadoConta", "Analise")

                            // 4. Executar tudo atomicamente
                            batch.commit().addOnSuccessListener {
                                uiState.value = uiState.value.copy(isLoading = false, submissionSuccess = true)
                                onSuccess()
                            }.addOnFailureListener {
                                uiState.value = uiState.value.copy(isLoading = false, error = "Erro batch: ${it.message}")
                            }
                        }
                }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${it.message}")
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