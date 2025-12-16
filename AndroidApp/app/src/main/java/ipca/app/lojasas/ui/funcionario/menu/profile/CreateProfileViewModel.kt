package ipca.app.lojasas.ui.funcionario.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.UserRole

data class CreateProfileState(
    var numMecanografico: String = "",
    var nome: String = "",
    var contacto: String = "",
    var email: String = "",
    var password: String = "",
    var documentNumber: String = "", // <--- MUDADO
    var documentType: String = "NIF",
    var morada: String = "",
    var codPostal: String = "",
    var role: UserRole = UserRole.FUNCIONARIO,
    var isLoading: Boolean = false,
    var error: String? = null,
    var success: Boolean = false
)

class CreateProfileViewModel : ViewModel() {

    var uiState = mutableStateOf(CreateProfileState())
        private set

    // Funções de update
    fun onNumMecanograficoChange(newValue: String) { uiState.value = uiState.value.copy(numMecanografico = newValue) }
    fun onNomeChange(newValue: String) { uiState.value = uiState.value.copy(nome = newValue) }
    fun onContactoChange(newValue: String) { uiState.value = uiState.value.copy(contacto = newValue) }
    fun onEmailChange(newValue: String) { uiState.value = uiState.value.copy(email = newValue) }
    fun onPasswordChange(newValue: String) { uiState.value = uiState.value.copy(password = newValue) }

    fun onDocumentNumberChange(newValue: String) {uiState.value = uiState.value.copy(documentNumber = newValue) }
    fun onDocumentTypeChange(newValue: String) { uiState.value = uiState.value.copy(documentType = newValue) }
    fun onMoradaChange(newValue: String) { uiState.value = uiState.value.copy(morada = newValue) }
    fun onCodPostalChange(newValue: String) { uiState.value = uiState.value.copy(codPostal = newValue) }
    fun onRoleChange(newValue: UserRole) { uiState.value = uiState.value.copy(role = newValue) }

    fun createProfile(onSuccess: () -> Unit) {
        val state = uiState.value

        // 1. Validação de Campos Vazios
        if (state.email.isEmpty() || state.password.isEmpty() || state.nome.isEmpty() || state.numMecanografico.isEmpty()) {
            uiState.value = state.copy(error = "Preencha os campos obrigatórios.")
            return
        }

        // 2. Validação do Formato do Nº Mecanográfico
        val mecanograficoRegex = Regex("^[a-zA-Z]\\d+$")
        if (!state.numMecanografico.matches(mecanograficoRegex)) {
            uiState.value = state.copy(error = "O Nº Mecanográfico deve começar com uma letra seguida de números (ex: f12345).")
            return
        }

        uiState.value = state.copy(isLoading = true, error = null)

        val auth = Firebase.auth

        auth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    saveToFirestore(authUid = userId, onSuccess)
                }
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = e.message ?: "Erro ao criar conta.")
            }
    }

    private fun saveToFirestore(authUid: String, onSuccess: () -> Unit) {
        val state = uiState.value
        val db = Firebase.firestore

        val userMap = hashMapOf(
            "uid" to authUid,
            "numMecanografico" to state.numMecanografico,
            "nome" to state.nome,
            "contacto" to state.contacto,
            "documentNumber" to state.documentNumber,
            "documentType" to state.documentType,
            "morada" to state.morada,
            "codPostal" to state.codPostal,
            "email" to state.email,
            "role" to state.role.name,
            "mudarPass" to true // Força a mudança de senha no próximo login
        )

        val collectionName = if (state.role == UserRole.FUNCIONARIO) "funcionarios" else "apoiados"

        // Lógica específica para Apoiados
        if (state.role == UserRole.APOIADO) {
            userMap["emailApoiado"] = state.email
            // --- VARIÁVEL PARA LEMBRAR DE PREENCHER DADOS ---
            userMap["dadosIncompletos"] = true
        }

        db.collection(collectionName).document(state.numMecanografico)
            .set(userMap)
            .addOnSuccessListener {
                uiState.value = state.copy(isLoading = false, success = true)
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao guardar dados: ${e.message}")
            }
    }
}