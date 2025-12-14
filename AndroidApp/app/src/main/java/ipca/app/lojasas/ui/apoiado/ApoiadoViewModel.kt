// Ficheiro: lojasas/ui/apoiado/ApoiadoViewModel.kt

package ipca.app.lojasas.ui.apoiado

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class ApoiadoState(
    val dadosIncompletos: Boolean = false,
    val faltaDocumentos: Boolean = false, // <--- NOVO CAMPO
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = ""
)

class ApoiadoViewModel : ViewModel() {

    var uiState = mutableStateOf(ApoiadoState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        checkStatus()
    }

    fun checkStatus() {
        val user = auth.currentUser
        if (user != null) {
            uiState.value = uiState.value.copy(isLoading = true)

            db.collection("apoiados")
                .whereEqualTo("uid", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]

                        val isIncomplete = doc.getBoolean("dadosIncompletos") ?: false
                        val mudarPass = doc.getBoolean("mudarPass") ?: false
                        val faltaDocs = doc.getBoolean("faltaDocumentos") ?: false // <--- LÊ DA BD

                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            dadosIncompletos = isIncomplete,
                            faltaDocumentos = faltaDocs, // <--- ATUALIZA ESTADO
                            showMandatoryPasswordChange = mudarPass,
                            docId = doc.id
                        )
                    } else {
                        uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não encontrado.")
                    }
                }
                .addOnFailureListener {
                    uiState.value = uiState.value.copy(isLoading = false, error = it.message)
                }
        }
    }

    // Função para trocar a senha (cópia adaptada do CalendarViewModel)
    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        val state = uiState.value

        if (user != null && email != null && state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true, error = null)

            val credential = EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            // Atualiza a flag na coleção 'apoiados'
                            db.collection("apoiados").document(state.docId)
                                .update("mudarPass", false)
                                .addOnSuccessListener {
                                    uiState.value = state.copy(
                                        isLoading = false,
                                        showMandatoryPasswordChange = false
                                    )
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    uiState.value = state.copy(isLoading = false, error = "Erro ao atualizar BD: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            uiState.value = state.copy(isLoading = false, error = "Erro ao definir senha: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    uiState.value = state.copy(isLoading = false, error = "Senha atual incorreta.")
                }
        }
    }
}