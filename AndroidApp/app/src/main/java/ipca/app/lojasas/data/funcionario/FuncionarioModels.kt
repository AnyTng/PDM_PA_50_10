package ipca.app.lojasas.data.funcionario

data class CollaboratorItem(
    val id: String,
    val uid: String,
    val nome: String,
    val email: String,
    val role: String
)

data class FuncionarioProfileInput(
    val uid: String,
    val numMecanografico: String,
    val nome: String,
    val contacto: String,
    val documentNumber: String,
    val documentType: String,
    val morada: String,
    val codPostal: String,
    val email: String,
    val role: String,
    val mudarPass: Boolean = true
)
