package ipca.app.lojasas.ui.funcionario.menu.apoiados

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.funcionario.menu.validate.DetailRow // Reutiliza se possível, ou recria abaixo
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ApoiadosListView(
    navController: NavController,
    viewModel: ApoiadosListViewModel = viewModel()
) {
    val state by viewModel.uiState
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(title = "Gestão de Apoiados", showBack = true, onBack = { navController.popBackStack() })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Lógica futura de adicionar manualmente se necessário */ },
                containerColor = GreenSas,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp) // Tamanho grande como na imagem
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F2F2))
        ) {
            // --- BARRA DE PESQUISA E FILTROS (Estilo da Imagem) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pesquisar...", color = Color.Gray, modifier = Modifier.weight(1f))

                // Filtro
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = Color.Black)
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        val filters = listOf("Todos", "Aprovado", "Bloqueado", "Negado", "Suspenso", "Analise", "Por Submeter")
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
                IconButton(onClick = { /* Ordenar */ }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.Black)
                }
                IconButton(onClick = { /* Download */ }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Download", tint = Color.Black)
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
                    // Espaço extra para o FAB não tapar o último item
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // --- MODAL DE DETALHES ---
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
                        IconButton(onClick = { viewModel.selectApoiado(null) }) {
                            Icon(Icons.Default.Close, null)
                        }
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
fun ApoiadoCard(
    apoiado: ApoiadoItem,
    onAction: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // --- HEADER VERDE (Como na imagem) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenSas)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = apoiado.nome,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // --- CORPO BRANCO ---
            Column(modifier = Modifier.padding(16.dp)) {

                // Informações
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Name:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(apoiado.nome, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

                        Text("E-mail:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(apoiado.email, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        val statusColor = when(apoiado.displayStatus) {
                            "Aprovado" -> GreenSas
                            "Bloqueado", "Negado" -> Color.Red
                            "Suspenso" -> Color(0xFFD88C28) // Laranja
                            else -> Color.Gray
                        }
                        Text(
                            text = apoiado.displayStatus,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- BOTÕES DE AÇÃO (Alinhados à direita) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Botão Detalhes (Sempre visível, "More" na imagem)
                    TextButton(onClick = { onAction("details") }) {
                        Text("Detalhes", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botões Condicionais por Estado
                    when (apoiado.rawStatus) {
                        "Bloqueado" -> {
                            ActionButton("Desbloquear", GreenSas) { onAction("unblock") }
                        }
                        "Aprovado" -> {
                            ActionButton("Suspender", Color(0xFFD88C28)) { onAction("suspend") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", Color.Black) { onAction("block") }
                        }
                        "Suspenso" -> {
                            ActionButton("Reativar", GreenSas) { onAction("reactivate") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", Color.Black) { onAction("block") }
                        }
                        else -> {
                            // "Negado", "Analise", "Falta_Documentos"
                            // Apenas botão de Bloquear
                            ActionButton("Bloquear", Color.Black) { onAction("block") }
                        }
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

// Pequeno helper para os detalhes se não quiseres importar de outro ficheiro
@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}