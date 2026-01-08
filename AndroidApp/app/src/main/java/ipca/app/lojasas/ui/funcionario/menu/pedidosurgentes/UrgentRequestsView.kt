package ipca.app.lojasas.ui.funcionario.menu.pedidosurgentes

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ipca.app.lojasas.ui.funcionario.pedidosurgentes.UrgentRequestsViewModel
import ipca.app.lojasas.data.requests.PedidoUrgenteItem
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun UrgentRequestsView(
    navController: NavController,
    viewModel: UrgentRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val dateFmt = rememberDateFormatter()
    val context = LocalContext.current
    val filtersScrollState = rememberScrollState()
    var showYearMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize()) {
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
                                val yearColor = if (state.selectedYear != "Todos") GreenSas else GreyColor
                                TextButton(onClick = { showYearMenu = true }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Ano: ${state.selectedYear}",
                                            color = yearColor,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = yearColor
                                        )
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
                        }

                        IconButton(onClick = { viewModel.exportToPDF(context) }) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar PDF",
                                tint = BlackColor
                            )
                        }
                    }
                }

                state.error?.let {
                    Text(
                        it,
                        color = RedColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (state.filteredPedidos.isEmpty()) {
                    val emptyText = if (state.pedidos.isEmpty()) {
                        "Sem pedidos urgentes."
                    } else {
                        "Sem resultados para os filtros."
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emptyText, color = GreyColor)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredPedidos, key = { it.id }) { pedido ->
                            PedidoUrgenteCard(
                                pedido = pedido,
                                dateFmt = dateFmt,
                                onNegar = {
                                    viewModel.negarPedido(pedido.id)
                                },
                                onAprovar = {
                                    // 1) Marca como aprovado
                                    viewModel.aprovarPedido(pedido.id) { ok ->
                                        if (ok) {
                                            // 2) Encaminha para criar cesta
                                            navController.navigate(
                                                Screen.CreateCestaUrgente.createRoute(pedido.id, pedido.numeroMecanografico)
                                            )
                                        }
                                    }
                                },
                                onCriarCesta = {
                                    // Botao de recuperacao: Vai direto para a criacao de cesta
                                    navController.navigate(
                                        Screen.CreateCestaUrgente.createRoute(pedido.id, pedido.numeroMecanografico)
                                    )
                                },
                                onExportPdf = {
                                    viewModel.exportPedidoPdf(context, pedido)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PedidoUrgenteCard(
    pedido: PedidoUrgenteItem,
    dateFmt: SimpleDateFormat,
    onNegar: () -> Unit,
    onAprovar: () -> Unit,
    onCriarCesta: () -> Unit,
    onExportPdf: () -> Unit
) {
    val estadoNorm = pedido.estado.trim().lowercase(Locale.getDefault())

    // Estado: Em Análise
    val isAnalise = estadoNorm.isBlank() || estadoNorm == "analise" || estadoNorm == "em analise" || estadoNorm == "em_analise"

    // Estado: Aprovado (Preparar Apoio), mas ainda SEM Cesta criada (cestaId é null ou vazio)
    val isAprovadoSemCesta = (estadoNorm == "preparar_apoio" || estadoNorm == "preparar apoio") && pedido.cestaId.isNullOrBlank()
    val hasCesta = !pedido.cestaId.isNullOrBlank()

    val estadoLabel = when {
        estadoNorm == "preparar_apoio" || estadoNorm == "preparar apoio" -> "Aprovado"
        estadoNorm == "negado" -> "Negado"
        isAnalise -> "Em Análise"
        hasCesta -> "Cesta criada"
        else -> pedido.estado.ifBlank { "—" }
    }
    val accentColor = resolvePedidoAccentColor(estadoNorm, isAnalise, isAprovadoSemCesta, hasCesta)

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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Apoiado", fontSize = 11.sp, color = GreyColor)
                        Text(
                            text = pedido.numeroMecanografico,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GreenSas
                        )
                    }
                    UrgentStatusPill(label = estadoLabel, color = accentColor)
                }

                if (pedido.dataSubmissao != null || pedido.dataDecisao != null) {
                    Spacer(Modifier.height(8.dp))
                    pedido.dataSubmissao?.let {
                        Text(
                            text = "Submetido: ${dateFmt.format(it)}",
                            fontSize = 12.sp,
                            color = GreyColor
                        )
                    }
                    pedido.dataDecisao?.let {
                        Text(
                            text = "Decisão: ${dateFmt.format(it)}",
                            fontSize = 12.sp,
                            color = GreyColor
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = pedido.descricao.ifBlank { "(Sem descrição)" },
                    fontSize = 14.sp,
                    color = TextDark
                )

                if (isAnalise) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UrgentActionButton(
                            text = "Negar",
                            color = ErrorRed,
                            modifier = Modifier.weight(1f),
                            onClick = onNegar
                        )
                        UrgentActionButton(
                            text = "Aprovar",
                            color = GreenSas,
                            modifier = Modifier.weight(1f),
                            onClick = onAprovar
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GreenSas)
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar PDF", color = GreenSas, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (isAprovadoSemCesta) {
                    Spacer(Modifier.height(12.dp))
                    UrgentActionButton(
                        text = "Finalizar: Criar Cesta",
                        color = PendingOrange,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCriarCesta
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}

@Composable
private fun UrgentStatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UrgentActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = WhiteColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun resolvePedidoAccentColor(
    estadoNorm: String,
    isAnalise: Boolean,
    isAprovadoSemCesta: Boolean,
    hasCesta: Boolean
): Color {
    return when {
        estadoNorm == "negado" -> ErrorRed
        hasCesta -> GreenSas
        isAprovadoSemCesta -> PendingOrange
        isAnalise -> StatusBlue
        else -> GreyColor
    }
}
