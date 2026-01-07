package ipca.app.lojasas.data.apoiado

import java.util.Date

data class ApoiadoItem(
    val id: String,
    val nome: String,
    val email: String,
    val rawStatus: String,
    val displayStatus: String,
    val contacto: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val morada: String = "",
    val nacionalidade: String = "",
    val dataNascimento: Date? = null
)

data class ApoiadoPdfDetails(
    val id: String,
    val nome: String,
    val email: String,
    val emailApoiado: String,
    val contacto: String,
    val documentType: String,
    val documentNumber: String,
    val nacionalidade: String,
    val dataNascimento: Date?,
    val morada: String,
    val codPostal: String,
    val relacaoIPCA: String,
    val curso: String,
    val graoEnsino: String,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String,
    val necessidades: List<String>,
    val estadoConta: String,
    val dadosIncompletos: Boolean,
    val faltaDocumentos: Boolean,
    val mudarPass: Boolean,
    val uid: String,
    val extraFields: Map<String, Any?>
)

data class ApoiadoCreationInput(
    val uid: String,
    val numMecanografico: String,
    val nome: String,
    val email: String,
    val contacto: String,
    val documentNumber: String,
    val documentType: String,
    val nacionalidade: String,
    val dataNascimento: Date,
    val morada: String,
    val codPostal: String,
    val relacaoIPCA: String,
    val curso: String,
    val graoEnsino: String,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String,
    val necessidades: List<String>
)

data class ApoiadoSummary(
    val id: String,
    val nome: String,
    val dataPedido: Date?
)

data class ApoiadoDetails(
    val id: String,
    val nome: String,
    val email: String,
    val contacto: String,
    val documentNumber: String,
    val documentType: String,
    val morada: String,
    val tipo: String,
    val dadosIncompletos: Boolean,
    val nacionalidade: String,
    val dataNascimento: Date?,
    val curso: String?,
    val grauEnsino: String?,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String?,
    val necessidades: List<String>
)

data class DocumentSummary(
    val typeTitle: String,
    val fileName: String,
    val url: String,
    val date: Date?,
    val entrega: Int
)
