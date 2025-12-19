package ipca.app.lojasas.ui.apoiado.menu

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MenuApoiadoViewModel : ViewModel() {

    var isApproved = mutableStateOf(false)
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
                        // Se estiver aprovado, fica true
                        isApproved.value = (estado == "Aprovado" || estado == "Suspenso")
                    }
                }
        }
    }
}