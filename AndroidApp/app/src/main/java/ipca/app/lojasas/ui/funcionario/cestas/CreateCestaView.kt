package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        placeholder = { Text("") }
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
        cursorColor = GreenSas
    )

    var query by remember { mutableStateOf("") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Pesquisar por nome ou nº mecanográfico", color = GreenSas) },
                    colors = ipcaFieldColors
                )
                Spacer(Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Text("Sem apoiados.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        grouped.forEach { (status, items) ->
                            Text(status, fontWeight = FontWeight.Bold, color = GreenSas)
                            Spacer(Modifier.height(6.dp))
                            items.forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(opt) }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(opt.nome, fontWeight = FontWeight.SemiBold)
                                        Text(opt.id, fontSize = 12.sp, color = GreyColor)
                                    }
                                    Text(
                                        text = opt.ultimoLevantamento?.let {
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                                        } ?: "—",
                                        fontSize = 12.sp,
                                        color = GreyColor
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
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
        cursorColor = GreenSas
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar produtos") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Pesquisar produto (nome, código, categoria)", color = GreenSas) },
                    colors = ipcaFieldColors
                )
                Spacer(Modifier.height(12.dp))

                if (filteredProdutos.isEmpty()) {
                    Text("Sem produtos disponíveis.", color = GreyColor)
                    return@Column
                }

                if (proximos.isNotEmpty()) {
                    Text(
                        text = "Mais próximos do fim da validade",
                        fontWeight = FontWeight.Bold,
                        color = ReservedOrange
                    )
                    Spacer(Modifier.height(8.dp))
                    proximos.forEach { p ->
                        ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                    }
                    Spacer(Modifier.height(14.dp))
                }

                porCategoria.forEach { (cat, list) ->
                    Text(cat, fontWeight = FontWeight.Bold, color = GreenSas)
                    Spacer(Modifier.height(6.dp))
                    list.forEach { p ->
                        ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun ProdutoPickRow(
    p: Product,
    selecionadosIds: Set<String>,
    dateFmt: SimpleDateFormat,
    onAdd: (Product) -> Unit
) {
    val already = selecionadosIds.contains(p.id)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(p.nomeProduto.ifBlank { p.id }, fontWeight = FontWeight.SemiBold)
            val validadeTxt = p.validade?.let { dateFmt.format(it) } ?: "—"
            Text(
                text = "Validade: $validadeTxt",
                fontSize = 12.sp,
                color = GreyColor
            )
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = { onAdd(p) },
            enabled = !already,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
        ) {
            Text(if (already) "Adicionado" else "Adicionar")
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    MaterialTheme
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
