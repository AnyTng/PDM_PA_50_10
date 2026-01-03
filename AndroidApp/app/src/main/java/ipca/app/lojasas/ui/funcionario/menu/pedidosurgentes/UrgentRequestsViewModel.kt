package ipca.app.lojasas.ui.funcionario.pedidosurgentes

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import java.util.Date

// 1. ADICIONAR O CAMPO cestaId AQUI
data class PedidoUrgenteItem(
    val id: String,
    val numeroMecanografico: String,
    val descricao: String,
    val estado: String,
    val dataSubmissao: Date? = null,
    val cestaId: String? = null // <--- IMPORTANTE
)

data class UrgentRequestsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val funcionarioId: String = "",
    val pedidos: List<PedidoUrgenteItem> = emptyList()
)

class UrgentRequestsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var uiState = mutableStateOf(UrgentRequestsState())
        private set

    private var listener: ListenerRegistration? = null

    init {
        loadFuncionarioId()
        listenPedidosUrgentes()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    private fun loadFuncionarioId() {
        val user = auth.currentUser
        if (user == null) {
            uiState.value = uiState.value.copy(error = "Utilizador não autenticado.", isLoading = false)
            return
        }

        db.collection("funcionarios")
            .whereEqualTo("uid", user.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                val id = doc?.id.orEmpty()
                uiState.value = uiState.value.copy(funcionarioId = id)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    private fun listenPedidosUrgentes() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        listener?.remove()
        listener = db.collection("pedidos_ajuda")
            .whereEqualTo("tipo", "Urgente")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val pedidos = snapshot?.documents.orEmpty().map { doc ->
                    val dataSubmissao = doc.getTimestamp("dataSubmissao")?.toDate() ?: (doc.get("dataSubmissao") as? Date)

                    // 2. LER O CAMPO cestaId DA BASE DE DADOS
                    PedidoUrgenteItem(
                        id = doc.id,
                        numeroMecanografico = doc.getString("numeroMecanografico")?.trim().orEmpty(),
                        descricao = doc.getString("descricao")?.trim().orEmpty(),
                        estado = doc.getString("estado")?.trim().orEmpty(),
                        dataSubmissao = dataSubmissao,
                        cestaId = doc.getString("cestaId") // <--- IMPORTANTE
                    )
                }.sortedByDescending { it.dataSubmissao ?: Date(0) }

                uiState.value = uiState.value.copy(isLoading = false, pedidos = pedidos)
            }
    }

    fun negarPedido(pedidoId: String, onDone: (Boolean) -> Unit = {}) {
        val funcId = uiState.value.funcionarioId
        val updates = mutableMapOf<String, Any>(
            "estado" to "Negado",
            "dataDecisao" to Date()
        )
        if (funcId.isNotBlank()) updates["funcionarioID"] = funcId

        db.collection("pedidos_ajuda").document(pedidoId)
            .update(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onDone(false)
            }
    }

    fun aprovarPedido(pedidoId: String, onDone: (Boolean) -> Unit = {}) {
        val funcId = uiState.value.funcionarioId
        val updates = mutableMapOf<String, Any>(
            "estado" to "Preparar_Apoio",
            "dataDecisao" to Date()
        )
        if (funcId.isNotBlank()) updates["funcionarioID"] = funcId

        db.collection("pedidos_ajuda").document(pedidoId)
            .update(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onDone(false)
            }
    }
}