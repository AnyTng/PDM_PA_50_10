package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.calendar.CalendarEvent
import ipca.app.lojasas.data.calendar.CalendarRepository
import ipca.app.lojasas.data.common.ListenerHandle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CalendarState(
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val docId: String = "",
    val events: List<CalendarEvent> = emptyList(),
    val selectedDate: Date = Date()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    var uiState = mutableStateOf(CalendarState())
        private set

    // Controla quais os meses que já estamos a "ouvir" para não duplicar listeners
    private val listeningMonths = mutableSetOf<String>()

    // Guarda os registos para poder cancelar se necessário (ex: ao fechar a view)
    private val listenerHandles = mutableListOf<ListenerHandle>()

    private val monthEvents = mutableMapOf<String, List<CalendarEvent>>()

    init {
        checkMudarPassStatus()
    }

    fun loadEventsForMonth(reference: Calendar) {
        val (start, end) = monthRange(reference)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(start)

        if (listeningMonths.contains(monthKey)) {
            return
        }

        listeningMonths.add(monthKey)
        uiState.value = uiState.value.copy(isLoading = true)

        val handle = repository.listenMonthEvents(
            start = start,
            end = end,
            onSuccess = { events ->
                monthEvents[monthKey] = events
                val merged = monthEvents.values.flatten().sortedBy { it.date }
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = null,
                    events = merged
                )
            },
            onError = { e ->
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        )
        listenerHandles.add(handle)
    }

    // Limpa os ouvintes quando saímos do ecrã para não gastar bateria/dados
    override fun onCleared() {
        listenerHandles.forEach { it.remove() }
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
        repository.fetchEmployeePasswordStatus(
            onSuccess = { status ->
                if (status == null) return@fetchEmployeePasswordStatus
                uiState.value = uiState.value.copy(
                    showMandatoryPasswordChange = status.mustChangePassword,
                    docId = status.docId
                )
            },
            onError = { }
        )
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val state = uiState.value

        uiState.value = state.copy(isLoading = true, error = null)

        repository.changeEmployeePassword(
            docId = state.docId,
            oldPassword = oldPass,
            newPassword = newPass,
            onSuccess = {
                uiState.value = state.copy(
                    isLoading = false,
                    showMandatoryPasswordChange = false
                )
                onSuccess()
            },
            onError = { message ->
                uiState.value = state.copy(isLoading = false, error = message)
            }
        )
    }
}
