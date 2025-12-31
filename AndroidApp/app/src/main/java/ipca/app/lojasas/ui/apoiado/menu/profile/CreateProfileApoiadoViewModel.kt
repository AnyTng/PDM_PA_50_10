package ipca.app.lojasas.ui.apoiado.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.utils.Validators

data class CreateProfileApoiadoState(
    var numMecanografico: String = "",
    var nome: String = "",
    var contacto: String = "",
    var email: String = "",
    var password: String = "",
    var documentNumber: String = "",
    var documentType: String = "NIF",
    var morada: String = "",
    var codPostal: String = "",
    var role: UserRole = UserRole.FUNCIONARIO,
    var isLoading: Boolean = false,
    var error: String? = null,
    var success: Boolean = false
)

class CreateProfileApoiadoViewModel : ViewModel() {

    var uiState = mutableStateOf(CreateProfileApoiadoState())
        private set

    // Funções de update
    fun onNumMecanograficoChange(newValue: String) { uiState.value = uiState.value.copy(numMecanografico = newValue) }
    fun onNomeChange(newValue: String) { uiState.value = uiState.value.copy(nome = newValue) }
    fun onContactoChange(newValue: String) { uiState.value = uiState.value.copy(contacto = newValue) }
    fun onEmailChange(newValue: String) { uiState.value = uiState.value.copy(email = newValue) }
    fun onPasswordChange(newValue: String) { uiState.value = uiState.value.copy(password = newValue) }
    fun onDocumentNumberChange(newValue: String) { uiState.value = uiState.value.copy(documentNumber = newValue) }
    fun onDocumentTypeChange(newValue: String) { uiState.value = uiState.value.copy(documentType = newValue) }
    fun onMoradaChange(newValue: String) { uiState.value = uiState.value.copy(morada = newValue) }
    fun onCodPostalChange(newValue: String) { uiState.value = uiState.value.copy(codPostal = newValue) }
    fun onRoleChange(newValue: UserRole) { uiState.value = uiState.value.copy(role = newValue) }

    fun createProfile(onSuccess: () -> Unit) {
        val s = uiState.value

        // --- Validações (campos obrigatórios + formatos) ---
        val email = s.email.trim()
        val nome = s.nome.trim()
        val numMec = s.numMecanografico.trim()
        val morada = s.morada.trim()

        if (email.isBlank() || s.password.isBlank() || nome.isBlank() || numMec.isBlank()) {
            uiState.value = s.copy(error = "Preencha os campos obrigatórios.")
            return
        }

        if (!Validators.isValidEmail(email)) {
            uiState.value = s.copy(error = "Email inválido.")
            return
        }

        if (s.password.length < 6) {
            uiState.value = s.copy(error = "A password deve ter pelo menos 6 caracteres.")
            return
        }

        if (!Validators.isValidMecanografico(numMec)) {
            uiState.value = s.copy(error = "O Nº Mecanográfico deve começar com uma letra seguida de números (ex: f12345).")
            return
        }

        val contactoNorm = Validators.normalizePhonePT(s.contacto)
        if (contactoNorm == null) {
            uiState.value = s.copy(error = "Contacto inválido. Use 9 dígitos (ex: 912345678) ou +351...")
            return
        }

        if (morada.isBlank()) {
            uiState.value = s.copy(error = "A morada é obrigatória.")
            return
        }

        val codPostalNorm = Validators.normalizePostalCodePT(s.codPostal)
        if (codPostalNorm == null) {
            uiState.value = s.copy(error = "Código Postal inválido. Formato esperado: 1234-567")
            return
        }

        val documentNumberTrim = s.documentNumber.trim()
        if (documentNumberTrim.isBlank()) {
            uiState.value = s.copy(error = "O número de documento é obrigatório.")
            return
        }

        val normalizedDocNumber = if (s.documentType == "NIF") {
            if (!Validators.isValidNif(documentNumberTrim)) {
                uiState.value = s.copy(error = "NIF inválido.")
                return
            }
            Validators.normalizeNif(documentNumberTrim)!!
        } else {
            if (documentNumberTrim.length < 5) {
                uiState.value = s.copy(error = "Nº Passaporte inválido.")
                return
            }
            documentNumberTrim
        }

        val normalizedState = s.copy(
            email = email,
            nome = nome,
            numMecanografico = numMec,
            contacto = contactoNorm,
            morada = morada,
            codPostal = codPostalNorm,
            documentNumber = normalizedDocNumber
        )

        uiState.value = normalizedState.copy(isLoading = true, error = null)

        val auth = Firebase.auth

        auth.createUserWithEmailAndPassword(normalizedState.email, normalizedState.password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    saveToFirestore(authUid = userId, onSuccess)
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message ?: "Erro ao criar conta.")
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
            "mudarPass" to false
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