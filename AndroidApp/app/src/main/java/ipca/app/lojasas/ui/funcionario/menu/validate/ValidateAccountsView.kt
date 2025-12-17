package ipca.app.lojasas.ui.funcionario.menu.validate

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.GreenSas // Assegura que tens esta cor no Theme ou define Color(0xFF094E33)

@Composable
fun ValidateAccountsView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ValidateAccountsViewModel = viewModel()
    val state by viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppHeader(
                title = "Validar Contas",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F2F2)) // Fundo cinza claro
        ) {
            if (state.isLoading && state.selectedApoiadoDetails == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
            } else if (state.pendingAccounts.isEmpty()) {
                Text(
                    text = "Não existem contas pendentes.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.pendingAccounts) { account ->
                        ValidateAccountCard(
                            account = account,
                            onActionClick = { viewModel.selectApoiado(account.id) }
                        )
                    }
                }
            }

            // --- POP-UP DE DETALHES ---
            if (state.selectedApoiadoDetails != null) {
                ApoiadoDetailDialog(
                    details = state.selectedApoiadoDetails!!,
                    documents = state.apoaidoDocuments,
                    isLoading = state.isLoading,
                    onDismiss = { viewModel.clearSelection() },
                    onApprove = { viewModel.approveAccount(state.selectedApoiadoDetails!!.id) { Toast.makeText(context, "Conta Aprovada!", Toast.LENGTH_SHORT).show() } },
                    onBlock = { viewModel.blockAccount(state.selectedApoiadoDetails!!.id) { Toast.makeText(context, "Conta Bloqueada!", Toast.LENGTH_SHORT).show() } },
                    onDeny = { reason -> viewModel.denyAccount(state.selectedApoiadoDetails!!.id, reason) { Toast.makeText(context, "Conta Negada.", Toast.LENGTH_SHORT).show() } },
                    onOpenFile = { path ->
                        viewModel.getFileUri(path) { uri ->
                            if (uri != null) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } else {
                                Toast.makeText(context, "Erro ao abrir ficheiro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- CARD ESTILO "IMAGEM FORNECIDA" ---
@Composable
fun ValidateAccountCard(
    account: ApoiadoSummary,
    onActionClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Verde
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenSas)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = account.nome,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Body Branco
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(text = "Info: Pedido de Validação", color = Color.Black, fontSize = 14.sp)
                Text(text = "When: Pendente", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Botões alinhados à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Action (Validar)
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Validar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- DIALOG DE DETALHES GIGANTE ---
@Composable
fun ApoiadoDetailDialog(
    details: ApoiadoDetails,
    documents: List<DocumentSummary>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onDeny: (String) -> Unit,
    onBlock: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    var showDenyInput by remember { mutableStateOf(false) }
    var denyReason by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Ocupa quase todo o ecrã
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header com botão fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Detalhes do Apoiado", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenSas)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                HorizontalDivider(color = GreenSas, thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Conteúdo Scrollável
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dados Pessoais
                    DetailRow("Nome", details.nome)
                    DetailRow("Nº Mecanográfico", details.id)
                    DetailRow("Tipo", details.tipo)
                    DetailRow("Contacto", details.contacto)
                    DetailRow("Email", details.email)
                    DetailRow("Morada", details.morada)
                    DetailRow("NIF/Passaporte", details.nif)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Documentos Submetidos:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GreenSas)

                    if (documents.isEmpty()) {
                        Text("Nenhum documento encontrado.", color = Color.Gray)
                    } else {
                        documents.forEach { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFile(doc.url) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = GreenSas)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    // Mostra "Titulo (Entrega X)"
                                    Row {
                                        Text(doc.typeTitle, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(Entrega ${doc.entrega})", fontSize = 12.sp, color = GreenSas)
                                    }
                                    Text(doc.fileName, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Área de Ações
                if (showDenyInput) {
                    // Layout para negar
                    Text("Motivo da Negação:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = denyReason,
                        onValueChange = { denyReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escreva a justificação...") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDenyInput = false }) { Text("Cancelar", color = Color.Gray) }
                        Button(
                            onClick = { if(denyReason.isNotEmpty()) onDeny(denyReason) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Confirmar Negação") }
                    }

                } else {
                    // Botões Principais
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // BLOQUEAR
                        Button(
                            onClick = onBlock,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            enabled = !isLoading
                        ) { Text("Bloquear") }

                        // NEGAR
                        Button(
                            onClick = { showDenyInput = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            enabled = !isLoading
                        ) { Text("Negar") }

                        // APROVAR
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            enabled = !isLoading
                        ) { Text("Aprovar") }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}