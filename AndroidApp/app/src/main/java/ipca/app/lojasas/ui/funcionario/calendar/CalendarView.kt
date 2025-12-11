package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.components.AppHeader

@Composable
fun CalendarView(navController: NavController) {
    Scaffold(
        topBar = {
            AppHeader(title = "Calendário")
        },
        bottomBar = {
            Footer(
                type = FooterType.FUNCIONARIO,
                onCalendarClick = {
                    // Já estamos no calendário, talvez recarregar ou nada
                }
            )
        }
    ) { innerPadding ->
        // O conteúdo principal da página fica aqui
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Conteúdo do Calendário Aqui")
            // Aqui podes implementar a lógica do calendário
        }
    }
}
