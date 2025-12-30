package ipca.app.lojasas.ui.funcionario.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class CreateProfileState(
    var numMecanografico: String = "",
    var nome: String = "",
    var contacto: String = "",
    var email: String = "",
    var password: String = "",
    var documentNumber: String = "",
    var documentType: String = "NIF",
    var morada: String = "",
    var codPostal: String = "",
    var isLoading: Boolean = false,
    var error: String? = null,
    var success: Boolean = false,
    var selectedRole: String = "Funcionario"
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
    fun onDocumentNumberChange(newValue: String) { uiState.value = uiState.value.copy(documentNumber = newValue) }
    fun onDocumentTypeChange(newValue: String) { uiState.value = uiState.value.copy(documentType = newValue) }
    fun onMoradaChange(newValue: String) { uiState.value = uiState.value.copy(morada = newValue) }
    fun onCodPostalChange(newValue: String) { uiState.value = uiState.value.copy(codPostal = newValue) }
    fun onRoleChange(newRole: String) { uiState.value = uiState.value.copy(selectedRole = newRole) }

    fun createProfile(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.email.isEmpty() || state.password.isEmpty() || state.nome.isEmpty() || state.numMecanografico.isEmpty()) {
            uiState.value = state.copy(error = "Preencha os campos obrigatórios.")
            return
        }

        val mecanograficoRegex = Regex("^[a-zA-Z]\\d+$")
        if (!state.numMecanografico.matches(mecanograficoRegex)) {
            uiState.value = state.copy(error = "O Nº Mecanográfico deve começar com uma letra seguida de números (ex: f12345).")
            return
        }

        uiState.value = state.copy(isLoading = true, error = null)

        // 1. Configurar uma App Firebase Secundária para não afetar a sessão atual
        val tempAppName = "SecondaryApp"
        val currentApp = Firebase.app

        val tempApp = try {
            FirebaseApp.getInstance(tempAppName)
        } catch (e: Exception) {
            FirebaseApp.initializeApp(currentApp.applicationContext, currentApp.options, tempAppName)
        }

        // 2. Usar o Auth dessa app secundária
        val tempAuth = Firebase.auth(tempApp)

        tempAuth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    // Importante: Fazer logout da app secundária imediatamente
                    tempAuth.signOut()

                    // 3. Guardar no Firestore (usando a instância principal que ainda tem o Admin logado)
                    saveToFirestore(authUid = userId, onSuccess)
                }
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = e.message ?: "Erro ao criar conta.")
            }
    }

    private fun saveToFirestore(authUid: String, onSuccess: () -> Unit) {
        val state = uiState.value
        val db = Firebase.firestore // Usa a instância DEFAULT (Admin logado)

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
            "role" to state.selectedRole,
            "mudarPass" to true
        )

        db.collection("funcionarios").document(state.numMecanografico)
            .set(userMap)
            .addOnSuccessListener {
                db.collection("users").document(authUid).set(mapOf("role" to "Funcionario", "email" to state.email))
                    .addOnSuccessListener {
                        uiState.value = state.copy(isLoading = false, success = true)
                        onSuccess()
                    }
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao guardar dados: ${e.message}")
            }
    }
}