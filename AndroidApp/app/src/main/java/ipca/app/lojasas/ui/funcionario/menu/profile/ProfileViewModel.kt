package ipca.app.lojasas.ui.funcionario.menu.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.UserRole
import java.util.Date

data class ProfileState(
    var numMecanografico: String = "",
    var nome: String = "",
    var contacto: String = "",
    var email: String = "",
    var documentNumber: String = "",
    var documentType: String = "NIF",
    var morada: String = "",
    var codPostal: String = "",
    var role: UserRole? = null,

    // --- CONTROLO DE ADMIN ---
    var isAdmin: Boolean = false, // Novo campo para controlar a UI

    // --- CAMPOS DO FORMULÁRIO (Mantidos do teu código) ---
    var nacionalidade: String = "",
    var dataNascimento: Date? = null,
    var relacaoIPCA: String = "",
    var curso: String = "",
    var graoEnsino: String = "",
    var apoioEmergencia: Boolean = false,
    var bolsaEstudos: Boolean = false,
    var valorBolsa: String = "",
    var necessidades: List<String> = emptyList(),
    var validadeConta: Date? = null,

    var isLoading: Boolean = true,
    var error: String? = null,
    var success: Boolean = false
)

class ProfileViewModel : ViewModel() {

    var uiState = mutableStateOf(ProfileState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    fun onNomeChange(newValue: String) { uiState.value = uiState.value.copy(nome = newValue) }
    fun onContactoChange(newValue: String) { uiState.value = uiState.value.copy(contacto = newValue) }
    fun onMoradaChange(newValue: String) { uiState.value = uiState.value.copy(morada = newValue) }
    fun onCodPostalChange(newValue: String) { uiState.value = uiState.value.copy(codPostal = newValue) }

    fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            return
        }

        val uid = currentUser.uid
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        // Tenta encontrar em Funcionários
        db.collection("funcionarios").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    fillState(doc.data, doc.id, UserRole.FUNCIONARIO)
                } else {
                    // Se não for funcionário, tenta Apoiados
                    db.collection("apoiados").whereEqualTo("uid", uid).get()
                        .addOnSuccessListener { apoiadoDocs ->
                            if (!apoiadoDocs.isEmpty) {
                                val doc = apoiadoDocs.documents[0]
                                fillState(doc.data, doc.id, UserRole.APOIADO)
                            } else {
                                uiState.value = uiState.value.copy(isLoading = false, error = "Perfil não encontrado.")
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = e.message)
            }
    }

    private fun fillState(data: Map<String, Any>?, docId: String, role: UserRole) {
        if (data == null) return

        val dtNasc = try {
            (data["dataNascimento"] as? com.google.firebase.Timestamp)?.toDate()
        } catch (e: Exception) { null }

        val dtVal = try {
            (data["validadeConta"] as? com.google.firebase.Timestamp)?.toDate()
        } catch (e: Exception) { null }

        // Verifica se é Admin baseado no campo 'role' (String) da BD
        val roleString = data["role"] as? String ?: ""
        val isAdminUser = role == UserRole.FUNCIONARIO && roleString.equals("Admin", ignoreCase = true)

        uiState.value = ProfileState(
            numMecanografico = docId,
            nome = data["nome"] as? String ?: "",
            contacto = data["contacto"] as? String ?: "",
            email = data["email"] as? String ?: data["emailApoiado"] as? String ?: "",
            documentNumber = data["documentNumber"] as? String ?: "",
            documentType = data["documentType"] as? String ?: "NIF",
            morada = data["morada"] as? String ?: "",
            codPostal = data["codPostal"] as? String ?: "",
            role = role,
            isAdmin = isAdminUser, // Define se é admin

            nacionalidade = data["nacionalidade"] as? String ?: "",
            dataNascimento = dtNasc,
            relacaoIPCA = data["relacaoIPCA"] as? String ?: "",
            curso = data["curso"] as? String ?: "",
            graoEnsino = data["graoEnsino"] as? String ?: "",
            apoioEmergencia = data["apoioEmergenciaSocial"] as? Boolean ?: false,
            bolsaEstudos = data["bolsaEstudos"] as? Boolean ?: false,
            valorBolsa = (data["valorBolsa"] as? String) ?: "",
            necessidades = (data["necessidade"] as? List<String>) ?: emptyList(),
            validadeConta = dtVal,

            isLoading = false
        )
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.nome.isEmpty()) {
            uiState.value = state.copy(error = "O nome não pode estar vazio.")
            return
        }

        uiState.value = state.copy(isLoading = true)
        val collectionName = if (state.role == UserRole.FUNCIONARIO) "funcionarios" else "apoiados"

        val updates = hashMapOf<String, Any>(
            "nome" to state.nome,
            "contacto" to state.contacto,
            "morada" to state.morada,
            "codPostal" to state.codPostal
        )

        db.collection(collectionName).document(state.numMecanografico)
            .update(updates)
            .addOnSuccessListener {
                uiState.value = state.copy(isLoading = false, success = true)
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao atualizar: ${e.message}")
            }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.role == null || state.numMecanografico.isEmpty()) return

        // Validação extra de segurança no ViewModel
        if (state.isAdmin) {
            uiState.value = state.copy(error = "Administradores não podem apagar a própria conta.")
            return
        }

        uiState.value = state.copy(isLoading = true)
        val collectionName = if (state.role == UserRole.FUNCIONARIO) "funcionarios" else "apoiados"
        val user = auth.currentUser

        db.collection(collectionName).document(state.numMecanografico)
            .delete()
            .addOnSuccessListener {
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        uiState.value = state.copy(isLoading = false)
                        onSuccess()
                    } else {
                        uiState.value = state.copy(isLoading = false, error = "Erro ao apagar conta Auth: ${task.exception?.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao apagar dados: ${e.message}")
            }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user != null && email != null) {
            uiState.value = uiState.value.copy(isLoading = true)

            val credential = EmailAuthProvider.getCredential(email, oldPass)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            uiState.value = uiState.value.copy(isLoading = false)
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            uiState.value = uiState.value.copy(isLoading = false)
                            onError("Erro ao atualizar senha: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    uiState.value = uiState.value.copy(isLoading = false)
                    onError("Senha antiga incorreta.")
                }
        }
    }
}