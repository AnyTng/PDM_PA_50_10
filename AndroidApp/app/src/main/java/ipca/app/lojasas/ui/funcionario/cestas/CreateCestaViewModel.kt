package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.data.products.toProductOrNull
import java.util.Calendar
import java.util.Date

data class ApoiadoOption(
    val id: String,
    val nome: String,
    val ultimoLevantamento: Date? = null,
    val rawStatus: String = "",
    val displayStatus: String = ""
)

private fun mapApoiadoDisplayStatus(rawStatus: String): String {
    return when (rawStatus) {
        "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
        "Suspenso" -> "Apoio Pausado"
        else -> rawStatus
    }
}

data class CreateCestaState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,

    val funcionarioId: String = "",

    // Contexto (manual vs vindo de pedido urgente)
    val fromUrgent: Boolean = false,
    val pedidoId: String? = null,
    val pedidoDescricao: String? = null,

    // Beneficiário
    val apoiadoSelecionado: ApoiadoOption? = null,
    val apoiados: List<ApoiadoOption> = emptyList(),

    // Produtos
    val produtos: List<Product> = emptyList(),
    val produtosSelecionados: List<Product> = emptyList(),

    // Agendamento
    val usarAgora: Boolean = true,
    val dataAgendada: Date? = null,

    // Recorrência
    val recorrente: Boolean = false,
    val recorrenciaDias: String = "",

    // Observações
    val obs: String = ""
)

class CreateCestaViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private companion object {
        const val RECORRENCIA_DIAS_DEFAULT = 30
    }

    var uiState = mutableStateOf(CreateCestaState())
        private set

    private var initialized = false

    private var produtosListener: ListenerRegistration? = null
    private var apoiadosListener: ListenerRegistration? = null

    override fun onCleared() {
        super.onCleared()
        produtosListener?.remove()
        apoiadosListener?.remove()
    }

    /**
     * Chamar uma vez no Composable (via LaunchedEffect) para configurar o ecrã.
     */
    fun start(fromUrgent: Boolean, pedidoId: String?, apoiadoId: String?) {
        if (initialized) return
        initialized = true

        uiState.value = uiState.value.copy(
            isLoading = true,
            fromUrgent = fromUrgent,
            pedidoId = pedidoId
        )

        loadFuncionarioId()
        listenProdutosDisponiveis()
        listenApoiados()

        if (fromUrgent) {
            // Força "única" quando vem de pedido urgente
            uiState.value = uiState.value.copy(recorrente = false, recorrenciaDias = "")
            if (!pedidoId.isNullOrBlank()) loadPedido(pedidoId)
            if (!apoiadoId.isNullOrBlank()) loadApoiado(apoiadoId)
        }
    }

    private fun loadFuncionarioId() {
        val user = auth.currentUser
        if (user == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
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

    private fun listenProdutosDisponiveis() {
        produtosListener?.remove()
        produtosListener = db.collection("produtos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val produtos = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toProductOrNull() }
                    // Apenas produtos disponíveis (quando o campo existe)
                    .filter { p ->
                        ProductStatus.fromFirestore(p.estadoProduto) == ProductStatus.AVAILABLE
                    }
                    // Nao mostrar produtos fora de validade
                    .filter { p ->
                        val validade = p.validade
                        validade == null || !isExpired(validade)
                    }

                uiState.value = uiState.value.copy(isLoading = false, produtos = produtos)
            }
    }

    private fun isExpired(validade: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time
        return validade.before(startOfToday)
    }

    private fun listenApoiados() {
        apoiadosListener?.remove()
        apoiadosListener = db.collection("apoiados")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty()
                    // Não mostrar apoiados bloqueados
                    .filterNot { doc ->
                        val estado = doc.getString("estadoConta")?.trim().orEmpty()
                        estado.equals("Bloqueado", ignoreCase = true)
                    }
                    .map { doc ->
                        val rawStatus = doc.getString("estadoConta")?.trim().orEmpty()
                        val displayStatus = mapApoiadoDisplayStatus(rawStatus)
                        val ultimo = doc.getTimestamp("ultimoLevantamento")?.toDate()
                            ?: (doc.get("ultimoLevantamento") as? Date)
                        ApoiadoOption(
                            id = doc.id,
                            nome = doc.getString("nome")?.trim().orEmpty().ifBlank { doc.id },
                            ultimoLevantamento = ultimo,
                            rawStatus = rawStatus,
                            displayStatus = displayStatus
                        )
                    }
                    // Ordem por último levantamento (quem levantou há mais tempo primeiro)
                    .sortedWith(compareBy<ApoiadoOption> { it.ultimoLevantamento ?: Date(0) })

                uiState.value = uiState.value.copy(apoiados = list)
            }
    }

    private fun loadPedido(pedidoId: String) {
        db.collection("pedidos_ajuda").document(pedidoId)
            .get()
            .addOnSuccessListener { doc ->
                val desc = doc.getString("descricao")?.trim()
                uiState.value = uiState.value.copy(
                    pedidoDescricao = desc,
                    // Prefill da nota com a descrição (pode ser editada)
                    obs = uiState.value.obs.ifBlank { desc.orEmpty() }
                )
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    private fun loadApoiado(apoiadoId: String) {
        db.collection("apoiados").document(apoiadoId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val ultimo = doc.getTimestamp("ultimoLevantamento")?.toDate() ?: (doc.get("ultimoLevantamento") as? Date)
                val rawStatus = doc.getString("estadoConta")?.trim().orEmpty()
                val displayStatus = mapApoiadoDisplayStatus(rawStatus)
                val option = ApoiadoOption(
                    id = doc.id,
                    nome = doc.getString("nome")?.trim().orEmpty().ifBlank { doc.id },
                    ultimoLevantamento = ultimo,
                    rawStatus = rawStatus,
                    displayStatus = displayStatus
                )
                uiState.value = uiState.value.copy(apoiadoSelecionado = option)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    fun selecionarApoiado(option: ApoiadoOption) {
        uiState.value = uiState.value.copy(apoiadoSelecionado = option)
    }

    fun setObs(value: String) {
        uiState.value = uiState.value.copy(obs = value)
    }

    fun setUsarAgora(value: Boolean) {
        uiState.value = uiState.value.copy(
            usarAgora = value,
            dataAgendada = if (value) Date() else uiState.value.dataAgendada
        )
    }

    fun setDataAgendada(date: Date) {
        uiState.value = uiState.value.copy(usarAgora = false, dataAgendada = date)
    }

    fun setRecorrente(value: Boolean) {
        // Quando vem de pedido urgente é sempre única
        if (uiState.value.fromUrgent) return

        uiState.value = uiState.value.copy(
            recorrente = value,
            recorrenciaDias = if (value) RECORRENCIA_DIAS_DEFAULT.toString() else ""
        )
    }

    fun setRecorrenciaDias(value: String) {
        uiState.value = uiState.value.copy(recorrenciaDias = value)
    }

    fun addProduto(prod: Product) {
        val current = uiState.value
        if (current.produtosSelecionados.any { it.id == prod.id }) return
        uiState.value = current.copy(produtosSelecionados = current.produtosSelecionados + prod)
    }

    fun removeProduto(productId: String) {
        val current = uiState.value
        uiState.value = current.copy(produtosSelecionados = current.produtosSelecionados.filterNot { it.id == productId })
    }

    fun submitCesta(onSuccess: () -> Unit) {
        val s = uiState.value

        val funcId = s.funcionarioId.trim()
        if (funcId.isBlank()) {
            uiState.value = s.copy(error = "Não foi possível identificar o funcionário.")
            return
        }

        val apoiado = s.apoiadoSelecionado
        if (apoiado == null) {
            uiState.value = s.copy(error = "Selecione um beneficiário.")
            return
        }

        if (s.produtosSelecionados.isEmpty()) {
            uiState.value = s.copy(error = "Selecione pelo menos 1 produto.")
            return
        }

        val agendada = if (s.usarAgora) Date() else s.dataAgendada
        if (agendada == null) {
            uiState.value = s.copy(error = "Selecione a data de entrega.")
            return
        }

        val recorrente = if (s.fromUrgent) false else s.recorrente
        val recDias: Int? = if (recorrente) RECORRENCIA_DIAS_DEFAULT else null

        uiState.value = s.copy(isSubmitting = true, error = null)

        val now = Date()
        val produtoIds = s.produtosSelecionados.map { it.id }

        // Criamos o ID da cesta antes para também o ligar às reservas de produtos.
        val cestaRef = db.collection("cestas").document()
        val cestaId = cestaRef.id

        val cestaMap = mutableMapOf<String, Any>(
            "apoiadoID" to apoiado.id,
            "funcionarioID" to funcId,
            "dataAtual" to now,
            "dataAgendada" to agendada,
            "dataRecolha" to agendada,
            "estadoCesta" to "Agendada",
            "produtos" to produtoIds,
            "obs" to s.obs,
            "tipoApoio" to (if (recorrente) "Recorrente" else "Unica"),
            "faltas" to 0,
            "origem" to (if (s.fromUrgent) "Urgente" else "Manual")
        )

        if (recDias != null) cestaMap["recorrenciaDias"] = recDias
        if (!s.pedidoId.isNullOrBlank()) cestaMap["pedidoUrgenteId"] = s.pedidoId!!

        // --- APENAS ESTA TRANSAÇÃO (A mais completa) ---
        db.runTransaction { txn ->
            // Leitura dos produtos (Firestore exige leituras antes de escritas)
            val produtoRefs = produtoIds.map { pid ->
                pid to db.collection("produtos").document(pid)
            }
            val produtoSnaps = produtoRefs.map { (pid, ref) ->
                pid to txn.get(ref)
            }

            // 1) Verifica produtos e disponibilidade
            produtoSnaps.forEach { (pid, snap) ->
                if (!snap.exists()) {
                    throw IllegalStateException("Produto não encontrado: $pid")
                }
                val estado = snap.getString("estadoProduto")?.trim().orEmpty()
                val isDisponivel = ProductStatus.fromFirestore(estado) == ProductStatus.AVAILABLE
                if (!isDisponivel) {
                    throw IllegalStateException("O produto '$pid' já não está disponível.")
                }
            }

            // 1.1) Reserva produtos
            produtoRefs.forEach { (_, ref) ->
                txn.update(
                    ref,
                    mapOf(
                        "estadoProduto" to ProductStatus.RESERVED.firestoreValue,
                        "cestaReservaId" to cestaId,
                        "reservadoEm" to now
                    )
                )
            }

            // 2) Criar cesta
            txn.set(cestaRef, cestaMap)

            // 3) Se for recorrente, guardar no apoiado a data agendada
            if (recorrente) {
                val apoioUpdates = mutableMapOf<String, Any>(
                    "dataLevantamentoAgendado" to agendada,
                    "recorrenciaDias" to recDias!!
                )
                txn.update(db.collection("apoiados").document(apoiado.id), apoioUpdates)
            }

            // 4) ATUALIZAR PEDIDO URGENTE (Isto é o que precisas!)
            val pedidoIdLocal = s.pedidoId
            if (s.fromUrgent && !pedidoIdLocal.isNullOrBlank()) {
                val updates = mutableMapOf<String, Any>(
                    "estado" to "Preparar_Apoio", // Mantém o estado aprovado
                    "cestaId" to cestaId,         // <--- ISTO FAZ O BOTÃO DESAPARECER
                    "funcionarioID" to funcId,
                    "dataDecisao" to now
                )
                // Nota: A coleção aqui tem de ser "pedidos_ajuda"
                txn.update(db.collection("pedidos_ajuda").document(pedidoIdLocal), updates)
            }

            null
        }
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isSubmitting = false)
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isSubmitting = false, error = e.message)
            }
    }
}
