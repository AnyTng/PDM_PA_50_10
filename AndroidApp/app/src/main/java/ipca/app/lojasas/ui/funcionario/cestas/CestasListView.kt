package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.cestas.CestaItem
import ipca.app.lojasas.ui.funcionario.stock.components.StockFab
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Calendar
import java.util.Date
import java.util.Locale



@Composable
fun CestasListView(
    navController: NavController,
    viewModel: CestasListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val dateFmt = rememberDateFormatter()
    val filtersScrollState = rememberScrollState()

    var showEstadoMenu by remember { mutableStateOf(false) }
    var showOrigemMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    // Diálogos
    var cestaParaReagendarEntrega by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaFaltaReagendar by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaTerceiraFalta by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaCancelar by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaAcoes by remember { mutableStateOf<CestaItem?>(null) }

    fun openDateTimePicker(initial: Date?, onSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.time = initial

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        onSelected(cal.time)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))

            else -> {
                val (agendadas, historico) = remember(state.filteredCestas) {
                    val ag = state.filteredCestas.filter { it.isAgendada() }
                        .sortedBy { it.dataAgendada ?: it.dataRecolha ?: Date(Long.MAX_VALUE) }
                    val hist = state.filteredCestas.filterNot { it.isAgendada() }
                        .sortedByDescending { it.dataAgendada ?: it.dataRecolha ?: Date(0) }
                    ag to hist
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                "Pesquisar numero mecanografico...",
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(filtersScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    val yearColor = if (state.selectedYear != YEAR_FILTER_ALL) GreenSas else Color.Gray
                                    TextButton(onClick = { showYearMenu = true }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Ano: ${state.selectedYear}",
                                                color = yearColor,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = yearColor)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showYearMenu,
                                        onDismissRequest = { showYearMenu = false }
                                    ) {
                                        state.availableYears.forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text(year) },
                                                onClick = {
                                                    viewModel.onYearSelected(year)
                                                    showYearMenu = false
                                                },
                                                trailingIcon = {
                                                    if (state.selectedYear == year) {
                                                        Icon(Icons.Default.Check, null, tint = GreenSas)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                Box {
                                    val estadoColor = if (state.selectedEstado != ESTADO_TODOS) GreenSas else Color.Gray
                                    TextButton(onClick = { showEstadoMenu = true }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Estado: ${state.selectedEstado}",
                                                color = estadoColor,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = estadoColor)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showEstadoMenu,
                                        onDismissRequest = { showEstadoMenu = false }
                                    ) {
                                        state.availableEstados.forEach { estado ->
                                            DropdownMenuItem(
                                                text = { Text(estado) },
                                                onClick = {
                                                    viewModel.onEstadoSelected(estado)
                                                    showEstadoMenu = false
                                                },
                                                trailingIcon = {
                                                    if (state.selectedEstado == estado) {
                                                        Icon(Icons.Default.Check, null, tint = GreenSas)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Box {
                                    val origemColor = if (state.selectedOrigem != ORIGEM_TODOS) GreenSas else Color.Gray
                                    TextButton(onClick = { showOrigemMenu = true }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Origem: ${state.selectedOrigem}",
                                                color = origemColor,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = origemColor)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showOrigemMenu,
                                        onDismissRequest = { showOrigemMenu = false }
                                    ) {
                                        state.availableOrigens.forEach { origem ->
                                            DropdownMenuItem(
                                                text = { Text(origem) },
                                                onClick = {
                                                    viewModel.onOrigemSelected(origem)
                                                    showOrigemMenu = false
                                                },
                                                trailingIcon = {
                                                    if (state.selectedOrigem == origem) {
                                                        Icon(Icons.Default.Check, null, tint = GreenSas)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                            }

                            IconButton(onClick = { viewModel.exportToCSV(context) }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = Color.Black)
                            }
                            IconButton(onClick = { viewModel.exportToPDF(context) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = Color.Black)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.error?.let {
                            item {
                                Text(it, color = Color.Red, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        if (agendadas.isEmpty()) {
                            item {
                                Text("Sem cestas agendadas.", color = Color.Gray)
                            }
                        } else {
                            items(agendadas, key = { it.id }) { cesta ->
                                CestaCard(
                                    cesta = cesta,
                                    dateFmt = dateFmt,
                                    showActions = true,
                                    onAcoes = { cestaParaAcoes = cesta },
                                    onVerDetalhes = {
                                        navController.navigate(Screen.CestaDetails.createRoute(cesta.id))
                                    }
                                )
                            }
                        }

                        if (historico.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Entregue / Não levantou",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                            items(historico, key = { it.id }) { cesta ->
                                CestaCard(
                                    cesta = cesta,
                                    dateFmt = dateFmt,
                                    showActions = false,
                                    onAcoes = {},
                                    onVerDetalhes = {
                                        navController.navigate(Screen.CestaDetails.createRoute(cesta.id))
                                    }
                                )
                            }
                        }
                    }
                }

                StockFab(
                    onClick = { navController.navigate(Screen.CreateCesta.route) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = 22.dp)
                )
            }
        }
    }

    // Ações Disponiveis
    if (cestaParaAcoes != null) {
        val cesta = cestaParaAcoes!!
        val isOverdue = cesta.isOverdue()
        AlertDialog(
            onDismissRequest = { cestaParaAcoes = null },
            title = { Text("Ações Disponiveis") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            cestaParaAcoes = null
                            cestaParaCancelar = cesta
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            cestaParaAcoes = null
                            viewModel.marcarEntregue(cesta)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Entregar")
                    }
                    Button(
                        onClick = {
                            cestaParaAcoes = null
                            cestaParaReagendarEntrega = cesta
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Reagendar")
                    }
                    Button(
                        onClick = {
                            cestaParaAcoes = null
                            // Na 3ª falta não permite escolher dia
                            if (cesta.faltas >= 2) {
                                cestaParaTerceiraFalta = cesta
                            } else {
                                cestaParaFaltaReagendar = cesta
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isOverdue,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonGrey,
                            disabledContainerColor = DisabledGrey
                        )
                    ) {
                        Text("Faltou")
                    }
                    if (!isOverdue) {
                        Text(
                            text = "So pode marcar falta depois de passar a data agendada.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { cestaParaAcoes = null }) { Text("Fechar") }
            }
        )
    }

    // Cancelar
    if (cestaParaCancelar != null) {
        val cesta = cestaParaCancelar!!
        AlertDialog(
            onDismissRequest = { cestaParaCancelar = null },
            title = { Text("Cancelar cesta") },
            text = { Text("Pretende cancelar a cesta para o apoiado ${cesta.apoiadoId}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelarCesta(cesta.id)
                        cestaParaCancelar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaCancelar = null }) { Text("Fechar") }
            }
        )
    }

    // Reagendar entrega (SEM contar falta)
    if (cestaParaReagendarEntrega != null) {
        val cesta = cestaParaReagendarEntrega!!
        AlertDialog(
            onDismissRequest = { cestaParaReagendarEntrega = null },
            title = { Text("Reagendar entrega") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(6.dp))
                    Text("Escolha uma nova data/hora.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openDateTimePicker(cesta.dataAgendada ?: cesta.dataRecolha) { novaData ->
                            viewModel.reagendarEntrega(cesta, novaData)
                        }
                        cestaParaReagendarEntrega = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                ) {
                    Text("Selecionar data")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaReagendarEntrega = null }) { Text("Fechar") }
            }
        )
    }

    // Faltou (conta como falta e permite reagendar apenas até à 2ª falta)
    if (cestaParaFaltaReagendar != null) {
        val cesta = cestaParaFaltaReagendar!!
        AlertDialog(
            onDismissRequest = { cestaParaFaltaReagendar = null },
            title = { Text("Faltou - reagendar") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(6.dp))
                    Text("Faltas atuais: ${cesta.faltas}")
                    Spacer(Modifier.height(10.dp))
                    Text("Escolha uma nova data/hora (isto irá contar como falta).")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openDateTimePicker(cesta.dataAgendada ?: cesta.dataRecolha) { novaData ->
                            viewModel.reagendarComFalta(cesta, novaData)
                        }
                        cestaParaFaltaReagendar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGrey)
                ) {
                    Text("Selecionar data")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaFaltaReagendar = null }) { Text("Fechar") }
            }
        )
    }

    // 3ª falta (sem escolher dia)
    if (cestaParaTerceiraFalta != null) {
        val cesta = cestaParaTerceiraFalta!!
        AlertDialog(
            onDismissRequest = { cestaParaTerceiraFalta = null },
            title = { Text("3ª falta") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(8.dp))
                    Text("Esta é a 3ª falta. A cesta passará para o estado 'Nao_Levantou'.")
                    Spacer(Modifier.height(8.dp))
                    Text("Não é possível selecionar novo dia.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.registarTerceiraFaltaSemReagendar(cesta)
                        cestaParaTerceiraFalta = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Marcar como Nao levantou")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaTerceiraFalta = null }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun CestaCard(
    cesta: CestaItem,
    dateFmt: SimpleDateFormat,
    showActions: Boolean,
    onAcoes: () -> Unit,
    onVerDetalhes: () -> Unit
) {
    val estadoLabel = cesta.estadoLabel()
    val data = cesta.dataReferencia()
    val isOverdue = showActions && cesta.isOverdue()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apoiado: ${cesta.apoiadoId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSas
                )
                Text(
                    text = estadoLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (estadoLabel == "Nao levantou") ErrorRed else Color.Gray
                )
            }

            if (cesta.origem?.equals("Urgente", ignoreCase = true) == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Origem: Pedido Urgente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReservedOrange
                )
            }

            data?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Entrega: ${dateFmt.format(it)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (isOverdue) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Passou da data agendada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorRed
                )
            }

            if (cesta.faltas > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Faltas: ${cesta.faltas}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (showActions) {
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAcoes,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Ações Disponiveis")
                    }
                    Button(
                        onClick = onVerDetalhes,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGrey)
                    ) {
                        Text("Ver Detalhes")
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onVerDetalhes,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGrey)
                ) {
                    Text("Ver Detalhes")
                }
            }
        }
    }
}

private fun CestaItem.isAgendada(): Boolean {
    val n = estado.trim().lowercase(Locale.getDefault())
    return n == "agendada" || n == "por preparar" || n == "por_preparar" || n == "em preparar" || n == "em_preparar"
}

private fun CestaItem.dataReferencia(): Date? {
    return dataAgendada ?: dataRecolha
}

private fun CestaItem.isOverdue(reference: Date = Date()): Boolean {
    val data = dataReferencia()
    return data != null && reference.after(data)
}

private fun CestaItem.estadoLabel(): String {
    val n = normalizeEstadoKey(estado)
    return when {
        n == "entregue" -> "Entregue"
        n == "agendada" -> "Agendada"
        n == "por preparar" || n == "por_preparar" -> "Por preparar"
        n == "em preparar" || n == "em_preparar" -> "Em preparar"
        n == "nao_levantou" || n == "nao levantou" -> "Nao levantou"
        n == "cancelada" -> "Cancelada"
        n.isBlank() -> "-"
        else -> estado
    }
}

private val estadoDiacriticsRegex = "\\p{Mn}+".toRegex()

private fun normalizeEstadoKey(value: String): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
    return estadoDiacriticsRegex.replace(normalized, "").trim().lowercase(Locale.getDefault())
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    MaterialTheme
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
