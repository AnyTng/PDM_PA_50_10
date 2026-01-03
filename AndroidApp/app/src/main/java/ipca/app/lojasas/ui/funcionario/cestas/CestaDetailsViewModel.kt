package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.toProductOrNull
import java.util.Date

data class CestaDetailsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cesta: CestaDetails? = null,
    val apoiado: ApoiadoInfo? = null,
    val apoiadoError: String? = null,
    val isLoadingProdutos: Boolean = false,
    val produtos: List<Product> = emptyList(),
    val produtosError: String? = null,
    val produtosMissingIds: List<String> = emptyList()
)

data class CestaDetails(
    val id: String,
    val apoiadoId: String,
    val funcionarioId: String,
    val dataAgendada: Date? = null,
    val dataRecolha: Date? = null,
    val dataReagendada: Date? = null,
    val dataEntregue: Date? = null,
    val dataCancelada: Date? = null,
    val dataUltimaFalta: Date? = null,
    val estado: String = "",
    val faltas: Int = 0,
    val origem: String? = null,
    val tipoApoio: String? = null,
    val produtoIds: List<String> = emptyList(),
    val produtosCount: Int = 0,
    val observacoes: String? = null
)

data class ApoiadoInfo(
    val id: String,
    val nome: String,
    val email: String,
    val contacto: String,
    val documento: String,
    val morada: String,
    val nacionalidade: String
)

class CestaDetailsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var cestaListener: ListenerRegistration? = null
    private var apoiadoListener: ListenerRegistration? = null
    private var currentCestaId: String? = null
    private var currentApoiadoId: String? = null
    private var currentProdutoIds: List<String> = emptyList()
    private var produtosRequestToken: Int = 0

    var uiState = mutableStateOf(CestaDetailsState())
        private set

    fun observeCesta(cestaId: String) {
        val normalized = cestaId.trim()
        if (normalized.isBlank()) {
            uiState.value = CestaDetailsState(isLoading = false, error = "Cesta invalida.")
            return
        }
        if (currentCestaId == normalized) return

        currentCestaId = normalized
        uiState.value = uiState.value.copy(
            isLoading = true,
            error = null,
            cesta = null,
            apoiado = null,
            apoiadoError = null,
            isLoadingProdutos = false,
            produtos = emptyList(),
            produtosError = null,
            produtosMissingIds = emptyList()
        )

        cestaListener?.remove()
        cestaListener = db.collection("cestas").document(normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao carregar cesta."
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        error = "Cesta nao encontrada."
                    )
                    return@addSnapshotListener
                }

                val cesta = CestaDetails(
                    id = snapshot.id,
                    apoiadoId = snapshot.getString("apoiadoID")?.trim().orEmpty(),
                    funcionarioId = snapshot.getString("funcionarioID")?.trim().orEmpty(),
                    dataAgendada = snapshotDate(snapshot, "dataAgendada"),
                    dataRecolha = snapshotDate(snapshot, "dataRecolha"),
                    dataReagendada = snapshotDate(snapshot, "dataReagendada"),
                    dataEntregue = snapshotDate(snapshot, "dataEntregue"),
                    dataCancelada = snapshotDate(snapshot, "dataCancelada"),
                    dataUltimaFalta = snapshotDate(snapshot, "dataUltimaFalta"),
                    estado = snapshot.getString("estadoCesta")?.trim().orEmpty(),
                    faltas = (snapshot.getLong("faltas") ?: 0L).toInt(),
                    origem = snapshot.getString("origem"),
                    tipoApoio = snapshot.getString("tipoApoio"),
                    produtoIds = (snapshot.get("produtos") as? List<*>)?.mapNotNull { it as? String }
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    produtosCount = (snapshot.get("produtos") as? List<*>)?.size ?: 0,
                    observacoes = snapshot.getString("obs")?.trim()
                )

                uiState.value = uiState.value.copy(isLoading = false, error = null, cesta = cesta)

                fetchProdutos(cesta.produtoIds)

                val apoiadoId = cesta.apoiadoId
                if (apoiadoId.isBlank()) {
                    currentApoiadoId = null
                    apoiadoListener?.remove()
                    uiState.value = uiState.value.copy(
                        apoiado = null,
                        apoiadoError = "Sem apoiado associado."
                    )
                } else {
                    observeApoiado(apoiadoId)
                }
            }
    }

    private fun observeApoiado(apoiadoId: String) {
        if (currentApoiadoId == apoiadoId) return
        currentApoiadoId = apoiadoId
        apoiadoListener?.remove()
        uiState.value = uiState.value.copy(apoiado = null, apoiadoError = null)

        apoiadoListener = db.collection("apoiados").document(apoiadoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(
                        apoiadoError = error.message ?: "Erro ao carregar apoiado."
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    uiState.value = uiState.value.copy(apoiadoError = "Apoiado nao encontrado.")
                    return@addSnapshotListener
                }

                val docType = snapshot.getString("documentType")?.trim().orEmpty()
                val docNumber = snapshot.getString("documentNumber")?.trim().orEmpty()
                val documento = when {
                    docType.isNotBlank() && docNumber.isNotBlank() -> "$docType: $docNumber"
                    docType.isNotBlank() -> docType
                    docNumber.isNotBlank() -> docNumber
                    else -> "-"
                }

                val moradaParts = listOf(
                    snapshot.getString("morada"),
                    snapshot.getString("codPostal")
                )
                    .mapNotNull { it?.trim() }
                    .filter { it.isNotBlank() }
                val morada = if (moradaParts.isEmpty()) "-" else moradaParts.joinToString(", ")

                val apoiado = ApoiadoInfo(
                    id = snapshot.id,
                    nome = snapshot.getString("nome")?.trim().orEmpty(),
                    email = (snapshot.getString("email") ?: snapshot.getString("emailApoiado"))
                        ?.trim()
                        .orEmpty(),
                    contacto = snapshot.getString("contacto")?.trim().orEmpty(),
                    documento = documento,
                    morada = morada,
                    nacionalidade = snapshot.getString("nacionalidade")?.trim().orEmpty()
                )

                uiState.value = uiState.value.copy(apoiado = apoiado, apoiadoError = null)
            }
    }

    private fun fetchProdutos(produtoIds: List<String>) {
        val normalized = produtoIds.map { it.trim() }.filter { it.isNotBlank() }
        if (currentProdutoIds == normalized) return
        currentProdutoIds = normalized

        val requestToken = ++produtosRequestToken

        if (normalized.isEmpty()) {
            uiState.value = uiState.value.copy(
                isLoadingProdutos = false,
                produtos = emptyList(),
                produtosError = null,
                produtosMissingIds = emptyList()
            )
            return
        }

        uiState.value = uiState.value.copy(
            isLoadingProdutos = true,
            produtos = emptyList(),
            produtosError = null,
            produtosMissingIds = emptyList()
        )

        val uniqueIds = normalized.distinct()
        val chunks = uniqueIds.chunked(10)
        val productsById = mutableMapOf<String, Product>()
        var pending = chunks.size
        var errorMessage: String? = null

        fun finalizeIfDone() {
            if (requestToken != produtosRequestToken) return
            if (pending > 0) return
            val ordered = uniqueIds.mapNotNull { productsById[it] }
            val missing = uniqueIds.filterNot { productsById.containsKey(it) }
            uiState.value = uiState.value.copy(
                isLoadingProdutos = false,
                produtos = ordered,
                produtosError = errorMessage,
                produtosMissingIds = missing
            )
        }

        chunks.forEach { chunk ->
            db.collection("produtos")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        doc.toProductOrNull()?.let { product ->
                            productsById[product.id] = product
                        }
                    }
                    pending -= 1
                    finalizeIfDone()
                }
                .addOnFailureListener { e ->
                    if (errorMessage == null) {
                        errorMessage = e.message ?: "Erro ao carregar produtos."
                    }
                    pending -= 1
                    finalizeIfDone()
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cestaListener?.remove()
        apoiadoListener?.remove()
        cestaListener = null
        apoiadoListener = null
    }
}

private fun snapshotDate(snapshot: DocumentSnapshot, field: String): Date? {
    return snapshot.getTimestamp(field)?.toDate() ?: (snapshot.get(field) as? Date)
}
