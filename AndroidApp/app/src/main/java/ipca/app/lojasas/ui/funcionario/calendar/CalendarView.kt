package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch

@Composable
fun CalendarView(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState
    val scope = rememberCoroutineScope()

    val baseMonth = remember { Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) } }
    val pageCount = 2400
    val initialPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    // Calcula mês atual
    val currentMonth = remember(pagerState.currentPage) {
        monthForPage(baseMonth, pagerState.currentPage, initialPage)
    }

    // Carrega eventos e sincroniza data selecionada quando a página muda
    LaunchedEffect(pagerState.currentPage) {
        viewModel.ensureSelectedDateInMonth(currentMonth)
        viewModel.loadEventsForMonth(currentMonth)
    }

    // Filtra eventos para a lista do dia selecionado
    val selectedDayEvents = remember(state.events, state.selectedDate) {
        state.events.filter { isSameDay(it.date, state.selectedDate) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior", tint = GreenSas)
                }
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT")).format(currentMonth.time).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenSas
                )
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Seguinte", tint = GreenSas)
                }
            }

            // --- GRELHA ---
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                // Otimização: Renderiza apenas páginas próximas
                if (page in (pagerState.currentPage - 1)..(pagerState.currentPage + 1)) {
                    val pageMonth = monthForPage(baseMonth, page, initialPage)

                    // Pré-filtro para a grelha
                    val monthEvents = remember(state.events, page) {
                        val (start, end) = getMonthRange(pageMonth)
                        state.events.filter { it.date >= start && it.date < end }
                    }

                    CalendarGrid(
                        displayedMonth = pageMonth,
                        events = monthEvents,
                        selectedDate = state.selectedDate,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // --- LISTA ---
            Text(
                text = "Eventos de ${SimpleDateFormat("dd 'de' MMMM", Locale("pt", "PT")).format(state.selectedDate)}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (selectedDayEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Sem eventos para este dia.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDayEvents, key = { it.id }) { event ->
                        EventItemCard(event)
                    }
                }
            }
        }

        if (state.isLoading && state.events.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
        }

        if (state.showMandatoryPasswordChange) {
            MandatoryPasswordChangeDialog(
                isLoading = state.isLoading,
                errorMessage = state.error,
                onConfirm = { old, new -> viewModel.changePassword(old, new) {} }
            )
        }
    }
}

// --- COMPONENTES E FUNÇÕES AUXILIARES ---

fun getMonthRange(cal: Calendar): Pair<Date, Date> {
    val start = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0) }
    val end = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1) }
    return start.time to end.time
}

@Composable
fun CalendarGrid(
    displayedMonth: Calendar,
    events: List<CalendarEvent>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (displayedMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                Text(it, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                repeat(7) { col ->
                    val day = (row * 7) + col - firstDayOfWeek + 1
                    if (day in 1..daysInMonth) {
                        val cal = displayedMonth.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        val date = cal.time
                        val isSelected = isSameDay(date, selectedDate)

                        // Filtra eventos deste dia específico
                        val dayEvents = events.filter { isSameDay(it.date, date) }

                        DayCell(day, isSelected, dayEvents) { onDateSelected(date) }
                    } else {
                        Box(modifier = Modifier.size(45.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: Int, isSelected: Boolean, events: List<CalendarEvent>, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(45.dp)
            .padding(2.dp)
            .background(if (isSelected) GreenSas else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = if (isSelected) Color.White else Color.Black,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (events.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.Center) {
                    events.take(3).forEach {
                        Box(modifier = Modifier.padding(horizontal = 1.dp).size(6.dp).background(getEventColor(it.type), CircleShape))
                    }
                }
            }
        }
    }
}

fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance(); c1.time = d1
    val c2 = Calendar.getInstance(); c2.time = d2
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun getEventColor(type: EventType): Color {
    return when(type) {
        EventType.CAMPAIGN_START -> Color(0xFF1E88E5)
        EventType.CAMPAIGN_END -> Color(0xFF5E35B1)
        EventType.PRODUCT_EXPIRY -> Color(0xFFE53935)
        EventType.BASKET_DELIVERY -> Color(0xFF43A047)
    }
}

private fun monthForPage(baseMonth: Calendar, page: Int, initialPage: Int): Calendar {
    return (baseMonth.clone() as Calendar).apply {
        add(Calendar.MONTH, page - initialPage)
    }
}

@Composable
fun MandatoryPasswordChangeDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: (String, String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Atualização de Segurança") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("É obrigatório alterar a sua palavra-passe.")
                if (errorMessage != null) Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                OutlinedTextField(value = oldPass, onValueChange = { oldPass = it }, label = { Text("Senha Atual") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("Nova Senha") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text("Repetir Senha") }, visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = {
            Button(onClick = { if (newPass == confirmPass && newPass.isNotEmpty()) onConfirm(oldPass, newPass) }) {
                Text("Alterar")
            }
        }
    )
}

@Composable
fun EventItemCard(event: CalendarEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).background(getEventColor(event.type), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (event.description.isNotEmpty()) {
                    Text(event.description, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}