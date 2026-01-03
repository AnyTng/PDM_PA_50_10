package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.Date

// Tipos de Eventos para colorir no calendário
enum class EventType {
    CAMPAIGN_START,
    CAMPAIGN_END,
    PRODUCT_EXPIRY,
    BASKET_DELIVERY
}

// Modelo único para o Calendário
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: Date,
    val type: EventType,
    val description: String = ""
)

data class CalendarState(
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "",
    val events: List<CalendarEvent> = emptyList(), // Lista de todos os eventos
    val selectedDate: Date = Date() // Data selecionada no UI
)

class CalendarViewModel : ViewModel() {

    var uiState = mutableStateOf(CalendarState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var loadSequence = 0

    init {
        checkMudarPassStatus()
    }

    // --- CARREGAMENTO DE EVENTOS ---
    fun loadEventsForMonth(reference: Calendar) {
        val loadId = ++loadSequence
        val (start, end) = monthRange(reference)
        uiState.value = uiState.value.copy(isLoading = true, error = null, events = emptyList())

        val eventsList = mutableListOf<CalendarEvent>()
        var pending = 4
        var hasError = false

        fun finish() {
            pending -= 1
            if (pending == 0 && loadId == loadSequence) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    events = eventsList,
                    error = if (hasError) "Erro parcial ao carregar eventos." else null
                )
            }
        }

        // 1. Campanhas (início)
        db.collection("campanha")
            .whereGreaterThanOrEqualTo("dataInicio", start)
            .whereLessThan("dataInicio", end)
            .get()
            .addOnSuccessListener { campSnap ->
                campSnap.documents.forEach { doc ->
                    val nome = doc.getString("nomeCampanha") ?: "Campanha"
                    val inicio = doc.getTimestamp("dataInicio")?.toDate()
                        ?: (doc.get("dataInicio") as? Date)
                    if (inicio != null) {
                        eventsList.add(
                            CalendarEvent(
                                doc.id + "_start",
                                "Início: $nome",
                                inicio,
                                EventType.CAMPAIGN_START
                            )
                        )
                    }
                }
                finish()
            }
            .addOnFailureListener {
                hasError = true
                finish()
            }

        // 2. Campanhas (fim)
        db.collection("campanha")
            .whereGreaterThanOrEqualTo("dataFim", start)
            .whereLessThan("dataFim", end)
            .get()
            .addOnSuccessListener { campSnap ->
                campSnap.documents.forEach { doc ->
                    val nome = doc.getString("nomeCampanha") ?: "Campanha"
                    val fim = doc.getTimestamp("dataFim")?.toDate()
                        ?: (doc.get("dataFim") as? Date)
                    if (fim != null) {
                        eventsList.add(
                            CalendarEvent(
                                doc.id + "_end",
                                "Fim: $nome",
                                fim,
                                EventType.CAMPAIGN_END
                            )
                        )
                    }
                }
                finish()
            }
            .addOnFailureListener {
                hasError = true
                finish()
            }

        // 3. Validade de Produtos
        db.collection("produtos")
            .whereGreaterThanOrEqualTo("validade", start)
            .whereLessThan("validade", end)
            .get()
            .addOnSuccessListener { prodSnap ->
                prodSnap.documents.forEach { doc ->
                    val validade = doc.getTimestamp("validade")?.toDate()
                        ?: (doc.get("validade") as? Date)
                    val nome = doc.getString("nomeProduto") ?: "Produto"
                    val marca = doc.getString("marca") ?: ""

                    if (validade != null) {
                        eventsList.add(
                            CalendarEvent(
                                id = doc.id,
                                title = "Validade: $nome ($marca)",
                                date = validade,
                                type = EventType.PRODUCT_EXPIRY
                            )
                        )
                    }
                }
                finish()
            }
            .addOnFailureListener {
                hasError = true
                finish()
            }

        // 4. Entregas de Cestas
        db.collection("cestas")
            .whereGreaterThanOrEqualTo("dataRecolha", start)
            .whereLessThan("dataRecolha", end)
            .get()
            .addOnSuccessListener { cestaSnap ->
                cestaSnap.documents.forEach { doc ->
                    val dataRecolha = doc.getTimestamp("dataRecolha")?.toDate()
                        ?: (doc.get("dataRecolha") as? Date)
                    val apoiadoId = doc.getString("apoiadoID") ?: "Desconhecido"
                    val estado = doc.getString("estadoCesta") ?: ""

                    if (dataRecolha != null) {
                        eventsList.add(
                            CalendarEvent(
                                id = doc.id,
                                title = "Entrega Cesta ($estado)",
                                description = "Apoiado: $apoiadoId",
                                date = dataRecolha,
                                type = EventType.BASKET_DELIVERY
                            )
                        )
                    }
                }
                finish()
            }
            .addOnFailureListener {
                hasError = true
                finish()
            }
    }

    fun ensureSelectedDateInMonth(reference: Calendar) {
        val current = uiState.value.selectedDate
        val selectedCal = Calendar.getInstance().apply { time = current }
        if (selectedCal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
            selectedCal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
        ) {
            return
        }

        val target = (reference.clone() as Calendar).apply {
            val targetDay = selectedCal.get(Calendar.DAY_OF_MONTH)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
        }
        uiState.value = uiState.value.copy(selectedDate = target.time)
    }

    private fun monthRange(reference: Calendar): Pair<Date, Date> {
        val start = (reference.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return start.time to end.time
    }

    fun selectDate(date: Date) {
        uiState.value = uiState.value.copy(selectedDate = date)
    }

    // --- LÓGICA DE PASSWORD (Mantida do original) ---
    private fun checkMudarPassStatus() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("funcionarios").whereEqualTo("uid", user.uid).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val mudarPass = doc.getBoolean("mudarPass") ?: false
                        uiState.value = uiState.value.copy(showMandatoryPasswordChange = mudarPass, docId = doc.id)
                    }
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
            user.reauthenticate(credential).addOnSuccessListener {
                user.updatePassword(newPass).addOnSuccessListener {
                    db.collection("funcionarios").document(state.docId).update("mudarPass", false)
                        .addOnSuccessListener {
                            uiState.value = state.copy(isLoading = false, showMandatoryPasswordChange = false)
                            onSuccess()
                        }
                }.addOnFailureListener { e -> uiState.value = state.copy(isLoading = false, error = e.message) }
            }.addOnFailureListener { uiState.value = state.copy(isLoading = false, error = "Senha incorreta.") }
        }
    }
}
