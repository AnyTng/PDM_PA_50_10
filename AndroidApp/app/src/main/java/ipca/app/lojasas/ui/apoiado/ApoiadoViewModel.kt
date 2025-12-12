package ipca.app.lojasas.ui.apoiado

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class ApoiadoState(
    val dadosIncompletos: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "" // Guardamos o ID (nº mecanográfico) para usar no update
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

                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            dadosIncompletos = isIncomplete,
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
}