package ipca.app.lojasas.data.history

import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun listenHistory(
        onSuccess: (List<HistoryEntry>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = firestore.collectionGroup("historico")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents.orEmpty().map { doc ->
                    HistoryEntry(
                        id = doc.id,
                        action = doc.getString("action") ?: "",
                        entity = doc.getString("entity") ?: "",
                        entityId = doc.getString("entityId") ?: "",
                        details = doc.getString("details"),
                        funcionarioNome = doc.getString("funcionarioNome") ?: "",
                        funcionarioId = doc.getString("funcionarioId") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.toDate()
                            ?: (doc.get("timestamp") as? Date)
                    )
                }
                onSuccess(entries)
            }

        return registration.asListenerHandle()
    }
}
