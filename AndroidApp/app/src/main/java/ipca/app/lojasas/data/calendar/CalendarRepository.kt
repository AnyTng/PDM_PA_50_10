package ipca.app.lojasas.data.calendar

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class EmployeePasswordStatus(
    val docId: String,
    val mustChangePassword: Boolean
)

@Singleton
class CalendarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val campaignsCollection = firestore.collection("campanha")
    private val productsCollection = firestore.collection("produtos")
    private val basketsCollection = firestore.collection("cestas")
    private val employeesCollection = firestore.collection("funcionarios")

    fun listenMonthEvents(
        start: Date,
        end: Date,
        onSuccess: (List<CalendarEvent>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val sourceEvents = mutableMapOf(
            SOURCE_CAMPAIGN_START to emptyList<CalendarEvent>(),
            SOURCE_CAMPAIGN_END to emptyList(),
            SOURCE_PRODUCT_EXPIRY to emptyList(),
            SOURCE_BASKET_DELIVERY to emptyList()
        )

        fun publish(source: String, events: List<CalendarEvent>) {
            sourceEvents[source] = events
            val merged = sourceEvents.values.flatten().sortedBy { it.date }
            onSuccess(merged)
        }

        val startCampaigns = campaignsCollection
            .whereGreaterThanOrEqualTo("dataInicio", start)
            .whereLessThan("dataInicio", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val nome = doc.getString("nomeCampanha") ?: "Campanha"
                    val inicio = doc.getTimestamp("dataInicio")?.toDate()
                        ?: (doc.get("dataInicio") as? Date)
                    inicio?.let {
                        CalendarEvent(
                            id = doc.id + "_start",
                            title = "Início: $nome",
                            date = it,
                            type = EventType.CAMPAIGN_START
                        )
                    }
                }
                publish(SOURCE_CAMPAIGN_START, events)
            }

        val endCampaigns = campaignsCollection
            .whereGreaterThanOrEqualTo("dataFim", start)
            .whereLessThan("dataFim", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val nome = doc.getString("nomeCampanha") ?: "Campanha"
                    val fim = doc.getTimestamp("dataFim")?.toDate()
                        ?: (doc.get("dataFim") as? Date)
                    fim?.let {
                        CalendarEvent(
                            id = doc.id + "_end",
                            title = "Fim: $nome",
                            date = it,
                            type = EventType.CAMPAIGN_END
                        )
                    }
                }
                publish(SOURCE_CAMPAIGN_END, events)
            }

        val expiryProducts = productsCollection
            .whereGreaterThanOrEqualTo("validade", start)
            .whereLessThan("validade", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val validade = doc.getTimestamp("validade")?.toDate()
                        ?: (doc.get("validade") as? Date)
                    val nome = doc.getString("nomeProduto") ?: "Produto"
                    validade?.let {
                        CalendarEvent(
                            id = doc.id,
                            title = "Validade: $nome",
                            date = it,
                            type = EventType.PRODUCT_EXPIRY
                        )
                    }
                }
                publish(SOURCE_PRODUCT_EXPIRY, events)
            }

        val basketDeliveries = basketsCollection
            .whereGreaterThanOrEqualTo("dataRecolha", start)
            .whereLessThan("dataRecolha", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.getTimestamp("dataRecolha")?.toDate()
                        ?: (doc.get("dataRecolha") as? Date)
                    val estado = doc.getString("estadoCesta")?.trim().orEmpty()
                    val apoiadoId = doc.getString("apoiadoID")?.trim().orEmpty()
                    data?.let {
                        CalendarEvent(
                            id = doc.id,
                            title = "Entrega Cesta ($estado)",
                            date = it,
                            type = EventType.BASKET_DELIVERY,
                            description = "Apoiado: $apoiadoId"
                        )
                    }
                }
                publish(SOURCE_BASKET_DELIVERY, events)
            }

        val handles = listOf(
            startCampaigns.asListenerHandle(),
            endCampaigns.asListenerHandle(),
            expiryProducts.asListenerHandle(),
            basketDeliveries.asListenerHandle()
        )
        return ListenerHandle { handles.forEach { it.remove() } }
    }

    fun fetchEmployeePasswordStatus(
        onSuccess: (EmployeePasswordStatus?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onSuccess(null)
            return
        }
        employeesCollection
            .whereEqualTo("uid", user.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                val mustChange = doc.getBoolean("mudarPass") ?: false
                onSuccess(EmployeePasswordStatus(doc.id, mustChange))
            }
            .addOnFailureListener { onError(it) }
    }

    fun changeEmployeePassword(
        docId: String,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (docId.isBlank()) {
            onError("Registo de utilizador indisponível.")
            return
        }
        val user = auth.currentUser
        if (user == null) {
            onError("Utilizador não autenticado.")
            return
        }
        val email = user.email?.trim().orEmpty()
        if (email.isBlank()) {
            onError("Email do utilizador indisponível.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        employeesCollection.document(docId)
                            .update("mudarPass", false)
                            .addOnSuccessListener {
                                AuditLogger.logAction(
                                    "Alterou palavra-passe",
                                    "funcionario",
                                    docId
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

    private companion object {
        const val SOURCE_CAMPAIGN_START = "campaign_start"
        const val SOURCE_CAMPAIGN_END = "campaign_end"
        const val SOURCE_PRODUCT_EXPIRY = "product_expiry"
        const val SOURCE_BASKET_DELIVERY = "basket_delivery"
    }
}
