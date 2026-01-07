package ipca.app.lojasas.data.users

import ipca.app.lojasas.data.UserRole
import java.util.Date

data class UserProfileInput(
    val uid: String,
    val numMecanografico: String,
    val nome: String,
    val contacto: String,
    val documentNumber: String,
    val documentType: String,
    val morada: String,
    val codPostal: String,
    val email: String,
    val role: UserRole
)

data class UserProfile(
    val docId: String,
    val role: UserRole,
    val isAdmin: Boolean,
    val nome: String,
    val contacto: String,
    val email: String,
    val documentNumber: String,
    val documentType: String,
    val morada: String,
    val codPostal: String,
    val nacionalidade: String,
    val dataNascimento: Date?,
    val relacaoIPCA: String,
    val curso: String,
    val graoEnsino: String,
    val apoioEmergencia: Boolean,
    val bolsaEstudos: Boolean,
    val valorBolsa: String,
    val necessidades: List<String>,
    val validadeConta: Date?
)
