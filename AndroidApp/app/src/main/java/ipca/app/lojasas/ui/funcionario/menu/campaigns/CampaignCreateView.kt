package ipca.app.lojasas.ui.funcionario.menu.campaigns

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CampaignCreateView(navController: NavController) {
    val viewModel: CampaignCreateViewModel = viewModel()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Helpers para DatePicker
    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        DatePickerDialog(context, { _, y, m, d ->
            calendar.set(y, m, d)
            onDateSelected(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(
        //topBar = { AppHeader("Nova Campanha", true, { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.save { navController.popBackStack() } },
                containerColor = GreenSas,
                contentColor = Color.White
            ) { Icon(Icons.Default.Check, null) }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(Color.White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Nome
            Text("Nome?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = viewModel.nome.value,
                onValueChange = { viewModel.nome.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nome da campanha") },
                shape = RoundedCornerShape(8.dp)
            )

            HorizontalDivider()

            // Datas
            Text("De quando a quando?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateButton("Início", viewModel.dataInicio.value) { showDatePicker { viewModel.dataInicio.value = it } }
                DateButton("Fim", viewModel.dataFim.value) { showDatePicker { viewModel.dataFim.value = it } }
            }

            HorizontalDivider()

            // Descrição
            Text("Descrição", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = viewModel.desc.value,
                onValueChange = { viewModel.desc.value = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Escreve aqui") },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun RowScope.DateButton(label: String, date: Date?, onClick: () -> Unit) {
    val text = if (date != null) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date) else "Selecionar"
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(text, color = Color.Black)
        }
    }
}