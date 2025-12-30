package ipca.app.lojasas.ui.apoiado.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- NOVOS MODELOS DE DADOS (Para o Design) ---
data class Cesta(
    val id: String = "",
    // Na BD (ver imagens) o campo principal é "dataRecolha".
    // Mantemos o modelo simples para a Home.
    val dataRecolha: Date? = null,
    val dataAgendada: Date? = null,
    val estadoCesta: String = "",
    val numeroItens: Int = 0
)

// Mantido o teu modelo original de Pedido
data class UrgentRequest(
    val id: String,
    val descricao: String,
    val estado: String,
    val data: Date?,
    val tipo: String
)

data class ApoiadoState(
    // --- Campos Originais (Necessários para a App funcionar) ---
    val dadosIncompletos: Boolean = false,
    val faltaDocumentos: Boolean = false,
    val estadoConta: String = "",
    val motivoNegacao: String? = null,
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "",
    val nome: String = "",
    val validadeConta: String? = null,
    val validade: Date? = null,
    val numeroMecanografico: String = "",
    val urgentRequests: List<UrgentRequest> = emptyList(), // Usado para o cartão Azul

    // --- NOVOS CAMPOS (Para os cartões Verdes) ---
    val cestasPendentes: List<Cesta> = emptyList(),
    val cestasRealizadas: List<Cesta> = emptyList(),
    val cestasNaoLevantadas: List<Cesta> = emptyList()
)

class ApoiadoViewModel : ViewModel() {

    var uiState = mutableStateOf(ApoiadoState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Evita múltiplos listeners duplicados quando a Home faz refresh (ex: após mudar password)
    private var cestasListener: ListenerRegistration? = null
    private var urgentRequestsListener: ListenerRegistration? = null

    init {
        checkStatus()
    }

    fun checkStatus() {
        val user = auth.currentUser
        if (user != null) {
            uiState.value = uiState.value.copy(isLoading = true)

            // 1. Lógica de Perfil
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
                        val numMec = doc.getString("numMecanografico") ?: doc.getString("numeroMecanografico") ?: ""

                        val validadeTimestamp = doc.getTimestamp("validadeConta") ?: doc.getTimestamp("validade")
                        val validadeDate = validadeTimestamp?.toDate()
                        val validadeString = validadeDate?.let {
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
                            validade = validadeDate,
                            validadeConta = validadeString,
                            numeroMecanografico = numMec
                        )

                        // 2. Carregar CESTAS com base no ID do Apoiado (doc.id) / nº mecanográfico
                        fetchCestas(
                            apoiadoDocId = doc.id,
                            numeroMecanografico = numMec,
                            uid = user.uid
                        )

                        if (numMec.isNotEmpty()) {
                            fetchUrgentRequests(numMec)
                        }

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

    // --- NOVA FUNÇÃO: Busca as cestas para os cartões verdes ---
    private fun fetchCestas(apoiadoDocId: String, numeroMecanografico: String, uid: String) {
        // Remove listener anterior (evita duplicação)
        cestasListener?.remove()

        // Na BD (prints): campo é "apoiadoID". Alguns projetos também guardam o nº mecanográfico.
        val keys = listOf(apoiadoDocId, numeroMecanografico).filter { it.isNotBlank() }.distinct()

        if (keys.isEmpty()) {
            uiState.value = uiState.value.copy(
                cestasPendentes = emptyList(),
                cestasRealizadas = emptyList(),
                cestasNaoLevantadas = emptyList()
            )
            return
        }

        val baseQuery = db.collection("cestas")

        // Preferimos whereIn para cobrir docId + numMec. Se só houver 1 chave, usamos whereEqualTo.
        val query = if (keys.size == 1) {
            baseQuery.whereEqualTo("apoiadoID", keys.first())
        } else {
            baseQuery.whereIn("apoiadoID", keys)
        }

        cestasListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ApoiadoHome", "Erro ao ler cestas", e)
                return@addSnapshotListener
            }

            val now = Date()

            val todasCestas = snapshot?.documents?.map { doc ->
                val dataRecolha = doc.getTimestamp("dataRecolha")?.toDate()
                val dataAgendada = doc.getTimestamp("dataAgendada")?.toDate()
                val estadoCesta = doc.getString("estadoCesta") ?: ""
                val produtos = (doc.get("produtos") as? List<*>) ?: emptyList<Any?>()

                Cesta(
                    id = doc.id,
                    dataRecolha = dataRecolha,
                    dataAgendada = dataAgendada,
                    estadoCesta = estadoCesta,
                    numeroItens = produtos.size
                )
            } ?: emptyList()

            fun isCompleted(estado: String): Boolean {
                val eLower = estado.trim().lowercase(Locale.getDefault())
                return eLower.contains("entreg") ||
                        eLower.contains("levant") ||
                        eLower.contains("conclu") ||
                        eLower.contains("finaliz")
            }

            fun isMissed(estado: String, data: Date?): Boolean {
                val eLower = estado.trim().lowercase(Locale.getDefault())
                val explicit = eLower.contains("não levant") || eLower.contains("nao levant")
                val pastNotCompleted = (data != null && data.before(now) && !isCompleted(estado))
                return explicit || pastNotCompleted
            }

            val naoLevantadas = todasCestas
                .filter { isMissed(it.estadoCesta, it.dataRecolha) }
                .sortedByDescending { it.dataRecolha }

            val realizadas = todasCestas
                .filter { isCompleted(it.estadoCesta) }
                .sortedByDescending { it.dataRecolha }

            val pendentes = todasCestas
                .filter { !isCompleted(it.estadoCesta) && !isMissed(it.estadoCesta, it.dataRecolha) }
                .sortedBy { it.dataRecolha }

            uiState.value = uiState.value.copy(
                cestasPendentes = pendentes,
                cestasRealizadas = realizadas,
                cestasNaoLevantadas = naoLevantadas
            )
        }
    }

    // --- FUNÇÕES ORIGINAIS MANTIDAS ---

    private fun fetchUrgentRequests(numMec: String) {
        urgentRequestsListener?.remove()

        urgentRequestsListener = db.collection("pedidos_ajuda")
            .whereEqualTo("numeroMecanografico", numMec)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val requests = value?.documents?.map { doc ->
                    UrgentRequest(
                        id = doc.id,
                        descricao = doc.getString("descricao") ?: "",
                        estado = doc.getString("estado") ?: "Analise",
                        data = doc.getTimestamp("dataSubmissao")?.toDate(),
                        tipo = doc.getString("tipo") ?: "Ajuda"
                    )
                } ?: emptyList()

                val sortedRequests = requests.sortedByDescending { it.data }

                uiState.value = uiState.value.copy(urgentRequests = sortedRequests)
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

    fun resetToTryAgain(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true)

            db.collection("apoiados").document(state.docId)
                .update(
                    mapOf(
                        "estadoConta" to "Correcao_Dados",
                        "dadosIncompletos" to true,
                        "faltaDocumentos" to false
                    )
                )
                .addOnSuccessListener {
                    checkStatus()
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
                                    uiState.value = state.copy(isLoading = false, showMandatoryPasswordChange = false)
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    uiState.value = state.copy(isLoading = false, error = "Erro BD: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            uiState.value = state.copy(isLoading = false, error = "Erro senha: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    uiState.value = state.copy(isLoading = false, error = "Senha incorreta.")
                }
        }
    }
}