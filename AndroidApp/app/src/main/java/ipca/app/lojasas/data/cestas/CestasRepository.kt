package ipca.app.lojasas.data.cestas

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.data.products.toProductOrNull
import ipca.app.lojasas.utils.AccountValidity
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CestasRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val cestasCollection = firestore.collection("cestas")
    private val produtosCollection = firestore.collection("produtos")
    private val apoiadosCollection = firestore.collection("apoiados")
    private val pedidosCollection = firestore.collection("pedidos_ajuda")
    private val funcionariosCollection = firestore.collection("funcionarios")

    fun listenCestas(
        onSuccess: (List<CestaItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = cestasCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents.orEmpty().map { doc ->
                val dataAgendada = snapshotDate(doc, "dataAgendada")
                val dataRecolha = snapshotDate(doc, "dataRecolha")
                val faltas = (doc.getLong("faltas") ?: 0L).toInt()
                CestaItem(
                    id = doc.id,
                    apoiadoId = doc.getString("apoiadoID")?.trim().orEmpty(),
                    funcionarioId = doc.getString("funcionarioID")?.trim().orEmpty(),
                    dataAgendada = dataAgendada,
                    dataRecolha = dataRecolha,
                    estado = doc.getString("estadoCesta")?.trim().orEmpty(),
                    faltas = faltas,
                    origem = doc.getString("origem"),
                    tipoApoio = doc.getString("tipoApoio")
                )
            }
            onSuccess(list)
        }
        return registration.asListenerHandle()
    }

    fun listenCestaDetails(
        cestaId: String,
        onSuccess: (CestaDetails?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = cestaId.trim()
        val registration = cestasCollection.document(normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onSuccess(null)
                    return@addSnapshotListener
                }

                val rawProdutos = (snapshot.get("produtos") as? List<*>) ?: emptyList<Any>()
                val produtoIds = rawProdutos
                    .mapNotNull { it as? String }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

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
                    produtoIds = produtoIds,
                    produtosCount = rawProdutos.size,
                    observacoes = snapshot.getString("obs")?.trim()
                )

                onSuccess(cesta)
            }
        return registration.asListenerHandle()
    }

    fun listenCestasForApoiado(
        apoiadoKeys: List<String>,
        onSuccess: (List<ApoiadoCesta>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val keys = apoiadoKeys.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (keys.isEmpty()) {
            onSuccess(emptyList())
            return ListenerHandle { }
        }

        val query = if (keys.size == 1) {
            cestasCollection.whereEqualTo("apoiadoID", keys.first())
        } else {
            cestasCollection.whereIn("apoiadoID", keys)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents.orEmpty().map { doc ->
                val rawProdutos = (doc.get("produtos") as? List<*>) ?: emptyList<Any?>()
                ApoiadoCesta(
                    id = doc.id,
                    dataRecolha = snapshotDate(doc, "dataRecolha"),
                    dataAgendada = snapshotDate(doc, "dataAgendada"),
                    estadoCesta = doc.getString("estadoCesta")?.trim().orEmpty(),
                    numeroItens = rawProdutos.size,
                    faltas = (doc.getLong("faltas") ?: 0L).toInt(),
                    origem = doc.getString("origem"),
                    pedidoUrgenteId = doc.getString("pedidoUrgenteId")
                )
            }
            onSuccess(list)
        }
        return registration.asListenerHandle()
    }

    fun listenApoiadoInfo(
        apoiadoId: String,
        onSuccess: (ApoiadoInfo?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = apoiadoId.trim()
        val registration = apoiadosCollection.document(normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    onSuccess(null)
                    return@addSnapshotListener
                }

                val info = buildApoiadoInfo(snapshot)
                onSuccess(info)
            }
        return registration.asListenerHandle()
    }

    fun fetchProdutosByIds(
        produtoIds: List<String>,
        onResult: (List<Product>, List<String>, String?) -> Unit
    ) {
        val normalized = produtoIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) {
            onResult(emptyList(), emptyList(), null)
            return
        }

        val chunks = normalized.chunked(10)
        val productsById = mutableMapOf<String, Product>()
        var pending = chunks.size
        var errorMessage: String? = null

        fun finalizeIfDone() {
            if (pending > 0) return
            val ordered = normalized.mapNotNull { productsById[it] }
            val missing = normalized.filterNot { productsById.containsKey(it) }
            onResult(ordered, missing, errorMessage)
        }

        chunks.forEach { chunk ->
            produtosCollection
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

    fun listenProdutosDisponiveis(
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = produtosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val produtos = snapshot?.documents.orEmpty()
                .mapNotNull { it.toProductOrNull() }
                .filter { product ->
                    ProductStatus.fromFirestore(product.estadoProduto) == ProductStatus.AVAILABLE
                }
                .filter { product ->
                    val validade = product.validade
                    validade == null || !isExpired(validade)
                }

            onSuccess(produtos)
        }
        return registration.asListenerHandle()
    }

    fun listenApoiados(
        onSuccess: (List<ApoiadoOption>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = apoiadosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents.orEmpty()
                .filterNot { doc ->
                    val estado = doc.getString("estadoConta")?.trim().orEmpty()
                    estado.equals("Bloqueado", ignoreCase = true)
                }
                .filter { doc ->
                    val validUntil = apoiadoValidUntil(doc)
                    validUntil != null && !AccountValidity.isExpired(validUntil)
                }
                .map { doc -> buildApoiadoOption(doc) }
                .sortedWith(compareBy<ApoiadoOption> { it.ultimoLevantamento ?: Date(0) })

            onSuccess(list)
        }
        return registration.asListenerHandle()
    }

    fun fetchApoiadoOption(
        apoiadoId: String,
        onSuccess: (ApoiadoOption?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        apoiadosCollection.document(normalized)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                onSuccess(buildApoiadoOption(doc))
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun fetchPedidoDescricao(
        pedidoId: String,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = pedidoId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        pedidosCollection.document(normalized)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                onSuccess(doc.getString("descricao")?.trim())
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun fetchFuncionarioIdByUid(
        uid: String,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        funcionariosCollection
            .whereEqualTo("uid", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                onSuccess(docs.documents.firstOrNull()?.id)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun createCesta(
        funcionarioId: String,
        apoiadoId: String,
        produtoIds: List<String>,
        dataAgendada: Date,
        observacoes: String,
        fromUrgent: Boolean,
        pedidoId: String?,
        recorrente: Boolean,
        recorrenciaDias: Int?,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = Date()
        val cestaRef = cestasCollection.document()
        val cestaId = cestaRef.id
        val recDias = if (recorrente) recorrenciaDias else null

        val cestaMap = mutableMapOf<String, Any>(
            "apoiadoID" to apoiadoId,
            "funcionarioID" to funcionarioId,
            "dataAtual" to now,
            "dataAgendada" to dataAgendada,
            "dataRecolha" to dataAgendada,
            "estadoCesta" to "Agendada",
            "produtos" to produtoIds,
            "obs" to observacoes,
            "tipoApoio" to (if (recorrente) "Recorrente" else "Unica"),
            "faltas" to 0,
            "origem" to (if (fromUrgent) "Urgente" else "Manual")
        )

        if (recDias != null) cestaMap["recorrenciaDias"] = recDias
        if (!pedidoId.isNullOrBlank()) cestaMap["pedidoUrgenteId"] = pedidoId

        firestore.runTransaction { txn ->
            val produtoRefs = produtoIds.map { pid ->
                pid to produtosCollection.document(pid)
            }
            val produtoSnaps = produtoRefs.map { (pid, ref) ->
                pid to txn.get(ref)
            }

            val apoiadoRef = apoiadosCollection.document(apoiadoId)
            val apoiadoSnap = txn.get(apoiadoRef)
            val estadoConta = apoiadoSnap.getString("estadoConta")?.trim().orEmpty()
            if (estadoConta.equals("Bloqueado", ignoreCase = true)) {
                throw IllegalStateException("Conta do apoiado bloqueada.")
            }

            val validUntil = apoiadoValidUntil(apoiadoSnap)
            if (validUntil == null || AccountValidity.isExpired(validUntil, now)) {
                throw IllegalStateException(
                    "A conta do apoiado está expirada. O apoiado deve voltar a submeter o formulário."
                )
            }

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

            txn.set(cestaRef, cestaMap)

            if (recorrente && recDias != null) {
                txn.update(
                    apoiadoRef,
                    mapOf(
                        "dataLevantamentoAgendado" to dataAgendada,
                        "recorrenciaDias" to recDias
                    )
                )
            }

            if (fromUrgent && !pedidoId.isNullOrBlank()) {
                txn.update(
                    pedidosCollection.document(pedidoId),
                    mapOf(
                        "estado" to "Preparar_Apoio",
                        "cestaId" to cestaId,
                        "funcionarioID" to funcionarioId,
                        "dataDecisao" to now
                    )
                )
            }

            null
        }
            .addOnSuccessListener {
                val origem = if (fromUrgent) "Urgente" else "Manual"
                val details = "Apoiado: $apoiadoId | Origem: $origem"
                AuditLogger.logAction("Criou cesta", "cesta", cestaId, details)
                onSuccess(cestaId)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun cancelarCesta(
        cestaId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = Date()
        val cestaRef = cestasCollection.document(cestaId)

        firestore.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            produtoIds.forEach { pid ->
                val prodRef = produtosCollection.document(pid)
                val prodSnap = txn.get(prodRef)
                if (prodSnap.exists()) {
                    val reservaId = prodSnap.getString("cestaReservaId")?.trim().orEmpty()
                    val estado = prodSnap.getString("estadoProduto")?.trim().orEmpty()

                    val podeLibertar = reservaId.isBlank() ||
                        reservaId == cestaId ||
                        ProductStatus.fromFirestore(estado) == ProductStatus.RESERVED

                    if (podeLibertar) {
                        txn.update(
                            prodRef,
                            mapOf(
                                "estadoProduto" to ProductStatus.AVAILABLE.firestoreValue,
                                "cestaReservaId" to FieldValue.delete(),
                                "reservadoEm" to FieldValue.delete()
                            )
                        )
                    }
                }
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Cancelada",
                    "dataCancelada" to now
                )
            )

            null
        }
            .addOnSuccessListener {
                AuditLogger.logAction("Cancelou cesta", "cesta", cestaId)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun marcarEntregue(
        cesta: CestaItem,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = Date()
        val cestaRef = cestasCollection.document(cesta.id)

        firestore.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            produtoIds.forEach { pid ->
                val prodRef = produtosCollection.document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to ProductStatus.DELIVERED.firestoreValue,
                        "cestaEntregaId" to cesta.id,
                        "entregueEm" to now,
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Entregue",
                    "dataRecolha" to now,
                    "dataEntregue" to now
                )
            )

            if (cesta.apoiadoId.isNotBlank()) {
                txn.update(
                    apoiadosCollection.document(cesta.apoiadoId),
                    mapOf("ultimoLevantamento" to now)
                )
            }

            null
        }
            .addOnSuccessListener {
                val details = if (cesta.apoiadoId.isNotBlank()) "Apoiado: ${cesta.apoiadoId}" else null
                AuditLogger.logAction("Marcou cesta como entregue", "cesta", cesta.id, details)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun reagendarEntrega(
        cestaId: String,
        novaData: Date,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        cestasCollection.document(cestaId)
            .update(
                mapOf(
                    "dataAgendada" to novaData,
                    "dataRecolha" to novaData,
                    "dataReagendada" to Date()
                )
            )
            .addOnSuccessListener {
                val details = "Nova data: $novaData"
                AuditLogger.logAction("Reagendou cesta", "cesta", cestaId, details)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun reagendarComFalta(
        cesta: CestaItem,
        novaData: Date,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val novasFaltas = cesta.faltas + 1
        val novoEstado = if (novasFaltas >= 3) "Nao_Levantou" else "Agendada"

        val updates = mutableMapOf<String, Any>(
            "faltas" to novasFaltas,
            "estadoCesta" to novoEstado,
            "dataAgendada" to novaData,
            "dataRecolha" to novaData,
            "dataUltimaFalta" to Date()
        )

        cestasCollection.document(cesta.id)
            .update(updates)
            .addOnSuccessListener {
                val details = "Faltas: $novasFaltas | Nova data: $novaData"
                AuditLogger.logAction("Registou falta e reagendou cesta", "cesta", cesta.id, details)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun registarTerceiraFaltaSemReagendar(
        cesta: CestaItem,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val now = Date()
        val cestaRef = cestasCollection.document(cesta.id)

        firestore.runTransaction { txn ->
            val snap = txn.get(cestaRef)
            val produtoIds = (snap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            val novasFaltas = cesta.faltas + 1
            txn.update(
                cestaRef,
                mapOf(
                    "faltas" to novasFaltas,
                    "estadoCesta" to "Nao_Levantou",
                    "dataUltimaFalta" to now
                )
            )

            produtoIds.forEach { pid ->
                val prodRef = produtosCollection.document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to ProductStatus.AVAILABLE.firestoreValue,
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            null
        }
            .addOnSuccessListener {
                AuditLogger.logAction("Registou terceira falta", "cesta", cesta.id)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e) }
    }

    private fun snapshotDate(snapshot: DocumentSnapshot, field: String): Date? {
        return snapshot.getTimestamp(field)?.toDate() ?: (snapshot.get(field) as? Date)
    }

    private fun apoiadoValidUntil(doc: DocumentSnapshot): Date? {
        val ts = doc.getTimestamp("validadeConta") ?: doc.getTimestamp("validade")
        if (ts != null) return ts.toDate()
        val any = doc.get("validadeConta") ?: doc.get("validade")
        return any as? Date
    }

    private fun mapApoiadoDisplayStatus(rawStatus: String): String {
        return when (rawStatus) {
            "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
            "Suspenso" -> "Apoio Pausado"
            else -> rawStatus
        }
    }

    private fun buildApoiadoOption(doc: DocumentSnapshot): ApoiadoOption {
        val rawStatus = doc.getString("estadoConta")?.trim().orEmpty()
        val displayStatus = mapApoiadoDisplayStatus(rawStatus)
        val ultimo = doc.getTimestamp("ultimoLevantamento")?.toDate()
            ?: (doc.get("ultimoLevantamento") as? Date)
        return ApoiadoOption(
            id = doc.id,
            nome = doc.getString("nome")?.trim().orEmpty().ifBlank { doc.id },
            ultimoLevantamento = ultimo,
            rawStatus = rawStatus,
            displayStatus = displayStatus
        )
    }

    private fun buildApoiadoInfo(snapshot: DocumentSnapshot): ApoiadoInfo {
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
        val necessidades = (snapshot.get("necessidade") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val ultimoLevantamento = snapshot.getTimestamp("ultimoLevantamento")?.toDate()
            ?: (snapshot.get("ultimoLevantamento") as? Date)
        val validadeConta = apoiadoValidUntil(snapshot)

        return ApoiadoInfo(
            id = snapshot.id,
            nome = snapshot.getString("nome")?.trim().orEmpty(),
            email = (snapshot.getString("email") ?: snapshot.getString("emailApoiado"))
                ?.trim()
                .orEmpty(),
            contacto = snapshot.getString("contacto")?.trim().orEmpty(),
            documento = documento,
            morada = morada,
            nacionalidade = snapshot.getString("nacionalidade")?.trim().orEmpty(),
            necessidades = necessidades,
            ultimoLevantamento = ultimoLevantamento,
            validadeConta = validadeConta
        )
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
}
