package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun CalendarView(navController: NavController) {
    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // O conteúdo principal da página fica aqui
        Text(text = "Conteúdo do Calendário Aqui")
        // Aqui podes implementar a lógica do calendário
    }
}
