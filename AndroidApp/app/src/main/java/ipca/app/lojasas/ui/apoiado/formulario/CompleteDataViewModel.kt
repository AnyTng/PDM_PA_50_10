package ipca.app.lojasas.ui.apoiado.formulario

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
    var valorBolsa: String = "",
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

    // Funções de UI para atualizar o estado
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

    // --- NOVA FUNÇÃO: Carregar dados existentes (CORREÇÃO DO ERRO) ---
    fun loadData(docId: String) {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("apoiados").document(docId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Converter Timestamp do Firestore para Date/Long
                        val timestamp = (data["dataNascimento"] as? com.google.firebase.Timestamp)

                        uiState.value = uiState.value.copy(
                            relacaoIPCA = data["relacaoIPCA"] as? String ?: "",
                            curso = data["curso"] as? String ?: "",
                            graoEnsino = data["graoEnsino"] as? String ?: "",
                            apoioEmergencia = data["apoioEmergenciaSocial"] as? Boolean ?: false,
                            bolsaEstudos = data["bolsaEstudos"] as? Boolean ?: false,
                            valorBolsa = (data["valorBolsa"] as? String) ?: "",
                            nacionalidade = data["nacionalidade"] as? String ?: "",
                            necessidades = (data["necessidade"] as? List<String>) ?: emptyList(),
                            dataNascimento = timestamp?.toDate()?.time,
                            isLoading = false
                        )
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false)
                }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = it.message)
            }
    }

    fun submitData(docId: String, onSuccess: () -> Unit) {
        val state = uiState.value
        uiState.value = state.copy(isLoading = true, error = null)

        // Se tem apoio de emergência, não precisa de submeter documentos (exemplo lógico)
        // Se NÃO tem apoio, FALTA documentos.
        val precisaDocumentos = !state.apoioEmergencia

        val updateMap = hashMapOf<String, Any>(
            "relacaoIPCA" to state.relacaoIPCA,
            "apoioEmergenciaSocial" to state.apoioEmergencia,
            "bolsaEstudos" to state.bolsaEstudos,
            "nacionalidade" to state.nacionalidade,
            "necessidade" to state.necessidades,
            // Guarda a data. Se for null usa a data atual (evita crash), mas idealmente deve validar antes.
            "dataNascimento" to if (state.dataNascimento != null) Date(state.dataNascimento!!) else Date(),

            "estadoConta" to if (precisaDocumentos) "Falta_Documentos" else "Analise",
            "dadosIncompletos" to false,
            "faltaDocumentos" to precisaDocumentos
        )

        // Adiciona campos opcionais apenas se preenchidos
        if (state.curso.isNotEmpty()) updateMap["curso"] = state.curso
        if (state.graoEnsino.isNotEmpty()) updateMap["graoEnsino"] = state.graoEnsino
        if (state.valorBolsa.isNotEmpty()) updateMap["valorBolsa"] = state.valorBolsa

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