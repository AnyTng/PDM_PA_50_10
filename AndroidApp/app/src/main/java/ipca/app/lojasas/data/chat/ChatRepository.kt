package ipca.app.lojasas.data.chat

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val apoiadosCollection = firestore.collection("apoiados")

    fun listenChatMessages(
        apoiadoId: String,
        onSuccess: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(emptyList())
            return ListenerHandle { }
        }

        val registration = apoiadosCollection
            .document(normalized)
            .collection(CHAT_SUBCOLLECTION)
            .orderBy(FIELD_CREATED_AT_CLIENT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val msgs = snapshot?.documents.orEmpty().map { doc ->
                    ChatMessage(
                        id = doc.id,
                        text = doc.getString(FIELD_TEXT) ?: "",
                        senderId = doc.getString(FIELD_SENDER_ID) ?: "",
                        senderName = doc.getString(FIELD_SENDER_NAME) ?: "",
                        senderRole = doc.getString(FIELD_SENDER_ROLE) ?: "",
                        createdAt = doc.getTimestamp(FIELD_CREATED_AT),
                        createdAtClient = doc.getTimestamp(FIELD_CREATED_AT_CLIENT),
                        seenByApoiadoAt = doc.getTimestamp(FIELD_SEEN_BY_APOIADO_AT),
                        seenByStaffAt = doc.getTimestamp(FIELD_SEEN_BY_STAFF_AT)
                    )
                }

                onSuccess(msgs)
            }

        return registration.asListenerHandle()
    }

    fun sendMessage(
        apoiadoId: String,
        text: String,
        senderName: String,
        senderRole: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalizedId = apoiadoId.trim()
        val normalizedText = text.trim()
        val normalizedName = senderName.trim()
        val normalizedRole = senderRole.trim()

        if (normalizedId.isBlank()) {
            onError(IllegalArgumentException("Missing apoiadoId"))
            return
        }

        if (normalizedText.isBlank()) {
            onError(IllegalArgumentException("Empty message"))
            return
        }

        val uid = auth.currentUser?.uid?.trim().orEmpty()

        val messageData = hashMapOf(
            FIELD_TEXT to normalizedText,
            FIELD_SENDER_ID to uid,
            FIELD_SENDER_NAME to normalizedName,
            FIELD_SENDER_ROLE to normalizedRole,
            // serverTimestamp é usado para consistência; client timestamp para ordenação imediata.
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            FIELD_CREATED_AT_CLIENT to Timestamp.now(),
            FIELD_SEEN_BY_APOIADO_AT to null,
            FIELD_SEEN_BY_STAFF_AT to null
        )

        apoiadosCollection
            .document(normalizedId)
            .collection(CHAT_SUBCOLLECTION)
            .add(messageData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Marca como vistas as mensagens do "outro lado" e faz reset ao contador de não lidas.
     *
     * - viewerIsApoiado = true: marca mensagens enviadas por funcionários/admin (senderRole != APOIADO)
     *   com seenByApoiadoAt.
     * - viewerIsApoiado = false: marca mensagens enviadas pelo apoiado (senderRole == APOIADO)
     *   com seenByStaffAt.
     */
    fun markThreadAsSeen(
        apoiadoId: String,
        viewerIsApoiado: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess()
            return
        }

        val messagesRef = apoiadosCollection
            .document(normalized)
            .collection(CHAT_SUBCOLLECTION)

        // Evitar queries com múltiplos filtros que podem exigir índices.
        // Vamos buscar apenas as últimas N mensagens e marcar as que interessam.
        messagesRef
            .orderBy(FIELD_CREATED_AT_CLIENT, Query.Direction.DESCENDING)
            .limit(250)
            .get()
            .addOnSuccessListener { snap ->
                val batch = firestore.batch()

                var anyUpdate = false
                snap.documents.forEach { doc ->
                    val role = doc.getString(FIELD_SENDER_ROLE) ?: ""
                    val isFromApoiado = role.equals(ROLE_APOIADO, ignoreCase = true)

                    if (viewerIsApoiado) {
                        // Apoiados "vê" mensagens de funcionários/admin.
                        val seenAt = doc.getTimestamp(FIELD_SEEN_BY_APOIADO_AT)
                        if (!isFromApoiado && seenAt == null) {
                            batch.update(doc.reference, FIELD_SEEN_BY_APOIADO_AT, FieldValue.serverTimestamp())
                            anyUpdate = true
                        }
                    } else {
                        // Staff "vê" mensagens do apoiado.
                        val seenAt = doc.getTimestamp(FIELD_SEEN_BY_STAFF_AT)
                        if (isFromApoiado && seenAt == null) {
                            batch.update(doc.reference, FIELD_SEEN_BY_STAFF_AT, FieldValue.serverTimestamp())
                            anyUpdate = true
                        }
                    }
                }

                // Reset do contador de não lidas no documento do apoiado.
                val parentUpdates = if (viewerIsApoiado) {
                    mapOf(FIELD_UNREAD_FOR_APOIADO to 0L)
                } else {
                    mapOf(FIELD_UNREAD_FOR_STAFF to 0L)
                }
                batch.update(apoiadosCollection.document(normalized), parentUpdates)
                anyUpdate = true

                if (!anyUpdate) {
                    onSuccess()
                    return@addOnSuccessListener
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    fun listenHasUnreadForApoiado(
        apoiadoId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val normalized = apoiadoId.trim()
        if (normalized.isBlank()) {
            onSuccess(false)
            return ListenerHandle { }
        }

        val registration = apoiadosCollection.document(normalized)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val unread = snapshot?.getLong(FIELD_UNREAD_FOR_APOIADO) ?: 0L
                onSuccess(unread > 0L)
            }

        return registration.asListenerHandle()
    }

    fun listenHasUnreadForStaff(
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = apoiadosCollection
            .whereGreaterThan(FIELD_UNREAD_FOR_STAFF, 0)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onSuccess(snapshot != null && !snapshot.isEmpty)
            }

        return registration.asListenerHandle()
    }

    fun listenChatThreadsForStaff(
        onSuccess: (List<ChatThreadSummary>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = apoiadosCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty().map { doc ->
                    ChatThreadSummary(
                        apoiadoId = doc.id,
                        apoiadoNome = doc.getString("nome") ?: doc.id,
                        lastMessageText = doc.getString(FIELD_LAST_TEXT) ?: "",
                        lastMessageAt = doc.getTimestamp(FIELD_LAST_AT),
                        lastSenderName = doc.getString(FIELD_LAST_SENDER_NAME) ?: "",
                        lastSenderRole = doc.getString(FIELD_LAST_SENDER_ROLE) ?: "",
                        unreadForStaff = doc.getLong(FIELD_UNREAD_FOR_STAFF) ?: 0L,
                        unreadForApoiado = doc.getLong(FIELD_UNREAD_FOR_APOIADO) ?: 0L
                    )
                }.sortedWith(compareByDescending<ChatThreadSummary> {
                    it.lastMessageAt?.toDate()?.time ?: 0L
                }.thenBy { it.apoiadoNome.lowercase() })

                onSuccess(list)
            }

        return registration.asListenerHandle()
    }

    companion object {
        const val CHAT_SUBCOLLECTION = "chat"

        const val ROLE_APOIADO = "APOIADO"
        const val ROLE_FUNCIONARIO = "FUNCIONARIO"
        const val ROLE_ADMIN = "ADMIN"

        const val FIELD_TEXT = "text"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_SENDER_NAME = "senderName"
        const val FIELD_SENDER_ROLE = "senderRole"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_CREATED_AT_CLIENT = "createdAtClient"
        const val FIELD_SEEN_BY_APOIADO_AT = "seenByApoiadoAt"
        const val FIELD_SEEN_BY_STAFF_AT = "seenByStaffAt"

        // Resumo no documento do apoiado
        const val FIELD_LAST_AT = "chatLastMessageAt"
        const val FIELD_LAST_TEXT = "chatLastMessageText"
        const val FIELD_LAST_SENDER_NAME = "chatLastSenderName"
        const val FIELD_LAST_SENDER_ROLE = "chatLastSenderRole"
        const val FIELD_UNREAD_FOR_STAFF = "chatUnreadForStaff"
        const val FIELD_UNREAD_FOR_APOIADO = "chatUnreadForApoiado"
    }
}
