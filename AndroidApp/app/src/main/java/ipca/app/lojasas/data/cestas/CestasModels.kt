package ipca.app.lojasas.data.cestas

import java.util.Date

data class CestaItem(
    val id: String,
    val apoiadoId: String,
    val funcionarioId: String,
    val dataAgendada: Date? = null,
    val dataRecolha: Date? = null,
    val estado: String = "",
    val faltas: Int = 0,
    val origem: String? = null,
    val tipoApoio: String? = null
)

data class CestaDetails(
    val id: String,
    val apoiadoId: String,
    val funcionarioId: String,
    val dataAgendada: Date? = null,
    val dataRecolha: Date? = null,
    val dataReagendada: Date? = null,
    val dataEntregue: Date? = null,
    val dataCancelada: Date? = null,
    val dataUltimaFalta: Date? = null,
    val estado: String = "",
    val faltas: Int = 0,
    val origem: String? = null,
    val tipoApoio: String? = null,
    val produtoIds: List<String> = emptyList(),
    val produtosCount: Int = 0,
    val observacoes: String? = null
)

data class ApoiadoCesta(
    val id: String,
    val dataRecolha: Date? = null,
    val dataAgendada: Date? = null,
    val estadoCesta: String = "",
    val numeroItens: Int = 0,
    val faltas: Int = 0,
    val origem: String? = null,
    val pedidoUrgenteId: String? = null
)

data class ApoiadoInfo(
    val id: String,
    val nome: String,
    val email: String,
    val contacto: String,
    val documento: String,
    val morada: String,
    val nacionalidade: String,
    val necessidades: List<String> = emptyList(),
    val ultimoLevantamento: Date? = null,
    val validadeConta: Date? = null
)

data class ApoiadoOption(
    val id: String,
    val nome: String,
    val ultimoLevantamento: Date? = null,
    val rawStatus: String = "",
    val displayStatus: String = ""
)
