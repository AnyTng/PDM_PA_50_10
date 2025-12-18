package ipca.app.lojasas.ui.funcionario.menu.apoiados

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

data class ApoiadoListState(
    val apoiados: List<ApoiadoItem> = emptyList(),
    val filteredApoiados: List<ApoiadoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentFilter: String = "Todos",
    val selectedApoiado: ApoiadoItem? = null // Para o modal de detalhes
)

data class ApoiadoItem(
    val id: String,
    val nome: String,
    val email: String,
    val rawStatus: String, // Estado real na BD (ex: "Falta_Documentos")
    val displayStatus: String, // Estado visual (ex: "Por Submeter")
    // Dados extra para o modal de detalhes
    val contacto: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val morada: String = "",
    val nacionalidade: String = "",
    val dataNascimento: java.util.Date? = null
)

class ApoiadosListViewModel : ViewModel() {

    var uiState = mutableStateOf(ApoiadoListState())
        private set

    private val db = Firebase.firestore

    init {
        loadApoiados()
    }

    fun loadApoiados() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("apoiados")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = value?.documents?.map { doc ->
                    val rawStatus = doc.getString("estadoConta") ?: ""

                    // Lógica de Agrupamento visual "Por Submeter"
                    val displayStatus = when (rawStatus) {
                        "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
                        else -> rawStatus
                    }

                    ApoiadoItem(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sem Nome",
                        email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "Sem Email",
                        rawStatus = rawStatus,
                        displayStatus = displayStatus,
                        contacto = doc.getString("contacto") ?: "",
                        documentType = doc.getString("documentType") ?: "Doc",
                        documentNumber = doc.getString("documentNumber") ?: "",
                        morada = "${doc.getString("morada")}, ${doc.getString("codPostal")}",
                        nacionalidade = doc.getString("nacionalidade") ?: "",
                        dataNascimento = doc.getTimestamp("dataNascimento")?.toDate()
                    )
                } ?: emptyList()

                uiState.value = uiState.value.copy(
                    apoiados = list,
                    isLoading = false
                )
                applyFilter(uiState.value.currentFilter)
            }
    }

    fun applyFilter(filter: String) {
        val all = uiState.value.apoiados
        val filtered = if (filter == "Todos") {
            all
        } else {
            all.filter { it.displayStatus.equals(filter, ignoreCase = true) }
        }
        uiState.value = uiState.value.copy(filteredApoiados = filtered, currentFilter = filter)
    }

    fun selectApoiado(item: ApoiadoItem?) {
        uiState.value = uiState.value.copy(selectedApoiado = item)
    }

    // --- AÇÕES DE ESTADO ---

    // 1. Desbloquear: Volta a ter de preencher dados
    fun unblockApoiado(id: String) {
        updateStatus(id, mapOf(
            "estadoConta" to "Correcao_Dados",
            "dadosIncompletos" to true,
            "bloqueadoPor" to null
        ))
    }

    // 2. Bloquear
    fun blockApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Bloqueado"))
    }

    // 3. Suspender
    fun suspendApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Suspenso"))
    }

    // 4. Voltar a Ajudar (Reativar) -> Fica Aprovado
    fun reactivateApoiado(id: String) {
        updateStatus(id, mapOf("estadoConta" to "Aprovado"))
    }

    private fun updateStatus(id: String, updates: Map<String, Any?>) {
        db.collection("apoiados").document(id).update(updates)
            .addOnFailureListener {
                // Opcional: Tratar erro
            }
    }
}