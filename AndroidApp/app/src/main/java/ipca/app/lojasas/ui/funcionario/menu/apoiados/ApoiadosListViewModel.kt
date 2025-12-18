package ipca.app.lojasas.ui.funcionario.menu.apoiados

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream

enum class SortOrder {
    NOME_ASC, NOME_DESC,
    ID_ASC, ID_DESC,
    STATUS_ASC, STATUS_DESC
}

data class ApoiadoListState(
    val apoiados: List<ApoiadoItem> = emptyList(),
    val filteredApoiados: List<ApoiadoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentFilter: String = "Todos",
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NOME_ASC,
    val selectedApoiado: ApoiadoItem? = null
)

data class ApoiadoItem(
    val id: String,
    val nome: String,
    val email: String,
    val rawStatus: String, // Estado real na BD
    val displayStatus: String, // Estado visual (ex: "Apoio Pausado")
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

                    // --- MAPEAMENTO DE ESTADOS ---
                    val displayStatus = when (rawStatus) {
                        "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
                        "Suspenso" -> "Apoio Pausado" // Alterado aqui
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

                uiState.value = uiState.value.copy(apoiados = list, isLoading = false)
                applyFiltersAndSort()
            }
    }

    // --- FILTROS E PESQUISA ---

    fun onSearchQueryChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        applyFiltersAndSort()
    }

    fun applyFilter(filter: String) {
        uiState.value = uiState.value.copy(currentFilter = filter)
        applyFiltersAndSort()
    }

    fun applySort(order: SortOrder) {
        uiState.value = uiState.value.copy(sortOrder = order)
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = uiState.value
        var result = state.apoiados

        // 1. Filtrar por Estado (Dropdown)
        if (state.currentFilter != "Todos") {
            result = result.filter { it.displayStatus.equals(state.currentFilter, ignoreCase = true) }
        }

        // 2. Filtrar por Pesquisa
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            result = result.filter {
                it.nome.lowercase().contains(q) ||
                        it.id.lowercase().contains(q) ||
                        it.displayStatus.lowercase().contains(q)
            }
        }

        // 3. Ordenar
        result = when (state.sortOrder) {
            SortOrder.NOME_ASC -> result.sortedBy { it.nome.lowercase() }
            SortOrder.NOME_DESC -> result.sortedByDescending { it.nome.lowercase() }
            SortOrder.ID_ASC -> result.sortedBy { it.id.lowercase() }
            SortOrder.ID_DESC -> result.sortedByDescending { it.id.lowercase() }
            SortOrder.STATUS_ASC -> result.sortedBy { it.displayStatus.lowercase() }
            SortOrder.STATUS_DESC -> result.sortedByDescending { it.displayStatus.lowercase() }
        }

        uiState.value = state.copy(filteredApoiados = result)
    }

    fun selectApoiado(item: ApoiadoItem?) { uiState.value = uiState.value.copy(selectedApoiado = item) }

    // --- AÇÕES DE ESTADO ---

    fun unblockApoiado(id: String) { updateStatus(id, mapOf("estadoConta" to "Correcao_Dados", "dadosIncompletos" to true, "bloqueadoPor" to null)) }

    fun blockApoiado(id: String) { updateStatus(id, mapOf("estadoConta" to "Bloqueado")) }

    // Mantém na BD como "Suspenso", mas na UI mostra "Apoio Pausado"
    fun suspendApoiado(id: String) { updateStatus(id, mapOf("estadoConta" to "Suspenso")) }

    fun reactivateApoiado(id: String) { updateStatus(id, mapOf("estadoConta" to "Aprovado")) }

    private fun updateStatus(id: String, updates: Map<String, Any?>) {
        db.collection("apoiados").document(id).update(updates)
    }

    fun exportToCSV(context: Context) {
        val data = uiState.value.filteredApoiados
        val csvHeader = "ID,Nome,Email,Status,Contacto,Nacionalidade\n"
        val csvBody = data.joinToString("\n") {
            "${it.id},\"${it.nome}\",${it.email},${it.displayStatus},${it.contacto},${it.nacionalidade}"
        }
        val csvContent = csvHeader + csvBody

        try {
            val fileName = "apoiados_export_${System.currentTimeMillis()}.csv"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, fileName)
            FileOutputStream(file).use { it.write(csvContent.toByteArray()) }
            Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}