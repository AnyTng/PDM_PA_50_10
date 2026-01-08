package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
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
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
                            .background(WhiteColor)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GreyColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 16.sp, color = BlackColor),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                "Pesquisar numero mecanografico...",
                                                color = GreyColor,
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
                                    val yearColor = if (state.selectedYear != YEAR_FILTER_ALL) GreenSas else GreyColor
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
                                    val estadoColor = if (state.selectedEstado != ESTADO_TODOS) GreenSas else GreyColor
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
                                    val origemColor = if (state.selectedOrigem != ORIGEM_TODOS) GreenSas else GreyColor
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
                                Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = BlackColor)
                            }
                            IconButton(onClick = { viewModel.exportToPDF(context) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = BlackColor)
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
                                Text(it, color = RedColor, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        if (agendadas.isEmpty()) {
                            item {
                                Text("Sem cestas agendadas.", color = GreyColor)
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
                                    color = GreyColor
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
        val dataRef = cesta.dataReferencia()
        val estadoLabel = cesta.estadoLabel()
        val statusLabel = resolveCestaStatusLabel(cesta)
        val isUrgent = cesta.origem?.trim()?.equals("Urgente", ignoreCase = true) == true
        val origemLabel = cesta.origem?.trim().orEmpty().ifBlank { "Sem origem" }
        val faltasLabel = if (cesta.faltas > 0) "Faltas: ${cesta.faltas}" else null
        val dataLabel = dataRef?.let { dateFmt.format(it) } ?: "Sem data definida"
        AlertDialog(
            onDismissRequest = { cestaParaAcoes = null },
            title = {
                Text(
                    text = "Ações disponíveis",
                    fontWeight = FontWeight.Bold,
                    color = GreenSas
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceLight,
                        border = BorderStroke(1.dp, DividerGreenLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Apoiado: ${cesta.apoiadoId}",
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estado: $estadoLabel",
                                    fontSize = 12.sp,
                                    color = GreyColor
                                )
                                CestaStatusPill(
                                    label = statusLabel,
                                    color = resolveCestaAccentColor(cesta, isOverdue)
                                )
                            }
                            Text(
                                text = "Entrega: $dataLabel",
                                fontSize = 12.sp,
                                color = GreyColor
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isUrgent) {
                            CestaTag(label = "Origem: Pedido urgente", color = ReservedOrange)
                        } else if (origemLabel.isNotBlank()) {
                            CestaTag(label = "Origem: $origemLabel", color = GreenSas)
                        }
                        if (isOverdue) {
                            CestaTag(label = "Passou da data agendada", color = ErrorRed)
                        }
                        faltasLabel?.let {
                            CestaTag(label = it, color = WarningAmber)
                        }
                    }

                    CestaActionOption(
                        title = "Cancelar cesta",
                        subtitle = "Anula esta entrega e remove da agenda.",
                        icon = Icons.Default.Close,
                        accent = ErrorRed
                    ) {
                        cestaParaAcoes = null
                        cestaParaCancelar = cesta
                    }
                    CestaActionOption(
                        title = "Marcar como entregue",
                        subtitle = "Confirma que a cesta foi entregue.",
                        icon = Icons.Default.Check,
                        accent = GreenSas
                    ) {
                        cestaParaAcoes = null
                        viewModel.marcarEntregue(cesta)
                    }
                    CestaActionOption(
                        title = "Reagendar entrega",
                        subtitle = "Define nova data e hora de recolha.",
                        icon = Icons.Default.Event,
                        accent = GreenSas
                    ) {
                        cestaParaAcoes = null
                        openDateTimePicker(cesta.dataAgendada ?: cesta.dataRecolha) { novaData ->
                            viewModel.reagendarEntrega(cesta, novaData)
                        }
                    }
                    CestaActionOption(
                        title = "Marcar falta",
                        subtitle = if (cesta.faltas >= 2) {
                            "3ª falta: passa para 'Não levantou'."
                        } else {
                            "Conta como falta e permite reagendar."
                        },
                        icon = Icons.Default.Warning,
                        accent = WarningAmber,
                        enabled = isOverdue
                    ) {
                        cestaParaAcoes = null
                        if (cesta.faltas >= 2) {
                            cestaParaTerceiraFalta = cesta
                        } else {
                            cestaParaFaltaReagendar = cesta
                        }
                    }
                    if (!isOverdue) {
                        Text(
                            text = "Só pode marcar falta após passar a data agendada.",
                            fontSize = 11.sp,
                            color = GreyColor
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { cestaParaAcoes = null }) { Text("Fechar", color = GreenSas) }
            },
            shape = RoundedCornerShape(14.dp),
            containerColor = WhiteColor
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
                    Text("Esta é a 3ª falta. A cesta passará para o estado 'Não levantou'.")
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
private fun CestaActionOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (enabled) SurfaceLight else SurfaceMuted
    val borderColor = if (enabled) accent.copy(alpha = 0.35f) else DividerGreenLight
    val iconColor = if (enabled) accent else GreyColor
    val textColor = if (enabled) TextDark else GreyColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = GreyColor
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CestaStatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CestaTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
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
    val statusLabel = resolveCestaStatusLabel(cesta)
    val data = cesta.dataReferencia()
    val isOverdue = showActions && cesta.isOverdue()
    val isUrgent = cesta.origem?.trim()?.equals("Urgente", ignoreCase = true) == true
    val origemLabel = cesta.origem?.trim().orEmpty().ifBlank { "Sem origem" }
    val accentColor = resolveCestaAccentColor(cesta, isOverdue)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DividerGreenLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Apoiado",
                            fontSize = 11.sp,
                            color = GreyColor
                        )
                        Text(
                            text = cesta.apoiadoId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GreenSas
                        )
                    }
                    CestaStatusPill(label = statusLabel, color = accentColor)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isUrgent) {
                        CestaTag(label = "Origem: Pedido urgente", color = ReservedOrange)
                    } else if (origemLabel.isNotBlank()) {
                        CestaTag(label = "Origem: $origemLabel", color = GreenSas)
                    }
                    if (isOverdue) {
                        CestaTag(label = "Passou da data agendada", color = ErrorRed)
                    }
                    if (cesta.faltas > 0) {
                        CestaTag(label = "Faltas: ${cesta.faltas}", color = WarningAmber)
                    }
                    cesta.tipoApoio?.trim()?.takeIf { it.isNotBlank() }?.let { tipo ->
                        val label = if (normalizeEstadoKey(tipo) == "unica") "Única" else tipo
                        CestaTag(label = "Tipo: $label", color = GreenSas)
                    }
                }

                data?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Entrega: ${dateFmt.format(it)}",
                        fontSize = 12.sp,
                        color = GreyColor
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onVerDetalhes) {
                        Text("Ver detalhes", color = GreyColor)
                    }
                    if (showActions) {
                        Spacer(Modifier.width(8.dp))
                        CestaCardActionButton(
                            text = "Ações",
                            color = GreenSas,
                            onClick = onAcoes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CestaCardActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun resolveCestaAccentColor(cesta: CestaItem, isOverdue: Boolean): Color {
    val estado = normalizeEstadoKey(cesta.estadoLabel())
    val isUrgent = cesta.origem?.trim()?.equals("Urgente", ignoreCase = true) == true
    return when {
        isOverdue -> ErrorRed
        estado == "cancelada" -> ErrorRed
        estado == "nao levantou" -> ErrorRed
        isUrgent -> ReservedOrange
        else -> GreenSas
    }
}

private fun resolveCestaStatusLabel(cesta: CestaItem, now: Date = Date()): String {
    val estadoLabel = cesta.estadoLabel()
    if (!cesta.isAgendada()) return estadoLabel
    val data = cesta.dataReferencia() ?: return estadoLabel
    val nowInstant = now.toInstant()
    val targetInstant = data.toInstant()
    if (!targetInstant.isAfter(nowInstant)) return estadoLabel

    val zoneId = ZoneId.systemDefault()
    val today = nowInstant.atZone(zoneId).toLocalDate()
    val targetDate = targetInstant.atZone(zoneId).toLocalDate()

    return when {
        targetDate.isEqual(today) -> formatSameDayCountdown(nowInstant, targetInstant)
        targetDate.isEqual(today.plusDays(1)) -> "Amanhã"
        targetDate.isAfter(today) -> {
            val days = ChronoUnit.DAYS.between(today, targetDate)
            "Daqui a ${formatCount(days, "dia")}"
        }
        else -> estadoLabel
    }
}

private fun formatSameDayCountdown(nowInstant: java.time.Instant, targetInstant: java.time.Instant): String {
    val duration = Duration.between(nowInstant, targetInstant)
    val totalMinutes = duration.toMinutes()
    if (totalMinutes < 60) {
        val minutes = totalMinutes.coerceAtLeast(1)
        return "Daqui a ${formatCount(minutes, "minuto")}"
    }
    val hours = duration.toHours().coerceAtLeast(1)
    return "Daqui a ${formatCount(hours, "hora")}"
}

private fun formatCount(value: Long, unit: String): String {
    val plural = if (value == 1L) unit else "${unit}s"
    return "$value $plural"
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
        n == "nao_levantou" || n == "nao levantou" -> "Não levantou"
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
