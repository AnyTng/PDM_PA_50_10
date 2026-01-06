package ipca.app.lojasas.ui.funcionario.menu.profile

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.AuditLogger

data class CollaboratorItem(
    val id: String, // ID do documento (ex: numMecanografico)
    val uid: String, // UID da autenticação
    val nome: String,
    val email: String,
    val role: String // "Admin" ou "Funcionario"
)

data class CollaboratorsListState(
    val collaborators: List<CollaboratorItem> = emptyList(),
    val filteredList: List<CollaboratorItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class CollaboratorsListViewModel : ViewModel() {
    var uiState = mutableStateOf(CollaboratorsListState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadCollaborators()
    }

    private fun loadCollaborators() {
        // Escuta em tempo real para atualizações automáticas
        db.collection("funcionarios")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = value?.documents?.mapNotNull { doc ->
                    // Proteção contra documentos mal formatados
                    try {
                        CollaboratorItem(
                            id = doc.id,
                            uid = doc.getString("uid") ?: "",
                            nome = doc.getString("nome") ?: "Sem Nome",
                            email = doc.getString("email") ?: "",
                            role = doc.getString("role") ?: "Funcionario"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                uiState.value = uiState.value.copy(collaborators = list, isLoading = false)
                filterList() // Atualiza a lista filtrada
            }
    }

    fun onSearchChange(query: String) {
        uiState.value = uiState.value.copy(searchQuery = query)
        filterList()
    }

    private fun filterList() {
        val query = uiState.value.searchQuery.trim().lowercase()
        val currentList = uiState.value.collaborators

        if (query.isEmpty()) {
            uiState.value = uiState.value.copy(filteredList = currentList)
        } else {
            val filtered = currentList.filter {
                it.nome.lowercase().contains(query) ||
                        it.id.lowercase().contains(query) ||
                        (if(it.role.equals("Admin", ignoreCase = true)) "administrador" else "colaborador").contains(query)
            }
            uiState.value = uiState.value.copy(filteredList = filtered)
        }
    }

    fun deleteCollaborator(item: CollaboratorItem, onSuccess: () -> Unit) {
        if (item.uid == currentUserId) return // Não se pode apagar a si próprio

        // Apagar da coleção 'funcionarios'
        db.collection("funcionarios").document(item.id).delete()
            .addOnSuccessListener {
                // Tenta apagar da coleção auxiliar 'users' se existir, mas não falha se não conseguir
                if (item.uid.isNotEmpty()) {
                    db.collection("users").document(item.uid).delete().addOnFailureListener { e ->
                        Log.w("CollaboratorsList", "Erro ao apagar user auth doc: ${e.message}")
                    }
                }
                val details = buildString {
                    if (item.nome.isNotBlank()) append("Nome: ").append(item.nome)
                    if (item.email.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("Email: ").append(item.email)
                    }
                }.takeIf { it.isNotBlank() }
                AuditLogger.logAction(
                    action = "Removeu colaborador",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = "Erro ao apagar: ${e.message}")
            }
    }

    fun promoteToAdmin(item: CollaboratorItem) {
        db.collection("funcionarios").document(item.id)
            .update("role", "Admin")
            .addOnSuccessListener {
                val details = if (item.nome.isNotBlank()) "Nome: ${item.nome}" else null
                AuditLogger.logAction(
                    action = "Promoveu colaborador a Admin",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = "Erro ao promover: ${e.message}")
            }
    }

    fun demoteToFuncionario(item: CollaboratorItem) {
        // REGRA: Admin não pode remover o seu próprio admin
        if (item.uid == currentUserId) {
            uiState.value = uiState.value.copy(error = "Não pode remover as suas próprias permissões de Admin.")
            return
        }

        db.collection("funcionarios").document(item.id)
            .update("role", "Funcionario")
            .addOnSuccessListener {
                val details = if (item.nome.isNotBlank()) "Nome: ${item.nome}" else null
                AuditLogger.logAction(
                    action = "Despromoveu colaborador",
                    entity = "funcionario",
                    entityId = item.id,
                    details = details
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = "Erro ao despromover: ${e.message}")
            }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }
}
