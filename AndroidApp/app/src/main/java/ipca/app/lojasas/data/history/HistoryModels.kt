package ipca.app.lojasas.data.history

import java.util.Date

data class HistoryEntry(
    val id: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String?,
    val funcionarioNome: String,
    val funcionarioId: String,
    val timestamp: Date?
)
