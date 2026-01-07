package ipca.app.lojasas.data.users

import ipca.app.lojasas.data.UserRole

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
