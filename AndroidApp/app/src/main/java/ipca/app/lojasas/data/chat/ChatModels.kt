package ipca.app.lojasas.data.chat

import com.google.firebase.Timestamp

/**
 * Representa uma mensagem dentro do chat do Apoiados.
 *
 * Firestore path:
 *  apoiados/{apoiadoId}/chat/{messageId}
 */
data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    /** "APOIADO", "FUNCIONARIO" ou "ADMIN" */
    val senderRole: String = "",
    /**
     * Timestamp do servidor (serverTimestamp) quando disponível.
     */
    val createdAt: Timestamp? = null,
    /**
     * Timestamp do cliente (fallback para ordenação/preview quando o serverTimestamp ainda não resolveu).
     */
    val createdAtClient: Timestamp? = null,
    /**
     * Quando o apoiado viu esta mensagem (normalmente mensagens enviadas por funcionários/admin).
     */
    val seenByApoiadoAt: Timestamp? = null,
    /**
     * Quando um funcionário/admin viu esta mensagem (normalmente mensagens enviadas pelo apoiado).
     */
    val seenByStaffAt: Timestamp? = null
)

/**
 * Resumo de um chat (thread) para listagens.
 *
 * Guardamos o resumo no documento do apoiado para permitir ordenar e mostrar badge de não lidas.
 */
data class ChatThreadSummary(
    val apoiadoId: String,
    val apoiadoNome: String,
    val lastMessageText: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderName: String = "",
    val lastSenderRole: String = "",
    val unreadForStaff: Long = 0L,
    val unreadForApoiado: Long = 0L
)
