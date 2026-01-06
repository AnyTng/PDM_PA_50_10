package ipca.app.lojasas.ui.funcionario.calendar

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.data.AuditLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- MODELOS DE DADOS ---

enum class EventType {
    CAMPAIGN_START,
    CAMPAIGN_END,
    PRODUCT_EXPIRY,
    BASKET_DELIVERY
}

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: Date,
    val type: EventType,
    val description: String = ""
)

data class CalendarState(
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val docId: String = "",
    val events: List<CalendarEvent> = emptyList(),
    val selectedDate: Date = Date()
)

// --- VIEW MODEL ---

class CalendarViewModel : ViewModel() {

    var uiState = mutableStateOf(CalendarState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Controla quais os meses que já estamos a "ouvir" para não duplicar listeners
    private val listeningMonths = mutableSetOf<String>()

    // Guarda os registos para poder cancelar se necessário (ex: ao fechar a view)
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // Mapa interno para gerir eventos por ID (evita duplicados e facilita remoções)
    private val eventsMap = mutableMapOf<String, CalendarEvent>()

    init {
        checkMudarPassStatus()
    }

    fun loadEventsForMonth(reference: Calendar) {
        val (start, end) = monthRange(reference)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(start)

        // Se já estamos a ouvir alterações deste mês, não fazemos nada (já é automático)
        if (listeningMonths.contains(monthKey)) {
            return
        }

        // Marca este mês como "ativo"
        listeningMonths.add(monthKey)
        uiState.value = uiState.value.copy(isLoading = true)

        // Função auxiliar para processar mudanças em tempo real
        fun handleSnapshot(changes: List<DocumentChange>, parser: (DocumentChange) -> List<CalendarEvent>) {
            changes.forEach { change ->
                val newEvents = parser(change)
                when (change.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        newEvents.forEach { event -> eventsMap[event.id] = event }
                    }
                    DocumentChange.Type.REMOVED -> {
                        // Para remoção, recriamos os IDs esperados e removemos do mapa
                        newEvents.forEach { event -> eventsMap.remove(event.id) }
                    }
                }
            }
            // Atualiza a UI com a lista atualizada do mapa
            uiState.value = uiState.value.copy(
                isLoading = false,
                events = eventsMap.values.toList().sortedBy { it.date }
            )
        }

        // 1. Ouvinte: Campanhas (Início)
        val reg1 = db.collection("campanha")
            .whereGreaterThanOrEqualTo("dataInicio", start)
            .whereLessThan("dataInicio", end)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                snap?.documentChanges?.let { changes ->
                    handleSnapshot(changes) { change ->
                        val doc = change.document
                        val nome = doc.getString("nomeCampanha") ?: "Campanha"
                        val inicio = doc.getTimestamp("dataInicio")?.toDate() ?: (doc.get("dataInicio") as? Date)
                        if (inicio != null) {
                            listOf(CalendarEvent(doc.id + "_start", "Início: $nome", inicio, EventType.CAMPAIGN_START))
                        } else emptyList()
                    }
                }
            }

        // 2. Ouvinte: Campanhas (Fim)
        val reg2 = db.collection("campanha")
            .whereGreaterThanOrEqualTo("dataFim", start)
            .whereLessThan("dataFim", end)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                snap?.documentChanges?.let { changes ->
                    handleSnapshot(changes) { change ->
                        val doc = change.document
                        val nome = doc.getString("nomeCampanha") ?: "Campanha"
                        val fim = doc.getTimestamp("dataFim")?.toDate() ?: (doc.get("dataFim") as? Date)
                        if (fim != null) {
                            listOf(CalendarEvent(doc.id + "_end", "Fim: $nome", fim, EventType.CAMPAIGN_END))
                        } else emptyList()
                    }
                }
            }

        // 3. Ouvinte: Validade Produtos
        val reg3 = db.collection("produtos")
            .whereGreaterThanOrEqualTo("validade", start)
            .whereLessThan("validade", end)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                snap?.documentChanges?.let { changes ->
                    handleSnapshot(changes) { change ->
                        val doc = change.document
                        val validade = doc.getTimestamp("validade")?.toDate() ?: (doc.get("validade") as? Date)
                        val nome = doc.getString("nomeProduto") ?: "Produto"
                        if (validade != null) {
                            listOf(CalendarEvent(doc.id, "Validade: $nome", validade, EventType.PRODUCT_EXPIRY))
                        } else emptyList()
                    }
                }
            }

        // 4. Ouvinte: Cestas
        val reg4 = db.collection("cestas")
            .whereGreaterThanOrEqualTo("dataRecolha", start)
            .whereLessThan("dataRecolha", end)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                snap?.documentChanges?.let { changes ->
                    handleSnapshot(changes) { change ->
                        val doc = change.document
                        val data = doc.getTimestamp("dataRecolha")?.toDate() ?: (doc.get("dataRecolha") as? Date)
                        val estado = doc.getString("estadoCesta") ?: ""
                        val apoiadoId = doc.getString("apoiadoID") ?: ""
                        if (data != null) {
                            listOf(CalendarEvent(doc.id, "Entrega Cesta ($estado)", data, EventType.BASKET_DELIVERY, "Apoiado: $apoiadoId"))
                        } else emptyList()
                    }
                }
            }

        listenerRegistrations.addAll(listOf(reg1, reg2, reg3, reg4))
    }

    // Limpa os ouvintes quando saímos do ecrã para não gastar bateria/dados
    override fun onCleared() {
        listenerRegistrations.forEach { it.remove() }
        super.onCleared()
    }

    fun ensureSelectedDateInMonth(reference: Calendar) {
        val currentCal = Calendar.getInstance().apply { time = uiState.value.selectedDate }
        if (currentCal.get(Calendar.MONTH) == reference.get(Calendar.MONTH) &&
            currentCal.get(Calendar.YEAR) == reference.get(Calendar.YEAR)) return

        val target = (reference.clone() as Calendar).apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val dayToSet = currentCal.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay)
            set(Calendar.DAY_OF_MONTH, dayToSet)
        }
        uiState.value = uiState.value.copy(selectedDate = target.time)
    }

    private fun monthRange(reference: Calendar): Pair<Date, Date> {
        val start = (reference.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return start.time to end.time
    }

    fun selectDate(date: Date) {
        uiState.value = uiState.value.copy(selectedDate = date)
    }

    private fun checkMudarPassStatus() {
        val user = auth.currentUser ?: return
        db.collection("funcionarios").whereEqualTo("uid", user.uid).limit(1).get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    val doc = it.documents[0]
                    uiState.value = uiState.value.copy(
                        showMandatoryPasswordChange = doc.getBoolean("mudarPass") ?: false,
                        docId = doc.id
                    )
                }
            }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val state = uiState.value

        uiState.value = state.copy(isLoading = true, error = null)

        val credential = EmailAuthProvider.getCredential(email, oldPass)
        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPass).addOnSuccessListener {
                db.collection("funcionarios").document(state.docId).update("mudarPass", false)
                    .addOnSuccessListener {
                        AuditLogger.logAction("Alterou palavra-passe", "funcionario", state.docId)
                        uiState.value = state.copy(isLoading = false, showMandatoryPasswordChange = false)
                        onSuccess()
                    }
            }.addOnFailureListener { uiState.value = state.copy(isLoading = false, error = it.message) }
        }.addOnFailureListener { uiState.value = state.copy(isLoading = false, error = "Senha incorreta.") }
    }
}
