package ipca.app.lojasas.data.requests

import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrgentRequestsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val requestsCollection = firestore.collection("pedidos_ajuda")

    fun listenUrgentRequests(
        onSuccess: (List<PedidoUrgenteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = requestsCollection
            .whereEqualTo("tipo", "Urgente")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val pedidos = snapshot?.documents.orEmpty().map { doc ->
                    val dataSubmissao = doc.getTimestamp("dataSubmissao")?.toDate()
                        ?: (doc.get("dataSubmissao") as? Date)
                    val dataDecisao = doc.getTimestamp("dataDecisao")?.toDate()
                        ?: (doc.get("dataDecisao") as? Date)

                    PedidoUrgenteItem(
                        id = doc.id,
                        numeroMecanografico = doc.getString("numeroMecanografico")?.trim().orEmpty(),
                        descricao = doc.getString("descricao")?.trim().orEmpty(),
                        estado = doc.getString("estado")?.trim().orEmpty(),
                        dataSubmissao = dataSubmissao,
                        dataDecisao = dataDecisao,
                        cestaId = doc.getString("cestaId")?.trim()
                    )
                }.sortedByDescending { it.dataSubmissao ?: Date(0) }

                onSuccess(pedidos)
            }

        return registration.asListenerHandle()
    }

    fun listenByNumeroMecanografico(
        numeroMecanografico: String,
        onSuccess: (List<UrgentRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = numeroMecanografico.trim()
        if (normalized.isBlank()) {
            onSuccess(emptyList())
            return ListenerHandle { }
        }

        val registration = requestsCollection
            .whereEqualTo("numeroMecanografico", normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents.orEmpty().map { doc ->
                    UrgentRequest(
                        id = doc.id,
                        descricao = doc.getString("descricao") ?: "",
                        estado = doc.getString("estado") ?: "Analise",
                        data = doc.getTimestamp("dataSubmissao")?.toDate(),
                        tipo = doc.getString("tipo") ?: "Ajuda",
                        cestaId = doc.getString("cestaId")?.trim()
                    )
                }.sortedByDescending { it.data }
                onSuccess(requests)
            }
        return registration.asListenerHandle()
    }

    fun submitUrgentRequest(
        numeroMecanografico: String,
        descricao: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "numeroMecanografico" to numeroMecanografico.trim(),
            "descricao" to descricao.trim(),
            "estado" to "Analise",
            "dataSubmissao" to Date(),
            "tipo" to "Urgente"
        )

        requestsCollection
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateUrgentRequest(
        pedidoId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = pedidoId.trim()
        if (normalized.isBlank()) {
            onError(IllegalArgumentException("Missing pedidoId"))
            return
        }

        requestsCollection.document(normalized)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
