package ipca.app.lojasas.data.requests

import java.util.Date

data class UrgentRequest(
    val id: String,
    val descricao: String,
    val estado: String,
    val data: Date?,
    val tipo: String,
    val cestaId: String? = null
)

data class PedidoUrgenteItem(
    val id: String,
    val numeroMecanografico: String,
    val descricao: String,
    val estado: String,
    val dataSubmissao: Date? = null,
    val dataDecisao: Date? = null,
    val cestaId: String? = null
)
