package ipca.app.lojasas.data.apoiado

import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import ipca.app.lojasas.utils.AccountValidity
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApoiadoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val apoiadosCollection = firestore.collection("apoiados")

    fun fetchApoiadoProfileByUid(
        uid: String,
        onSuccess: (ApoiadoProfile?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        queryApoiadoByUid(
            uid = uid,
            onSuccess = { doc ->
                if (doc == null) {
                    onSuccess(null)
                    return@queryApoiadoByUid
                }
                val profile = ApoiadoProfile(
                    docId = doc.id,
                    dadosIncompletos = doc.getBoolean("dadosIncompletos") ?: false,
                    faltaDocumentos = doc.getBoolean("faltaDocumentos") ?: false,
                    estadoConta = doc.getString("estadoConta") ?: "",
                    nome = doc.getString("nome") ?: "Utilizador",
                    numeroMecanografico = doc.getString("numMecanografico")
                        ?: doc.getString("numeroMecanografico")
                        ?: "",
                    mudarPass = doc.getBoolean("mudarPass") ?: false,
                    validade = snapshotDate(doc, "validadeConta") ?: snapshotDate(doc, "validade")
                )
                onSuccess(profile)
            },
            onError = onError
        )
    }

    fun fetchApoiadoIdByUid(
        uid: String,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        queryApoiadoByUid(
            uid = uid,
            onSuccess = { doc -> onSuccess(doc?.id) },
            onError = onError
        )
    }

    fun listenApoiadoStatus(
        uid: String,
        onSuccess: (ApoiadoStatus) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            return ListenerHandle { }
        }

        val registration = apoiadosCollection
            .whereEqualTo("uid", normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                val estado = doc.getString("estadoConta") ?: ""
                val numMec = doc.getString("numMecanografico")
                    ?: doc.getString("numeroMecanografico")
                    ?: ""
                onSuccess(ApoiadoStatus(estadoConta = estado, numeroMecanografico = numMec))
            }
        return registration.asListenerHandle()
    }

    fun listenApoiados(
        onSuccess: (List<ApoiadoItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = apoiadosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents.orEmpty().map { doc ->
                val rawStatus = doc.getString("estadoConta") ?: ""
                val validadeDate = snapshotDate(doc, "validadeConta") ?: snapshotDate(doc, "validade")
                val displayStatus = resolveDisplayStatus(rawStatus, validadeDate)
                val email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "Sem Email"
                val morada = buildMorada(doc)

                ApoiadoItem(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "Sem Nome",
                    email = email,
                    rawStatus = rawStatus,
                    displayStatus = displayStatus,
                    contacto = doc.getString("contacto") ?: "",
                    documentType = doc.getString("documentType") ?: "Doc",
                    documentNumber = doc.getString("documentNumber") ?: "",
                    morada = morada,
                    nacionalidade = doc.getString("nacionalidade") ?: "",
                    dataNascimento = snapshotDate(doc, "dataNascimento")
                )
            }

            onSuccess(list)
        }

        return registration.asListenerHandle()
    }

    fun fetchApoiadoPdfDetails(
        apoiadoId: String,
        onSuccess: (ApoiadoPdfDetails?) -> Unit,
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
                onSuccess(buildApoiadoPdfDetails(doc))
            }
            .addOnFailureListener { onError(it) }
    }

    fun createApoiadoProfile(
        input: ApoiadoCreationInput,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = input.numMecanografico.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing numMecanografico"))
            return
        }

        val apoiadoMap = hashMapOf(
            "uid" to input.uid,
            "role" to "Apoiado",
            "email" to input.email,
            "emailApoiado" to input.email,
            "nome" to input.nome,
            "numMecanografico" to input.numMecanografico,
            "contacto" to input.contacto,
            "documentNumber" to input.documentNumber,
            "documentType" to input.documentType,
            "nacionalidade" to input.nacionalidade,
            "dataNascimento" to input.dataNascimento,
            "morada" to input.morada,
            "codPostal" to input.codPostal,
            "relacaoIPCA" to input.relacaoIPCA,
            "curso" to if (input.relacaoIPCA == "Estudante") input.curso else "",
            "graoEnsino" to if (input.relacaoIPCA == "Estudante") input.graoEnsino else "",
            "apoioEmergenciaSocial" to input.apoioEmergencia,
            "bolsaEstudos" to input.bolsaEstudos,
            "valorBolsa" to if (input.bolsaEstudos) input.valorBolsa else "",
            "necessidade" to input.necessidades,
            "estadoConta" to "Aprovado",
            "faltaDocumentos" to false,
            "dadosIncompletos" to false,
            "mudarPass" to true,
            "validadeConta" to AccountValidity.nextSeptembem30()
        )

        apoiadosCollection.document(normalized)
            .set(apoiadoMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun fetchPendingApoiados(
        onSuccess: (List<ApoiadoSummary>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        apoiadosCollection
            .whereEqualTo("estadoConta", "Analise")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    ApoiadoSummary(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Sem Nome",
                        dataPedido = null
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun fetchApoiadoValidationDetails(
        apoiadoId: String,
        onSuccess: (ApoiadoDetails?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        apoiadosCollection.document(normalized).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val details = ApoiadoDetails(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "",
                    email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "",
                    contacto = doc.getString("contacto") ?: "",
                    documentNumber = doc.getString("documentNumber") ?: "",
                    documentType = doc.getString("documentType") ?: "NIF",
                    morada = buildMorada(doc),
                    tipo = doc.getString("relacaoIPCA") ?: "N/A",
                    dadosIncompletos = doc.getBoolean("dadosIncompletos") ?: false,
                    nacionalidade = doc.getString("nacionalidade") ?: "—",
                    dataNascimento = snapshotDate(doc, "dataNascimento"),
                    curso = doc.getString("curso"),
                    grauEnsino = doc.getString("graoEnsino"),
                    apoioEmergencia = doc.getBoolean("apoioEmergenciaSocial") ?: false,
                    bolsaEstudos = doc.getBoolean("bolsaEstudos") ?: false,
                    valorBolsa = doc.getString("valorBolsa"),
                    necessidades = (doc.get("necessidade") as? List<String>) ?: emptyList()
                )

                onSuccess(details)
            }
            .addOnFailureListener { onError(it) }
    }

    fun approveApoiadoAccount(
        apoiadoId: String,
        funcionarioId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing apoiadoId"))
            return
        }

        apoiadosCollection.document(normalized).get()
            .addOnSuccessListener { doc ->
                val updates = mutableMapOf<String, Any>(
                    "estadoConta" to "Aprovado",
                    "validadoPor" to funcionarioId,
                    "dataValidacao" to Date()
                )

                val hasValidity = (doc.getTimestamp("validadeConta") != null) || (doc.get("validadeConta") != null)
                if (!hasValidity) {
                    updates["validadeConta"] = AccountValidity.nextSeptembem30()
                }

                updateApoiadoStatus(normalized, updates, onSuccess, onError)
            }
            .addOnFailureListener {
                val updates = mutableMapOf<String, Any>(
                    "estadoConta" to "Aprovado",
                    "validadoPor" to funcionarioId,
                    "dataValidacao" to Date()
                )
                updateApoiadoStatus(normalized, updates, onSuccess, onError)
            }
    }

    fun denyApoiadoAccount(
        apoiadoId: String,
        funcionarioId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing apoiadoId"))
            return
        }

        val denialData = hashMapOf(
            "motivo" to reason,
            "negadoPor" to funcionarioId,
            "data" to Date()
        )

        apoiadosCollection.document(normalized)
            .collection("JustificacoesNegacao")
            .add(denialData)
            .addOnSuccessListener {
                val updates = mapOf(
                    "estadoConta" to "Negado",
                    "negadoPor" to funcionarioId
                )
                updateApoiadoStatus(normalized, updates, onSuccess, onError)
            }
            .addOnFailureListener { onError(it) }
    }

    fun blockApoiadoAccount(
        apoiadoId: String,
        funcionarioId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "estadoConta" to "Bloqueado",
            "bloqueadoPor" to funcionarioId
        )
        updateApoiadoStatus(apoiadoId, updates, onSuccess, onError)
    }

    fun updateApoiadoStatus(
        apoiadoId: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing apoiadoId"))
            return
        }

        apoiadosCollection.document(normalized)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun markAccountExpired(docId: String, onFailure: (Exception) -> Unit) {
        val normalized = docId.trim()
        if (normalized.isBlank()) return

        apoiadosCollection.document(normalized)
            .update(
                mapOf(
                    "estadoConta" to "Correcao_Dados",
                    "dadosIncompletos" to true,
                    "faltaDocumentos" to false
                )
            )
            .addOnFailureListener { onFailure(it) }
    }

    fun resetToRetry(
        docId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing docId"))
            return
        }

        apoiadosCollection.document(normalized)
            .update(
                mapOf(
                    "estadoConta" to "Correcao_Dados",
                    "dadosIncompletos" to true,
                    "faltaDocumentos" to false
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun fetchLatestDenialReason(
        docId: String,
        onSuccess: (String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        apoiadosCollection.document(normalized)
            .collection("JustificacoesNegacao")
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val motivo = docs.documents.firstOrNull()?.getString("motivo")
                onSuccess(motivo)
            }
            .addOnFailureListener { onError(it) }
    }

    fun changePassword(
        docId: String,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onError("Registo de utilizador indisponivel.")
            return
        }

        val user = auth.currentUser
        val email = user?.email?.trim().orEmpty()
        if (user == null || email.isBlank()) {
            onError("Utilizador nao autenticado.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        apoiadosCollection.document(normalized)
                            .update("mudarPass", false)
                            .addOnSuccessListener {
                                AuditLogger.logAction(
                                    "Alterou palavra-passe",
                                    "apoiado",
                                    normalized
                                )
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onError(e.message ?: "Erro ao atualizar perfil.")
                            }
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Erro ao atualizar palavra-passe.")
                    }
            }
            .addOnFailureListener {
                onError("Senha incorreta.")
            }
    }

    fun fetchNationalities(
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        apoiadosCollection.get()
            .addOnSuccessListener { result ->
                val list = result.documents
                    .mapNotNull { it.getString("nacionalidade") }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun fetchApoiadoFormData(
        docId: String,
        onSuccess: (ApoiadoFormData?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        apoiadosCollection.document(normalized).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val form = ApoiadoFormData(
                    relacaoIPCA = document.getString("relacaoIPCA") ?: "",
                    curso = document.getString("curso") ?: "",
                    graoEnsino = document.getString("graoEnsino") ?: "",
                    apoioEmergencia = document.getBoolean("apoioEmergenciaSocial") ?: false,
                    bolsaEstudos = document.getBoolean("bolsaEstudos") ?: false,
                    valorBolsa = document.getString("valorBolsa") ?: "",
                    dataNascimento = snapshotDate(document, "dataNascimento"),
                    nacionalidade = document.getString("nacionalidade") ?: "",
                    necessidades = (document.get("necessidade") as? List<String>) ?: emptyList()
                )
                onSuccess(form)
            }
            .addOnFailureListener { onError(it) }
    }

    fun updateApoiadoFormData(
        docId: String,
        data: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = docId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing docId"))
            return
        }

        apoiadosCollection.document(normalized)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    private fun queryApoiadoByUid(
        uid: String,
        onSuccess: (DocumentSnapshot?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = uid.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        apoiadosCollection
            .whereEqualTo("uid", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.firstOrNull())
            }
            .addOnFailureListener { onError(it) }
    }

    private fun snapshotDate(
        doc: DocumentSnapshot,
        field: String
    ): Date? {
        return doc.getTimestamp(field)?.toDate()
            ?: (doc.get(field) as? Date)
    }

    private fun resolveDisplayStatus(rawStatus: String, validade: Date?): String {
        val isExpired = validade?.let { AccountValidity.isExpired(it) } ?: false
        return if (isExpired &&
            !rawStatus.equals("Bloqueado", ignoreCase = true) &&
            !rawStatus.equals("Negado", ignoreCase = true)
        ) {
            "Conta Expirada"
        } else {
            when (rawStatus) {
                "Falta_Documentos", "Correcao_Dados", "" -> "Por Submeter"
                "Suspenso" -> "Apoio Pausado"
                else -> rawStatus
            }
        }
    }

    private fun buildApoiadoPdfDetails(doc: DocumentSnapshot): ApoiadoPdfDetails {
        val dataNascimento = snapshotDate(doc, "dataNascimento")
        val necessidades = (doc.get("necessidade") as? List<*>)?.mapNotNull { it as? String }
            ?: emptyList()
        val knownKeys = setOf(
            "uid",
            "email",
            "emailApoiado",
            "nome",
            "contacto",
            "documentNumber",
            "documentType",
            "nacionalidade",
            "dataNascimento",
            "morada",
            "codPostal",
            "relacaoIPCA",
            "curso",
            "graoEnsino",
            "apoioEmergenciaSocial",
            "bolsaEstudos",
            "valorBolsa",
            "necessidade",
            "estadoConta",
            "faltaDocumentos",
            "dadosIncompletos",
            "mudarPass"
        )
        val extraFields = doc.data
            ?.filterKeys { key -> key !in knownKeys }
            ?.mapValues { (_, value) -> normalizeExtraValue(value) }
            .orEmpty()

        return ApoiadoPdfDetails(
            id = doc.id,
            nome = doc.getString("nome").orEmpty(),
            email = doc.getString("email") ?: doc.getString("emailApoiado") ?: "",
            emailApoiado = doc.getString("emailApoiado").orEmpty(),
            contacto = doc.getString("contacto").orEmpty(),
            documentType = doc.getString("documentType") ?: "Doc",
            documentNumber = doc.getString("documentNumber").orEmpty(),
            nacionalidade = doc.getString("nacionalidade").orEmpty(),
            dataNascimento = dataNascimento,
            morada = doc.getString("morada").orEmpty(),
            codPostal = doc.getString("codPostal").orEmpty(),
            relacaoIPCA = doc.getString("relacaoIPCA").orEmpty(),
            curso = doc.getString("curso").orEmpty(),
            graoEnsino = doc.getString("graoEnsino").orEmpty(),
            apoioEmergencia = doc.getBoolean("apoioEmergenciaSocial") ?: false,
            bolsaEstudos = doc.getBoolean("bolsaEstudos") ?: false,
            valorBolsa = doc.getString("valorBolsa").orEmpty(),
            necessidades = necessidades,
            estadoConta = doc.getString("estadoConta").orEmpty(),
            dadosIncompletos = doc.getBoolean("dadosIncompletos") ?: false,
            faltaDocumentos = doc.getBoolean("faltaDocumentos") ?: false,
            mudarPass = doc.getBoolean("mudarPass") ?: false,
            uid = doc.getString("uid").orEmpty(),
            extraFields = extraFields
        )
    }

    private fun buildMorada(doc: DocumentSnapshot): String {
        val morada = doc.getString("morada")?.trim().orEmpty()
        val codPostal = doc.getString("codPostal")?.trim().orEmpty()
        return when {
            morada.isNotEmpty() && codPostal.isNotEmpty() -> "$morada, $codPostal"
            morada.isNotEmpty() -> morada
            codPostal.isNotEmpty() -> codPostal
            else -> ""
        }
    }

    private fun normalizeExtraValue(value: Any?): Any? {
        return when (value) {
            is Timestamp -> value.toDate()
            is List<*> -> value.map { item -> normalizeExtraValue(item) }
            else -> value
        }
    }
}
