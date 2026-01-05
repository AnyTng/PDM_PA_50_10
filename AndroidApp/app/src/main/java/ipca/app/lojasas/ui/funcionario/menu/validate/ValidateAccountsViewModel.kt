package ipca.app.lojasas.ui.funcionario.menu.validate

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.utils.AccountValidity
import java.util.Date

// Estado da UI
data class ValidateState(
    val pendingAccounts: List<ApoiadoSummary> = emptyList(),
    val selectedApoiadoDetails: ApoiadoDetails? = null,
    val apoaidoDocuments: List<DocumentSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Resumo para a lista
data class ApoiadoSummary(
    val id: String,
    val nome: String,
    val dataPedido: Date?
)

// Detalhes completos para o pop-up
data class ApoiadoDetails(
    val id: String,
    val nome: String,
    val email: String,
    val contacto: String,

    // Identificação Civil
    val documentNumber: String, // Substitui o antigo 'nif'
    val documentType: String,   // "NIF" ou "Passaporte"
    val morada: String,

    // Relação com IPCA
    val tipo: String, // Estudante, Funcionário, etc.
    val dadosIncompletos: Boolean,

    // Campos do Formulário "CompleteData"
    val nacionalidade: String,
    val dataNascimento: Date?,
    val curso: String?,
    val grauEnsino: String?,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String?,
    val necessidades: List<String>
)

// Resumo dos documentos submetidos
data class DocumentSummary(
    val typeTitle: String,
    val fileName: String,
    val url: String,
    val date: Date?,
    val entrega: Int
)

class ValidateAccountsViewModel : ViewModel() {

    var uiState = mutableStateOf(ValidateState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        loadPendingAccounts()
    }

    fun loadPendingAccounts() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados")
            .whereEqualTo("estadoConta", "Analise")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    ApoiadoSummary(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sem Nome",
                        dataPedido = null // Pode-se adicionar data de pedido se existir no doc
                    )
                }
                uiState.value = uiState.value.copy(pendingAccounts = list, isLoading = false)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
    }

    fun selectApoiado(apoiadoId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados").document(apoiadoId).get()
            .addOnSuccessListener { doc ->

                // Tentar ler a data de nascimento como Timestamp (formato Firestore)
                val dtNasc = try {
                    doc.getTimestamp("dataNascimento")?.toDate()
                } catch (e: Exception) { null }

                val details = ApoiadoDetails(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "",
                    email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "",
                    contacto = doc.getString("contacto") ?: "",

                    // Lê o tipo de documento e o número guardados no perfil
                    documentNumber = doc.getString("documentNumber") ?: "",
                    documentType = doc.getString("documentType") ?: "NIF",

                    morada = "${doc.getString("morada")}, ${doc.getString("codPostal")}",
                    tipo = doc.getString("relacaoIPCA") ?: "N/A",
                    dadosIncompletos = doc.getBoolean("dadosIncompletos") ?: false,

                    // Lê os dados extra do formulário de preenchimento
                    nacionalidade = doc.getString("nacionalidade") ?: "—",
                    dataNascimento = dtNasc,
                    curso = doc.getString("curso"),
                    grauEnsino = doc.getString("graoEnsino"),
                    apoioEmergencia = doc.getBoolean("apoioEmergenciaSocial") ?: false,
                    bolsaEstudos = doc.getBoolean("bolsaEstudos") ?: false,
                    valorBolsa = doc.getString("valorBolsa"),
                    necessidades = (doc.get("necessidade") as? List<String>) ?: emptyList()
                )

                // Buscar documentos submetidos (ordenados por entrega descrescente)
                db.collection("apoiados").document(apoiadoId)
                    .collection("Submissoes")
                    .orderBy("numeroEntrega", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { docsSnapshot ->
                        val docsList = docsSnapshot.documents.map { fileDoc ->
                            DocumentSummary(
                                typeTitle = fileDoc.getString("typeTitle") ?: "Doc",
                                fileName = fileDoc.getString("fileName") ?: "",
                                url = fileDoc.getString("storagePath") ?: "",
                                date = fileDoc.getLong("date")?.let { Date(it) },
                                entrega = fileDoc.getLong("numeroEntrega")?.toInt() ?: 1
                            )
                        }
                            // Ordenação secundária por data em memória
                            .sortedWith(compareByDescending<DocumentSummary> { it.entrega }.thenByDescending { it.date })

                        uiState.value = uiState.value.copy(
                            selectedApoiadoDetails = details,
                            apoaidoDocuments = docsList,
                            isLoading = false
                        )
                    }
                    .addOnFailureListener {
                        uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar documentos: ${it.message}")
                    }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar perfil: ${it.message}")
            }
    }

    fun clearSelection() {
        uiState.value = uiState.value.copy(selectedApoiadoDetails = null, apoaidoDocuments = emptyList())
    }

    // --- AÇÕES DE VALIDAÇÃO ---

    fun approveAccount(apoiadoId: String, onSuccess: () -> Unit) {
        getCurrentFuncionarioId { funcionarioId ->
            // ✅ A validade é atribuída na submissão do formulário.
            // Porém, para contas antigas (submetidas antes desta alteração),
            // podemos não ter "validadeConta" ainda. Nesse caso, atribuimos
            // aqui apenas como fallback para não deixar a conta sem validade.
            db.collection("apoiados").document(apoiadoId).get()
                .addOnSuccessListener { doc ->
                    val updates = hashMapOf<String, Any>(
                        "estadoConta" to "Aprovado",
                        "validadoPor" to funcionarioId,
                        "dataValidacao" to Date()
                    )

                    val hasValidity = (doc.getTimestamp("validadeConta") != null) || (doc.get("validadeConta") != null)
                    if (!hasValidity) {
                        updates["validadeConta"] = AccountValidity.nextAugust31()
                    }

                    updateApoiadoStatus(apoiadoId, updates, onSuccess)
                }
                .addOnFailureListener {
                    // Se falhar a leitura, aprovamos na mesma sem mexer na validade.
                    val updates = hashMapOf<String, Any>(
                        "estadoConta" to "Aprovado",
                        "validadoPor" to funcionarioId,
                        "dataValidacao" to Date()
                    )
                    updateApoiadoStatus(apoiadoId, updates, onSuccess)
                }
        }
    }

    fun denyAccount(apoiadoId: String, reason: String, onSuccess: () -> Unit) {
        getCurrentFuncionarioId { funcionarioId ->
            val denialData = hashMapOf(
                "motivo" to reason,
                "negadoPor" to funcionarioId,
                "data" to Date()
            )
            // Guarda o histórico da negação
            db.collection("apoiados").document(apoiadoId)
                .collection("JustificacoesNegacao")
                .add(denialData)
                .addOnSuccessListener {
                    val updates = hashMapOf<String, Any>(
                        "estadoConta" to "Negado",
                        "negadoPor" to funcionarioId
                    )
                    updateApoiadoStatus(apoiadoId, updates, onSuccess)
                }
        }
    }

    fun blockAccount(apoiadoId: String, onSuccess: () -> Unit) {
        getCurrentFuncionarioId { funcionarioId ->
            val updates = hashMapOf<String, Any>(
                "estadoConta" to "Bloqueado",
                "bloqueadoPor" to funcionarioId
            )
            updateApoiadoStatus(apoiadoId, updates, onSuccess)
        }
    }

    private fun updateApoiadoStatus(id: String, updates: Map<String, Any>, onSuccess: () -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("apoiados").document(id).update(updates)
            .addOnSuccessListener {
                loadPendingAccounts()
                uiState.value = uiState.value.copy(selectedApoiadoDetails = null)
                onSuccess()
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${it.message}")
            }
    }

    private fun getCurrentFuncionarioId(onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("funcionarios").whereEqualTo("uid", uid).get()
            .addOnSuccessListener {
                if (!it.isEmpty) onResult(it.documents[0].id)
                else onResult("unknown_staff")
            }
    }

    fun getFileUri(path: String, onResult: (android.net.Uri?) -> Unit) {
        com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(path).downloadUrl
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }
}