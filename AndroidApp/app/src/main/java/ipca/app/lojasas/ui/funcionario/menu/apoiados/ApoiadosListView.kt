package ipca.app.lojasas.ui.funcionario.menu.apoiados

import android.widget.Toast
import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
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
    var itemToDelete by remember { mutableStateOf<ApoiadoItem?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(title = "Gestão de Apoiados", showBack = true, onBack = { navController.popBackStack() })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateApoiado.route) },
                containerColor = GreenSas,
                contentColor = WhiteColor,
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
                    .background(WhiteColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = GreyColor)
                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 16.sp, color = BlackColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (state.searchQuery.isEmpty()) Text("Pesquisar...", color = GreyColor)
                        innerTextField()
                    }
                )

                // Filtros
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = if(state.currentFilter != "Todos") GreenSas else BlackColor)
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
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = BlackColor)
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
                        tint = BlackColor
                    )
                }
                IconButton(onClick = { viewModel.exportToCSV(context) }) {      
                    Icon(Icons.Default.FileDownload, contentDescription = "Download CSV", tint = BlackColor)
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
        val statusColor = resolveApoiadoStatusColor(user.displayStatus)
        val contactLabel = user.contacto.trim().ifBlank { "-" }
        val emailLabel = user.email.ifBlank { "-" }
        val documentTitle = user.documentType.ifBlank { "Documento" }
        val documentNumber = user.documentNumber.ifBlank { "-" }
        val addressLabel = user.morada.ifBlank { "-" }
        val nationalityLabel = user.nacionalidade.ifBlank { "-" }

        Dialog(onDismissRequest = { viewModel.selectApoiado(null) }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DividerGreenLight)
            ) {
                Column {
                    ApoiadoDialogHeader(
                        title = "Detalhes do Apoiado",
                        subtitle = "${user.nome} • ${user.id}",
                        onDismiss = { viewModel.selectApoiado(null) }
                    )

                    Column(Modifier.padding(16.dp)) {
                        ApoiadoSummaryCard(user = user, statusColor = statusColor)
                        Spacer(Modifier.height(12.dp))

                        ApoiadoDetailSection(title = "Informação geral") {
                            DetailRow("Nº Mecanográfico", user.id)
                            DetailRow("Contacto", contactLabel)
                            DetailRow("Email", emailLabel)
                            DetailRow(documentTitle, documentNumber)
                            DetailRow("Morada", addressLabel)
                            DetailRow("Nacionalidade", nationalityLabel)
                            DetailRow("Data Nascimento", user.dataNascimento?.let { dateFormat.format(it) } ?: "-")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.exportApoiadoPdf(context, user.id) },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas, contentColor = WhiteColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Exportar PDF", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (state.isAdmin) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { itemToDelete = user },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedColor, contentColor = WhiteColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Apagar perfil", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Apagar Beneficiário") },
            text = { Text("Tem a certeza que deseja apagar ${itemToDelete?.nome}? Esta ação é irreversível.") },
            confirmButton = {
                Button(
                    onClick = {
                        val item = itemToDelete
                        if (item != null) {
                            viewModel.deleteApoiado(
                                item = item,
                                onSuccess = {
                                    Toast.makeText(context, "Beneficiário removido", Toast.LENGTH_SHORT).show()
                                    viewModel.selectApoiado(null)
                                },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor, contentColor = WhiteColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("Apagar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = GreyColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = WhiteColor,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun ApoiadoCard(apoiado: ApoiadoItem, onAction: (String) -> Unit) {
    val statusColor = resolveApoiadoStatusColor(apoiado.displayStatus)
    val contactLabel = apoiado.contacto.trim()

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
                    .background(statusColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = apoiado.nome,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BlackColor
                        )
                        Text(
                            text = "Nº Mec.: ${apoiado.id}",
                            fontSize = 12.sp,
                            color = GreyColor
                        )
                    }
                    ApoiadoStatusPill(label = apoiado.displayStatus, color = statusColor)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = apoiado.email,
                    fontSize = 12.sp,
                    color = DarkGreyColor
                )
                if (contactLabel.isNotBlank()) {
                    Text(
                        text = "Contacto: $contactLabel",
                        fontSize = 12.sp,
                        color = GreyColor
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerGreenLight)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onAction("details") },
                        colors = ButtonDefaults.textButtonColors(contentColor = GreyColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Detalhes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    when (apoiado.rawStatus) {
                        "Bloqueado" -> ActionButton("Desbloquear", GreenSas) { onAction("unblock") }
                        "Aprovado" -> {
                            ActionButton("Pausar Apoio", StatusOrange) { onAction("suspend") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", BlackColor) { onAction("block") }
                        }
                        "Suspenso" -> {
                            ActionButton("Reativar", GreenSas) { onAction("reactivate") }
                            Spacer(modifier = Modifier.width(8.dp))
                            ActionButton("Bloquear", BlackColor) { onAction("block") }
                        }
                        else -> ActionButton("Bloquear", BlackColor) { onAction("block") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApoiadoStatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
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
private fun ApoiadoDialogHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenSas)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WhiteColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = WhiteColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun ApoiadoSummaryCard(user: ApoiadoItem, statusColor: Color) {
    val contactLabel = user.contacto.trim()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, DividerGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text("Nº Mecanográfico: ${user.id}", fontSize = 12.sp, color = GreyColor)
                Text(user.email.ifBlank { "-" }, fontSize = 12.sp, color = GreyColor)
                if (contactLabel.isNotBlank()) {
                    Text("Contacto: $contactLabel", fontSize = 12.sp, color = GreyColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                ApoiadoStatusPill(label = user.displayStatus, color = statusColor)
            }
        }
    }
}

@Composable
private fun ApoiadoDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        border = BorderStroke(1.dp, DividerGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = GreenSas, fontSize = 15.sp)
            HorizontalDivider(color = DividerGreenLight, modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = WhiteColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
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

private fun resolveApoiadoStatusColor(status: String): Color {
    return when (status) {
        "Aprovado" -> GreenSas
        "Conta Expirada" -> WarningOrangeDark
        "Bloqueado", "Negado" -> DarkRed
        "Apoio Pausado" -> StatusOrange
        "Analise" -> StatusBlue
        else -> GreyColor
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = GreyColor)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
