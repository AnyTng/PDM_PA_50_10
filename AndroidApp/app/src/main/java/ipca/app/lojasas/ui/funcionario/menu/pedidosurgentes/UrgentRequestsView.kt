package ipca.app.lojasas.ui.funcionario.menu.pedidosurgentes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.funcionario.pedidosurgentes.PedidoUrgenteItem
import ipca.app.lojasas.ui.funcionario.pedidosurgentes.UrgentRequestsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val GreenSas = Color(0xFF094E33)
private val GreyBg = Color(0xFFF2F2F2)
private val OrangeWarning = Color(0xFFE6A519) // Cor para destacar a ação pendente

@Composable
fun UrgentRequestsView(
    navController: NavController,
    viewModel: UrgentRequestsViewModel = viewModel()
) {
    val state by viewModel.uiState
    val dateFmt = rememberDateFormatter()
    val context = LocalContext.current
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Numero mecanografico...", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ano: ${state.selectedYear}", fontSize = 12.sp, color = Color.Gray)
                            IconButton(onClick = { showYearMenu = true }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Filtrar por ano",
                                    tint = if (state.selectedYear != "Todos") GreenSas else Color.Black
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
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.exportToPDF(context) }) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar PDF",
                            tint = Color.Black
                        )
                    }
                }

                state.error?.let {
                    Text(
                        it,
                        color = Color.Red,
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
                        Text(emptyText, color = Color.Gray)
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

    val estadoLabel = when {
        estadoNorm == "preparar_apoio" || estadoNorm == "preparar apoio" -> "Aprovado"
        estadoNorm == "negado" -> "Negado"
        isAnalise -> "Em Análise"
        // Se tiver cestaId, podemos até mostrar que já tem cesta
        !pedido.cestaId.isNullOrBlank() -> "Concluído (Cesta Criada)"
        else -> pedido.estado.ifBlank { "—" }
    }

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
                    text = "Apoiado: ${pedido.numeroMecanografico}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSas
                )
                Text(
                    text = estadoLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = when {
                        estadoNorm == "negado" -> Color(0xFFB00020)
                        !pedido.cestaId.isNullOrBlank() -> GreenSas // Verde se já tiver cesta
                        else -> Color.Gray
                    }
                )
            }

            pedido.dataSubmissao?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Submetido: ${dateFmt.format(it)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = pedido.descricao.ifBlank { "(Sem descrição)" },
                fontSize = 14.sp,
                color = Color.Black
            )

            // CASO 1: EM ANÁLISE (Botões de Decisão)
            if (isAnalise) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNegar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                    ) {
                        Text("Negar")
                    }
                    Button(
                        onClick = onAprovar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Aprovar")
                    }
                }
            }

            // CASO 2: APROVADO MAS FALTA CRIAR CESTA (Botão de Recuperação)
            if (isAprovadoSemCesta) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onCriarCesta,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeWarning)
                ) {
                    Text("⚠️ Finalizar: Criar Cesta", color = Color.White, fontWeight = FontWeight.Bold)
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
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
