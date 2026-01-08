package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.cestas.ApoiadoInfo
import ipca.app.lojasas.data.cestas.ApoiadoOption
import ipca.app.lojasas.data.products.Product
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun CreateCestaView(
    navController: NavController,
    fromUrgent: Boolean,
    pedidoId: String?,
    apoiadoId: String?,
    viewModel: CreateCestaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val dateFmt = rememberDateFormatter()
    val infoDateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val ipcaFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenSas,
        unfocusedBorderColor = GreenSas,
        focusedLabelColor = GreenSas,
        unfocusedLabelColor = GreenSas,
        focusedPlaceholderColor = GreenSas,
        unfocusedPlaceholderColor = GreenSas,
        cursorColor = GreenSas
    )

    // Inicialização (carregar produtos, apoiados, etc.)
    LaunchedEffect(fromUrgent, pedidoId, apoiadoId) {
        viewModel.start(fromUrgent = fromUrgent, pedidoId = pedidoId, apoiadoId = apoiadoId)
    }

    // Diálogos
    var showApoiadoPicker by remember { mutableStateOf(false) }
    var showProdutosPicker by remember { mutableStateOf(false) }

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
        if (state.isLoading) {
            CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                state.error?.let {
                    Text(it, color = RedColor, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                }

                SectionTitle("Para quem?")
                BeneficiarioSelector(
                    value = state.apoiadoSelecionado?.let { "${it.nome} (${it.id})" } ?: "Selecionar beneficiário",
                    enabled = !state.fromUrgent,
                    onClick = { if (!state.fromUrgent) showApoiadoPicker = true }
                )

                if (state.apoiadoSelecionado != null) {
                    Spacer(Modifier.height(10.dp))
                    ApoiadoInfoCard(
                        info = state.apoiadoInfo,
                        isLoading = state.isLoadingApoiadoInfo,
                        error = state.apoiadoInfoError,
                        dateFormatter = infoDateFmt
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                SectionTitle("O quê?")
                ProdutosSelecionadosList(
                    produtos = state.produtosSelecionados,
                    onRemove = { viewModel.removeProduto(it) },
                    onVer = { productId ->
                        navController.navigate(Screen.StockProduct.createRoute(productId))
                    },
                    onAdd = { showProdutosPicker = true }
                )

                Spacer(Modifier.height(16.dp))

                SectionTitle("Quando?")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.usarAgora,
                                onClick = { viewModel.setUsarAgora(true) },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                            )
                            Text("Agora")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !state.usarAgora,
                                onClick = {
                                    viewModel.setUsarAgora(false)
                                    openDateTimePicker(state.dataAgendada) { viewModel.setDataAgendada(it) }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                            )
                            Text(
                                text = "Selecionar",
                                modifier = Modifier.clickable {
                                    viewModel.setUsarAgora(false)
                                    openDateTimePicker(state.dataAgendada) { viewModel.setDataAgendada(it) }
                                }
                            )
                        }

                        if (!state.usarAgora && state.dataAgendada != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Agendado para: ${dateFmt.format(state.dataAgendada)}",
                                fontSize = 12.sp,
                                color = GreyColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("É recorrente?")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        // Vindo de pedido urgente: sempre única
                        val recorrenteEnabled = !state.fromUrgent

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !state.recorrente,
                                onClick = { viewModel.setRecorrente(false) },
                                enabled = recorrenteEnabled,
                                colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                            )
                            Text("Única", color = if (recorrenteEnabled) UnspecifiedColor else GreyColor)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            RadioButton(
                                selected = state.recorrente,
                                onClick = { viewModel.setRecorrente(true) },
                                enabled = recorrenteEnabled,
                                colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                            )
                            Text("Sim", color = if (recorrenteEnabled) UnspecifiedColor else GreyColor)
                        }

                       // if (state.recorrente) {
                       //     Spacer(Modifier.height(6.dp))
                       //     Text("Intervalo fixo: 30 dias", fontSize = 12.sp, color = GreyColor)
                       // }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SectionTitle("Alguma coisa a notar?")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor)
                ) {
                    OutlinedTextField(
                        value = state.obs,
                        onValueChange = { viewModel.setObs(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        placeholder = { Text("", color = GreenSas) },
                        colors = ipcaFieldColors
                    )
                }

                Spacer(Modifier.height(90.dp))
            }

            FloatingActionButton(
                onClick = {
                    if (state.isSubmitting) return@FloatingActionButton
                    viewModel.submitCesta {
                        navController.popBackStack()
                    }
                },
                containerColor = GreenSas,
                contentColor = WhiteColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(64.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = WhiteColor, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Confirmar")
                }
            }
        }
    }

    // Picker de beneficiário
    if (showApoiadoPicker) {
        ApoiadoPickerDialog(
            title = "Selecionar beneficiário",
            options = state.apoiados,
            onDismiss = { showApoiadoPicker = false },
            onSelect = {
                viewModel.selecionarApoiado(it)
                showApoiadoPicker = false
            }
        )
    }

    // Picker de produtos
    if (showProdutosPicker) {
        ProdutosPickerDialog(
            produtos = state.produtos,
            selecionadosIds = state.produtosSelecionados.map { it.id }.toSet(),
            onDismiss = { showProdutosPicker = false },
            onAdd = { viewModel.addProduto(it) }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = BlackColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BeneficiarioSelector(value: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .border(1.dp, GreenSas, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = if (enabled) GreenSas else GreyColor, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.Add, contentDescription = "Selecionar", tint = GreenSas)
        }
    }
}

@Composable
private fun ApoiadoInfoCard(
    info: ApoiadoInfo?,
    isLoading: Boolean,
    error: String?,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GreenSas.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dados do apoiado",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GreenSas
            )
            HorizontalDivider(color = DividerGreenLight)
            when {
                isLoading -> {
                    Text("A carregar dados do apoiado...", fontSize = 12.sp, color = GreyColor)
                }
                error != null -> {
                    Text(error, fontSize = 12.sp, color = RedColor)
                }
                info != null -> {
                    val necessidades = info.necessidades
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                        .ifBlank { "Sem necessidades registadas" }
                    val ultimo = info.ultimoLevantamento?.let { dateFormatter.format(it) } ?: "—"
                    val validade = info.validadeConta?.let { dateFormatter.format(it) } ?: "—"

                    InfoRow("Nome", info.nome)
                    InfoRow("Email", info.email)
                    InfoRow("Telemóvel", info.contacto)
                    InfoRow("Produtos que precisa", necessidades)
                    InfoRow("Data da última cesta", ultimo)
                    InfoRow("Validade da conta", validade)
                }
                else -> {
                    Text("Dados do apoiado indisponíveis.", fontSize = 12.sp, color = GreyColor)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = GreyColor)
        Text(value.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProdutosSelecionadosList(
    produtos: List<Product>,
    onRemove: (String) -> Unit,
    onVer: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        produtos.forEach { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .border(1.dp, GreenSas, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(p.nomeProduto.ifBlank { p.id }, color = GreenSas, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRemove(p.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remover", tint = WhiteColor)
                        }
                        Button(
                            onClick = { onVer(p.id) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                        ) {
                            Text("Ver")
                        }
                    }
                }
            }
        }

        // Botão adicionar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAdd() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .border(1.dp, GreenSas, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Adicionar um Produto", color = GreenSas, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = GreenSas)
            }
        }
    }
}

@Composable
private fun PickerDialogContainer(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column {
                PickerDialogHeader(title = title, subtitle = subtitle, onDismiss = onDismiss)
                HorizontalDivider(color = DividerGreenLight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
                HorizontalDivider(color = DividerGreenLight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = GreenSas, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerDialogHeader(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(GreenSas)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WhiteColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = WhiteColor.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun PickerMetaPill(text: String, color: Color = GreenSas) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private fun resolveApoiadoStatusColor(status: String): Color = when (status.trim()) {
    "Aprovado" -> GreenSas
    "Conta Expirada" -> WarningOrangeDark
    "Bloqueado", "Negado" -> DarkRed
    "Apoio Pausado" -> StatusOrange
    "Analise" -> StatusBlue
    "Por Submeter" -> GreyColor
    else -> GreyColor
}

@Composable
private fun ApoiadoPickerDialog(
    title: String,
    options: List<ApoiadoOption>,
    onDismiss: () -> Unit,
    onSelect: (ApoiadoOption) -> Unit
) {
    val ipcaFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenSas,
        unfocusedBorderColor = GreenSas,
        focusedLabelColor = GreenSas,
        unfocusedLabelColor = GreenSas,
        focusedPlaceholderColor = GreenSas,
        unfocusedPlaceholderColor = GreenSas,
        cursorColor = GreenSas,
        focusedContainerColor = SurfaceLight,
        unfocusedContainerColor = SurfaceLight,
        disabledContainerColor = SurfaceLight
    )

    var query by remember { mutableStateOf("") }
    val ultimoFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val filtered = remember(options, query) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) options
        else options.filter {
            it.id.lowercase(Locale.getDefault()).contains(q) ||
                    it.nome.lowercase(Locale.getDefault()).contains(q)
        }
    }
    val grouped = remember(filtered) {
        val order = listOf("Aprovado", "Analise", "Por Submeter", "Apoio Pausado", "Negado")
        val orderMap = order.withIndex().associate { it.value to it.index }
        filtered.groupBy { it.displayStatus.ifBlank { "Sem estado" } }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<ApoiadoOption>>>(
                    { orderMap[it.first] ?: Int.MAX_VALUE },
                    { it.first }
                )
            )
    }
    val headerSubtitle = if (query.isBlank()) {
        "${options.size} beneficiários disponíveis"
    } else {
        "${filtered.size} resultados"
    }

    PickerDialogContainer(
        title = title,
        subtitle = headerSubtitle,
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenSas) },
            placeholder = { Text("Pesquisar por nome ou nº mecanográfico", color = GreenSas) },
            shape = RoundedCornerShape(12.dp),
            colors = ipcaFieldColors
        )
        HorizontalDivider(color = DividerGreenLight)

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sem apoiados.", color = GreyColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                grouped.forEach { (status, items) ->
                    val statusColor = resolveApoiadoStatusColor(status)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PickerSectionLabel(text = status, color = statusColor)
                        Text(
                            text = "${items.size}",
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items.forEach { opt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(opt) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteColor),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, CircleShape)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(opt.nome, fontWeight = FontWeight.SemiBold, color = TextDark)
                                    }
                                    Text(opt.id, fontSize = 12.sp, color = GreyColor)
                                }
                                val ultimo = opt.ultimoLevantamento?.let { ultimoFmt.format(it) } ?: "—"
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Ultimo levantamento", fontSize = 10.sp, color = GreyColor)
                                    Text(ultimo, fontSize = 12.sp, color = TextDark)
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
private fun ProdutosPickerDialog(
    produtos: List<Product>,
    selecionadosIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val ipcaFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenSas,
        unfocusedBorderColor = GreenSas,
        focusedLabelColor = GreenSas,
        unfocusedLabelColor = GreenSas,
        focusedPlaceholderColor = GreenSas,
        unfocusedPlaceholderColor = GreenSas,
        cursorColor = GreenSas,
        focusedContainerColor = SurfaceLight,
        unfocusedContainerColor = SurfaceLight,
        disabledContainerColor = SurfaceLight
    )

    var query by remember { mutableStateOf("") }
    val filteredProdutos = remember(produtos, query) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) produtos
        else produtos.filter { p ->
            p.id.lowercase(Locale.getDefault()).contains(q) ||
                    p.nomeProduto.lowercase(Locale.getDefault()).contains(q) ||
                    p.subCategoria.lowercase(Locale.getDefault()).contains(q) ||
                    (p.categoria?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                    (p.codBarras?.lowercase(Locale.getDefault())?.contains(q) == true)
        }
    }

    // Topo: mais próximos do fim da validade
    val proximos = remember(filteredProdutos) {
        filteredProdutos
            .filter { it.validade != null }
            .sortedBy { it.validade }
            .take(8)
    }

    // Resto: agrupado por categoria
    val porCategoria = remember(filteredProdutos) {
        filteredProdutos
            .groupBy { it.categoria?.ifBlank { "Sem categoria" } ?: "Sem categoria" }
            .toSortedMap()
            .mapValues { (_, list) ->
                list.sortedWith(
                    compareBy<Product> { it.validade ?: Date(Long.MAX_VALUE) }
                        .thenBy { it.nomeProduto.lowercase(Locale.getDefault()) }
                )
            }
    }
    val headerSubtitle = if (query.isBlank()) {
        "${produtos.size} produtos disponíveis"
    } else {
        "${filteredProdutos.size} resultados"
    }

    PickerDialogContainer(
        title = "Selecionar produtos",
        subtitle = headerSubtitle,
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenSas) },
            placeholder = { Text("Pesquisar produto (nome, código, categoria)", color = GreenSas) },
            shape = RoundedCornerShape(12.dp),
            colors = ipcaFieldColors
        )
        PickerMetaPill(text = "Selecionados: ${selecionadosIds.size}", color = GreenSas)
        HorizontalDivider(color = DividerGreenLight)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredProdutos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem produtos disponíveis.", color = GreyColor)
                }
            } else {
                if (proximos.isNotEmpty()) {
                    PickerSectionLabel(text = "Mais proximos do fim da validade", color = ReservedOrange)
                    proximos.forEach { p ->
                        ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                    }
                }

                porCategoria.forEach { (cat, list) ->
                    PickerSectionLabel(text = cat, color = GreenSas)
                    list.forEach { p ->
                        ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerSectionLabel(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ProdutoPickRow(
    p: Product,
    selecionadosIds: Set<String>,
    dateFmt: SimpleDateFormat,
    onAdd: (Product) -> Unit
) {
    val already = selecionadosIds.contains(p.id)
    val backgroundColor = if (already) SurfaceMuted else WhiteColor
    val textColor = if (already) GreyColor else TextDark
    val borderColor = if (already) DividerLight else DividerGreenLight
    val validadeTxt = p.validade?.let { dateFmt.format(it) } ?: "—"
    val validadeColor = when {
        p.validade == null -> GreyColor
        p.alertaValidade7d -> WarningOrange
        else -> GreenSas
    }
    val categoriaLabel = listOfNotNull(
        p.categoria?.takeIf { it.isNotBlank() },
        p.subCategoria.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(p.nomeProduto.ifBlank { p.id }, fontWeight = FontWeight.SemiBold, color = textColor)
                if (categoriaLabel.isNotBlank()) {
                    Text(categoriaLabel, fontSize = 12.sp, color = GreyColor)
                    Spacer(Modifier.height(4.dp))
                }
                PickerMetaPill(text = "Validade: $validadeTxt", color = validadeColor)
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onAdd(p) },
                enabled = !already,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenSas,
                    contentColor = WhiteColor,
                    disabledContainerColor = SurfaceMuted,
                    disabledContentColor = GreyColor
                )
            ) {
                if (already) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionado")
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar")
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    MaterialTheme
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
