package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.theme.GreenSas // Sua cor do tema
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.input.PasswordVisualTransformation // Necessário para o Dialog antigo

@Composable
fun CalendarView(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState

    // Controlo local do mês visualizado
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER: Mês e Navegação ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = displayedMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    displayedMonth = newCal
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior", tint = GreenSas)
                }

                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT")).format(displayedMonth.time).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenSas
                )

                IconButton(onClick = {
                    val newCal = displayedMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    displayedMonth = newCal
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Seguinte", tint = GreenSas)
                }
            }

            // --- GRELHA DO CALENDÁRIO ---
            CalendarGrid(
                displayedMonth = displayedMonth,
                events = state.events,
                selectedDate = state.selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // --- LISTA DE EVENTOS DO DIA SELECIONADO ---
            Text(
                text = "Eventos de ${SimpleDateFormat("dd 'de' MMMM", Locale("pt", "PT")).format(state.selectedDate)}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val selectedDayEvents = state.events.filter { isSameDay(it.date, state.selectedDate) }

            if (selectedDayEvents.isEmpty()) {
                Text("Sem eventos para este dia.", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDayEvents) { event ->
                        EventItemCard(event)

                    }
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
        }

        // --- pop-up da password ---
        if (state.showMandatoryPasswordChange) {
            MandatoryPasswordChangeDialog(
                isLoading = state.isLoading,
                errorMessage = state.error,
                onConfirm = { old, new -> viewModel.changePassword(old, new) {} }
            )
        }
    }
}

@Composable
fun CalendarGrid(
    displayedMonth: Calendar,
    events: List<CalendarEvent>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = displayedMonth.clone() as Calendar
    firstDayOfWeek.set(Calendar.DAY_OF_MONTH, 1)

    val startOffset = firstDayOfWeek.get(Calendar.DAY_OF_WEEK) - 1

    val weekDays = listOf("D", "S", "T", "Q", "Q", "S", "S")

    Column {
        // Cabeçalho Dias da Semana
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { day ->
                Text(text = day, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            // Espaços vazios antes do dia 1
            items(startOffset) {
                Box(modifier = Modifier.size(40.dp))
            }

            // Dias do mês
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val cal = displayedMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, day)
                val date = cal.time
                val isSelected = isSameDay(date, selectedDate)
                val dayEvents = events.filter { isSameDay(it.date, date) }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(45.dp)
                        .padding(2.dp)
                        .background(
                            color = if (isSelected) GreenSas else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onDateSelected(date) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.toString(),
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        // Indicadores (bolinhas)
                        if (dayEvents.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 2.dp)) {
                                dayEvents.take(3).forEach { event ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 1.dp)
                                            .size(6.dp)
                                            .background(getEventColor(event.type), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra lateral colorida
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(getEventColor(event.type), RoundedCornerShape(2.dp))
            )
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

// Funções Auxiliares
fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance(); c1.time = d1
    val c2 = Calendar.getInstance(); c2.time = d2
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun getEventColor(type: EventType): Color {
    return when(type) {
        EventType.CAMPAIGN_START -> Color(0xFF1E88E5) // Azul
        EventType.CAMPAIGN_END -> Color(0xFF5E35B1)   // Roxo
        EventType.PRODUCT_EXPIRY -> Color(0xFFE53935) // Vermelho
        EventType.BASKET_DELIVERY -> Color(0xFF43A047) // Verde
    }
}

// Mantendo o Dialog original para não quebrar a lógica de segurança
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
