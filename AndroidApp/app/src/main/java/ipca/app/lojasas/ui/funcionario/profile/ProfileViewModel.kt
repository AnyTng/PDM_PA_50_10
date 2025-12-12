package ipca.app.lojasas.ui.funcionario.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.UserRole

data class ProfileState(
    var numMecanografico: String = "",
    var nome: String = "",
    var contacto: String = "",
    var email: String = "",
    var nif: String = "",
    var morada: String = "",
    var codPostal: String = "",
    var role: UserRole? = null,
    var isLoading: Boolean = true,
    var error: String? = null,
    var success: Boolean = false
)

class ProfileViewModel : ViewModel() {

    var uiState = mutableStateOf(ProfileState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Apenas os campos permitidos para edição
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

        // Tentar encontrar na coleção 'funcionarios'
        db.collection("funcionarios").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    fillState(doc.data, doc.id, UserRole.FUNCIONARIO)
                } else {
                    // Se não encontrar, tentar na coleção 'apoiados'
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
        uiState.value = ProfileState(
            numMecanografico = docId,
            nome = data["nome"] as? String ?: "",
            contacto = data["contacto"] as? String ?: "",
            email = data["email"] as? String ?: data["emailApoiado"] as? String ?: "",
            nif = data["nif"] as? String ?: "",
            morada = data["morada"] as? String ?: "",
            codPostal = data["codPostal"] as? String ?: "",
            role = role,
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

        // Atualizar apenas os campos permitidos
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

        uiState.value = state.copy(isLoading = true)

        val collectionName = if (state.role == UserRole.FUNCIONARIO) "funcionarios" else "apoiados"
        val user = auth.currentUser

        // 1. Apagar documento do Firestore
        db.collection(collectionName).document(state.numMecanografico)
            .delete()
            .addOnSuccessListener {
                // 2. Apagar utilizador da Autenticação
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
}