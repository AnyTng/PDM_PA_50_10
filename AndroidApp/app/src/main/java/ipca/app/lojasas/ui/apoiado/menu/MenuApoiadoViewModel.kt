package ipca.app.lojasas.ui.apoiado.menu

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MenuApoiadoViewModel : ViewModel() {
    var isBlock = mutableStateOf(false)
        private set
    var isApproved = mutableStateOf(false)
        private set

    // 1. Adicionar esta variável de estado
    var numeroMecanografico = mutableStateOf("")
        private set

    init {
        checkStatus()
    }

    private fun checkStatus() {
        val user = Firebase.auth.currentUser
        val db = Firebase.firestore

        if (user != null) {
            db.collection("apoiados")
                .whereEqualTo("uid", user.uid)
                .addSnapshotListener { documents, e ->
                    if (e == null && documents != null && !documents.isEmpty) {
                        val doc = documents.documents[0]
                        val estado = doc.getString("estadoConta") ?: ""

                        // 2. Ler o número mecanográfico do documento
                        val numMec = doc.getString("numMecanografico") ?: ""
                        isBlock.value = (estado == "Bloqueado")
                        isApproved.value = (estado == "Aprovado" || estado == "Suspenso")
                        numeroMecanografico.value = numMec // Atualizar o estado
                    }
                }
        }
    }
}