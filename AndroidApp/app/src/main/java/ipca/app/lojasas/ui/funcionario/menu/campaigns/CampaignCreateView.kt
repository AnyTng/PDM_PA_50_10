package ipca.app.lojasas.ui.funcionario.menu.campaigns

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CampaignCreateView(navController: NavController) {
    val vm: CampaignCreateViewModel = hiltViewModel()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    fun showDatePicker(onDateSelected: (Date) -> Unit) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    CampaignCreateViewContent(
        nome = vm.nome.value,
        onNomeChange = { vm.nome.value = it },
        dataInicio = vm.dataInicio.value,
        onDataInicioClick = { showDatePicker { vm.dataInicio.value = it } },
        dataFim = vm.dataFim.value,
        onDataFimClick = { showDatePicker { vm.dataFim.value = it } },
        desc = vm.desc.value,
        onDescChange = { vm.desc.value = it },
        onSaveClick = { vm.save { navController.popBackStack() } }
    )
}

/**
 * UI "pura" -> dá para Preview sem NavController/ViewModel/DatePicker real.
 */
@Composable
private fun CampaignCreateViewContent(
    nome: String,
    onNomeChange: (String) -> Unit,
    dataInicio: Date?,
    onDataInicioClick: () -> Unit,
    dataFim: Date?,
    onDataFimClick: () -> Unit,
    desc: String,
    onDescChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
                containerColor = GreenSas,
                contentColor = WhiteColor,
                modifier = Modifier.size(64.dp)
            ) { Icon(Icons.Default.Check, null) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(WhiteColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Nome
            Text("Nome?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = nome,
                onValueChange = onNomeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nome da campanha") },
                shape = RoundedCornerShape(8.dp)
            )

            HorizontalDivider()

            // Datas
            Text("De quando a quando?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateButton("Início", dataInicio, onClick = onDataInicioClick)
                DateButton("Fim", dataFim, onClick = onDataFimClick)
            }

            HorizontalDivider()

            // Descrição
            Text("Descrição", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = desc,
                onValueChange = onDescChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Escreve aqui") },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun RowScope.DateButton(label: String, date: Date?, onClick: () -> Unit) {
    val text = if (date != null)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    else
        "Selecionar"

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, fontSize = 12.sp, color = GreyColor)
            Text(text, color = BlackColor)
        }
    }
}

// ---------------- PREVIEWS ----------------

@Preview(showBackground = true, name = "CampaignCreate - Vazio")
@Composable
private fun CampaignCreateViewPreview_Empty() {
    CampaignCreateViewContent(
        nome = "",
        onNomeChange = {},
        dataInicio = null,
        onDataInicioClick = {},
        dataFim = null,
        onDataFimClick = {},
        desc = "",
        onDescChange = {},
        onSaveClick = {}
    )
}

@Preview(showBackground = true, name = "CampaignCreate - Preenchido")
@Composable
private fun CampaignCreateViewPreview_Filled() {
    val cal = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 10) }
    val inicio = cal.time
    cal.set(2026, Calendar.FEBRUARY, 20)
    val fim = cal.time

    CampaignCreateViewContent(
        nome = "Campanha de Inverno",
        onNomeChange = {},
        dataInicio = inicio,
        onDataInicioClick = {},
        dataFim = fim,
        onDataFimClick = {},
        desc = "Angariação de bens alimentares e higiene para famílias apoiadas.",
        onDescChange = {},
        onSaveClick = {}
    )
}
