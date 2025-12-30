package ipca.app.lojasas.ui.apoiado.menu.help


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Date

class UrgentHelpViewModel : ViewModel() {
    val db = Firebase.firestore

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var success = mutableStateOf(false)

    // Função para submeter o pedido
    fun submitUrgentRequest(numeroMecanografico: String, descricao: String, onSuccess: () -> Unit) {
        if (descricao.isBlank()) {
            error.value = "A descrição não pode estar vazia."
            return
        }

        isLoading.value = true

        val pedido = hashMapOf(
            "numeroMecanografico" to numeroMecanografico,
            "descricao" to descricao,
            "estado" to "Analise",
            "dataSubmissao" to Date(),
            "tipo" to "Urgente"
        )

        db.collection("pedidos_ajuda")
            .add(pedido)
            .addOnSuccessListener {
                isLoading.value = false
                success.value = true
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                error.value = "Erro ao enviar: ${e.message}"
            }
    }
}