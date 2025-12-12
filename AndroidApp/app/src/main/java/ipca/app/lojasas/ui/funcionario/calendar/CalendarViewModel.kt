package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class CalendarState(
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "" // Para saber qual documento atualizar
)

class CalendarViewModel : ViewModel() {

    var uiState = mutableStateOf(CalendarState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        checkMudarPassStatus()
    }

    private fun checkMudarPassStatus() {
        val user = auth.currentUser
        if (user != null) {
            // Assume-se que quem vê o Calendário é "Funcionario" (conforme navegação no MainActivity)
            db.collection("funcionarios")
                .whereEqualTo("uid", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val mudarPass = doc.getBoolean("mudarPass") ?: false

                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            showMandatoryPasswordChange = mudarPass,
                            docId = doc.id
                        )
                    } else {
                        uiState.value = uiState.value.copy(isLoading = false)
                    }
                }
                .addOnFailureListener {
                    uiState.value = uiState.value.copy(isLoading = false, error = it.message)
                }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        val state = uiState.value

        if (user != null && email != null && state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true, error = null)

            // 1. Reautenticar (segurança exigida pelo Firebase para mudar senha)
            val credential = EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // 2. Atualizar Senha no Auth
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            // 3. Atualizar Flag no Firestore
                            db.collection("funcionarios").document(state.docId)
                                .update("mudarPass", false)
                                .addOnSuccessListener {
                                    uiState.value = state.copy(
                                        isLoading = false,
                                        showMandatoryPasswordChange = false
                                    )
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    uiState.value = state.copy(isLoading = false, error = "Senha mudou, mas erro ao atualizar BD: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            uiState.value = state.copy(isLoading = false, error = "Erro ao definir nova senha: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    uiState.value = state.copy(isLoading = false, error = "Senha atual incorreta.")
                }
        }
    }
}