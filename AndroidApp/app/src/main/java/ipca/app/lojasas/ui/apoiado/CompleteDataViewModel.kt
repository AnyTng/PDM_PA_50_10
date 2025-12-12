package ipca.app.lojasas.ui.apoiado

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.Date

data class CompleteDataState(
    var relacaoIPCA: String = "", // "Estudante", "Funcionário", "Docente"
    var curso: String = "",
    var graoEnsino: String = "", // "CTeSP", "Licenciatura", "Mestrado"
    var apoioEmergencia: Boolean = false,
    var bolsaEstudos: Boolean = false,
    var valorBolsa: String = "", // String para input, converter para Int depois
    var dataNascimento: Long? = null, // Timestamp do DatePicker
    var nacionalidade: String = "",
    var necessidades: List<String> = emptyList(),

    var isLoading: Boolean = false,
    var error: String? = null
)

class CompleteDataViewModel : ViewModel() {

    var uiState = mutableStateOf(CompleteDataState())
        private set

    val db = Firebase.firestore

    // Funções de UI
    fun onRelacaoChange(value: String) { uiState.value = uiState.value.copy(relacaoIPCA = value) }
    fun onCursoChange(value: String) { uiState.value = uiState.value.copy(curso = value) }
    fun onGraoChange(value: String) { uiState.value = uiState.value.copy(graoEnsino = value) }
    fun onApoioEmergenciaChange(value: Boolean) { uiState.value = uiState.value.copy(apoioEmergencia = value) }
    fun onBolsaChange(value: Boolean) { uiState.value = uiState.value.copy(bolsaEstudos = value) }
    fun onValorBolsaChange(value: String) { uiState.value = uiState.value.copy(valorBolsa = value) }
    fun onDataNascimentoChange(value: Long?) { uiState.value = uiState.value.copy(dataNascimento = value) }
    fun onNacionalidadeChange(value: String) { uiState.value = uiState.value.copy(nacionalidade = value) }

    fun toggleNecessidade(item: String) {
        val currentList = uiState.value.necessidades.toMutableList()
        if (currentList.contains(item)) {
            currentList.remove(item)
        } else {
            currentList.add(item)
        }
        uiState.value = uiState.value.copy(necessidades = currentList)
    }

    fun submitData(docId: String, onSuccess: () -> Unit) {
        val state = uiState.value

        // Validações básicas
        if (state.relacaoIPCA.isEmpty() || state.nacionalidade.isEmpty() || state.dataNascimento == null || state.necessidades.isEmpty()) {
            uiState.value = state.copy(error = "Preencha os campos obrigatórios e selecione pelo menos uma necessidade.")
            return
        }
        if (state.relacaoIPCA == "Estudante" && (state.curso.isEmpty() || state.graoEnsino.isEmpty())) {
            uiState.value = state.copy(error = "Estudantes devem indicar Curso e Grau.")
            return
        }

        uiState.value = state.copy(isLoading = true, error = null)

        val updateMap = hashMapOf<String, Any>(
            "relacaoIPCA" to state.relacaoIPCA,
            "apoioEmergenciaSocial" to state.apoioEmergencia,
            "bolsaEstudos" to state.bolsaEstudos,
            "nacionalidade" to state.nacionalidade,
            "necessidade" to state.necessidades,
            "dataNascimento" to Date(state.dataNascimento!!), // Converte Long para Date (Timestamp no Firebase)

            // Campos Fixos pedidos
            "estadoConta" to "Falta_Documentos",
            "dadosIncompletos" to false
        )

        // Campos Condicionais
        if (state.relacaoIPCA == "Estudante") {
            updateMap["curso"] = state.curso
            updateMap["graoEnsino"] = state.graoEnsino
        }
        if (state.bolsaEstudos && state.valorBolsa.isNotEmpty()) {
            updateMap["valorBolsaEstudos"] = state.valorBolsa.toIntOrNull() ?: 0
        }

        db.collection("apoiados").document(docId)
            .update(updateMap)
            .addOnSuccessListener {
                uiState.value = state.copy(isLoading = false)
                onSuccess()
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Erro ao guardar: ${e.message}")
            }
    }
}