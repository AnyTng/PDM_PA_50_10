package ipca.app.lojasas.data.apoiado

import java.util.Date

data class ApoiadoProfile(
    val docId: String,
    val dadosIncompletos: Boolean,
    val faltaDocumentos: Boolean,
    val estadoConta: String,
    val nome: String,
    val numeroMecanografico: String,
    val mudarPass: Boolean,
    val validade: Date?
)

data class ApoiadoStatus(
    val estadoConta: String,
    val numeroMecanografico: String
)

data class ApoiadoFormData(
    val relacaoIPCA: String,
    val curso: String,
    val graoEnsino: String,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String,
    val dataNascimento: Date?,
    val nacionalidade: String,
    val necessidades: List<String>
)
