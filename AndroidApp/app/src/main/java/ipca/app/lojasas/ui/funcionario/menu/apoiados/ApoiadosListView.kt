package ipca.app.lojasas.ui.funcionario.menu.apoiados

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.data.apoiado.ApoiadoItem
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import java.util.Calendar
import java.util.Date


@Composable
fun ApoiadosListView(
    navController: NavController,
    viewModel: ApoiadosListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(title = "Gestão de Apoiados", showBack = true, onBack = { navController.popBackStack() })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateApoiado.route) },
                containerColor = GreenSas,
                contentColor = Color.White,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Criar Apoiado", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(GreyBg)
        ) {
            // --- BARRA DE FERRAMENTAS ---
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
                        if (state.searchQuery.isEmpty()) Text("Pesquisar...", color = Color.Gray)
                        innerTextField()
                    }
                )

                // Filtros
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = if(state.currentFilter != "Todos") GreenSas else Color.Black)
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        val filters = listOf(
                            "Todos",
                            "Aprovado",
                            "Conta Expirada",
                            "Bloqueado",
                            "Negado",
                            "Apoio Pausado",
                            "Analise",
                            "Por Submeter"
                        )
                        filters.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter) },
                                onClick = {
                                    viewModel.applyFilter(filter)
                                    showFilterMenu = false
                                },
                                trailingIcon = {
                                    if(state.currentFilter == filter) Icon(Icons.Default.Check, null, tint = GreenSas)
                                }
                            )
                        }
                    }
                }

                // Ordenação
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.Black)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Nome (A-Z)") }, onClick = { viewModel.applySort(SortOrder.NOME_ASC); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Nome (Z-A)") }, onClick = { viewModel.applySort(SortOrder.NOME_DESC); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Nº Mec. (A-Z)") }, onClick = { viewModel.applySort(SortOrder.ID_ASC); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Status") }, onClick = { viewModel.applySort(SortOrder.STATUS_ASC); showSortMenu = false })
                    }
                }

                // Download
                IconButton(onClick = { viewModel.exportToPDF(context) }) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Download PDF",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { viewModel.exportToCSV(context) }) {      
                    Icon(Icons.Default.FileDownload, contentDescription = "Download CSV", tint = Color.Black)
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenSas)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.filteredApoiados) { apoiado ->
                        ApoiadoCard(
                            apoiado = apoiado,
                            onAction = { action ->
                                when(action) {
                                    "block" -> viewModel.blockApoiado(apoiado.id)
                                    "unblock" -> viewModel.unblockApoiado(apoiado.id)
                                    "suspend" -> viewModel.suspendApoiado(apoiado.id)
                                    "reactivate" -> viewModel.reactivateApoiado(apoiado.id)
                                    "details" -> viewModel.selectApoiado(apoiado)
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Modal de Detalhes
    if (state.selectedApoiado != null) {
        val user = state.selectedApoiado!!
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        Dialog(onDismissRequest = { viewModel.selectApoiado(null) }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Detalhes do Apoiado", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenSas)
                        IconButton(onClick = { viewModel.selectApoiado(null) }) { Icon(Icons.Default.Close, null) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportApoiadoPdf(context, user.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar PDF", color = Color.White)
                    }
                    HorizontalDivider(color = GreenSas, modifier = Modifier.padding(vertical = 8.dp))

                    Column(Modifier.weight(1f, fill = false)) {
                        Text(user.nome, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(user.email, fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        DetailRow("Nº Mecanográfico", user.id)
                        DetailRow("Estado", user.displayStatus)
                        DetailRow("Contacto", user.contacto)
                        DetailRow(user.documentType, user.documentNumber)
                        DetailRow("Morada", user.morada)
                        DetailRow("Nacionalidade", user.nacionalidade)
                        DetailRow("Data Nascimento", user.dataNascimento?.let { dateFormat.format(it) } ?: "-")
                    }
                }
            }
        }
    }
}

@Composable
fun ApoiadoCard(apoiado: ApoiadoItem, onAction: (String) -> Unit) {
    // Definir a cor do cabeçalho baseada no status
    val statusColor = when(apoiado.displayStatus) {
        "Aprovado" -> GreenSas
        "Conta Expirada" -> WarningOrangeDark // Laranja Escuro
        "Bloqueado", "Negado" -> DarkRed // Vermelho Escuro
        "Apoio Pausado" -> StatusOrange // Laranja
        "Analise" -> StatusBlue // Azul
        else -> Color.Gray // Cinzento para outros (Por Submeter, etc)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Fundo do cabeçalho agora usa a cor dinâmica 'statusColor'
            Box(Modifier.fillMaxWidth().background(statusColor).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(text = apoiado.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Name:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(apoiado.nome, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Text("E-mail:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(apoiado.email, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        // Texto do status também usa a mesma cor para consistência
                        Text(text = apoiado.displayStatus, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onAction("details") }) { Text("Detalhes", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Botões de ação dinâmica
                    when (apoiado.rawStatus) {
                        "Bloqueado" -> ActionButton("Desbloquear", GreenSas) { onAction("unblock") }
                        "Aprovado" -> {
                            ActionButton("Pausar Apoio", StatusOrange) { onAction("suspend") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", Color.Black) { onAction("block") }
                        }
                        "Suspenso" -> {
                            ActionButton("Reativar", GreenSas) { onAction("reactivate") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", Color.Black) { onAction("block") }
                        }
                        else -> ActionButton("Bloquear", Color.Black) { onAction("block") }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
