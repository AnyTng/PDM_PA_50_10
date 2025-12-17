package ipca.app.lojasas.ui.apoiado.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale

data class ApoiadoState(
    val dadosIncompletos: Boolean = false,
    val faltaDocumentos: Boolean = false,
    val estadoConta: String = "",
    val motivoNegacao: String? = null,
    val validadeConta: String? = null,
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "",
    val nome: String = ""
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
                        val faltaDocs = doc.getBoolean("faltaDocumentos") ?: false
                        val estado = doc.getString("estadoConta") ?: ""
                        val nomeUser = doc.getString("nome") ?: "Utilizador"

                        val validadeTimestamp = doc.getTimestamp("validadeConta")
                        val validadeString = validadeTimestamp?.toDate()?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                        }

                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            dadosIncompletos = isIncomplete,
                            faltaDocumentos = faltaDocs,
                            estadoConta = estado,
                            showMandatoryPasswordChange = mudarPass,
                            docId = doc.id,
                            nome = nomeUser,
                            validadeConta = validadeString
                        )

                        if (estado == "Negado") {
                            fetchDenialReason(doc.id)
                        }
                    } else {
                        uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não encontrado.")
                    }
                }
                .addOnFailureListener {
                    uiState.value = uiState.value.copy(isLoading = false, error = it.message)
                }
        }
    }

    private fun fetchDenialReason(docId: String) {
        db.collection("apoiados").document(docId)
            .collection("JustificacoesNegacao")
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val motivo = docs.documents[0].getString("motivo")
                    uiState.value = uiState.value.copy(motivoNegacao = motivo)
                }
            }
    }

    // --- ALTERAÇÃO PRINCIPAL AQUI ---
    fun resetToTryAgain(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true)

            // Define "dadosIncompletos" como true para forçar o formulário a abrir
            db.collection("apoiados").document(state.docId)
                .update(
                    mapOf(
                        "estadoConta" to "Correcao_Dados",
                        "dadosIncompletos" to true,
                        "faltaDocumentos" to false // Será recalculado após o formulário
                    )
                )
                .addOnSuccessListener {
                    checkStatus() // Atualiza o estado local para refletir a mudança
                    onSuccess()
                }
                .addOnFailureListener {
                    uiState.value = state.copy(isLoading = false, error = "Erro ao reiniciar: ${it.message}")
                }
        }
    }

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