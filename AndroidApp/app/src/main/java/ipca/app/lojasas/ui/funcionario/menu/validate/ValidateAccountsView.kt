package ipca.app.lojasas.ui.funcionario.menu.validate

import ipca.app.lojasas.ui.theme.*
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.apoiado.ApoiadoDetails
import ipca.app.lojasas.data.apoiado.ApoiadoSummary
import ipca.app.lojasas.data.apoiado.DocumentSummary
import ipca.app.lojasas.ui.components.AppHeader
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ValidateAccountsView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ValidateAccountsViewModel = hiltViewModel()
    val state by viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(title = "Validar Contas", showBack = true, onBack = { navController.popBackStack() })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GreyBg)
        ) {
            if (state.isLoading && state.selectedApoiadoDetails == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
            } else if (state.pendingAccounts.isEmpty()) {
                Text("Não existem contas pendentes.", modifier = Modifier.align(Alignment.Center), color = GreyColor)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.pendingAccounts) { account ->
                        ValidateAccountCard(account = account, onActionClick = { viewModel.selectApoiado(account.id) })
                    }
                }
            }

            if (state.selectedApoiadoDetails != null) {
                ApoiadoDetailDialog(
                    details = state.selectedApoiadoDetails!!,
                    documents = state.apoiadoDocuments,
                    isLoading = state.isLoading,
                    onDismiss = { viewModel.clearSelection() },
                    onApprove = { viewModel.approveAccount(state.selectedApoiadoDetails!!.id) { Toast.makeText(context, "Conta Aprovada!", Toast.LENGTH_SHORT).show() } },
                    onBlock = { viewModel.blockAccount(state.selectedApoiadoDetails!!.id) { Toast.makeText(context, "Conta Bloqueada!", Toast.LENGTH_SHORT).show() } },
                    onDeny = { reason -> viewModel.denyAccount(state.selectedApoiadoDetails!!.id, reason) { Toast.makeText(context, "Conta Negada.", Toast.LENGTH_SHORT).show() } },
                    onOpenFile = { path ->
                        viewModel.getFileUri(path) { uri ->
                            if (uri != null) context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            else Toast.makeText(context, "Erro ao abrir ficheiro", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ValidateAccountCard(account: ApoiadoSummary, onActionClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().background(GreenSas).padding(16.dp, 10.dp)) {
                Text(account.nome, color = WhiteColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(Modifier.fillMaxWidth().background(WhiteColor).padding(16.dp)) {
                Text("Info: Pedido de Validação", color = BlackColor, fontSize = 14.sp)
                Text("Estado: Pendente", color = GreyColor, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onActionClick, colors = ButtonDefaults.buttonColors(containerColor = GreenSas), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(24.dp, 8.dp), modifier = Modifier.height(36.dp)) {
                        Text("Veredito", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Detalhes Completos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenSas)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fechar") }
                }
                HorizontalDivider(color = GreenSas, thickness = 2.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Scroll Content
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECÇÃO 1: DADOS PESSOAIS
                    Text("Identificação", fontWeight = FontWeight.Bold, color = GreenSas, fontSize = 16.sp)
                    DetailRow("Nome", details.nome)
                    DetailRow("Nº Mecanográfico", details.id)
                    DetailRow("Nacionalidade", details.nacionalidade)
                    DetailRow("Data de Nascimento", details.dataNascimento?.let { dateFormatter.format(it) } ?: "N/A")

                    // --- ALTERAÇÃO: Usa o tipo dinâmico (NIF ou Passaporte) ---
                    DetailRow(details.documentType, details.documentNumber)

                    Spacer(Modifier.height(8.dp))

                    // SECÇÃO 2: CONTACTOS
                    Text("Contactos & Morada", fontWeight = FontWeight.Bold, color = GreenSas, fontSize = 16.sp)
                    DetailRow("Email", details.email)
                    DetailRow("Telemóvel", details.contacto)
                    DetailRow("Morada", details.morada)

                    Spacer(Modifier.height(8.dp))

                    // SECÇÃO 3: DADOS SÓCIO-ECONÓMICOS
                    Text("Situação Sócio-Económica", fontWeight = FontWeight.Bold, color = GreenSas, fontSize = 16.sp)
                    DetailRow("Relação IPCA", details.tipo)

                    if (details.tipo == "Estudante") {
                        DetailRow("Curso", details.curso ?: "N/A")
                        DetailRow("Grau", details.grauEnsino ?: "N/A")
                        DetailRow("Bolsa de Estudos?", if(details.bolsaEstudos) "Sim" else "Não")
                        if (details.bolsaEstudos) {
                            DetailRow("Valor Bolsa", "${details.valorBolsa} €")
                        }
                    }

                    DetailRow("Apoio Emergência Social?", if(details.apoioEmergencia) "Sim (Prioritário)" else "Não")

                    // Necessidades
                    if (details.necessidades.isNotEmpty()) {
                        DetailRow("Necessidades Declaradas", details.necessidades.joinToString(", "))
                    } else {
                        DetailRow("Necessidades", "Nenhuma selecionada")
                    }

                    Spacer(Modifier.height(16.dp))

                    // SECÇÃO 4: DOCUMENTOS
                    Text("Documentos Submetidos", fontWeight = FontWeight.Bold, color = GreenSas, fontSize = 18.sp)
                    if (documents.isEmpty()) {
                        Text("Nenhum documento encontrado.", color = GreyColor, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        documents.forEach { doc ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenFile(doc.url) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = GreenSas)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row {
                                        Text(doc.typeTitle, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(4.dp))
                                        Text("(Entrega ${doc.entrega})", fontSize = 12.sp, color = GreenSas)
                                    }
                                    Text(doc.fileName, fontSize = 12.sp, color = GreyColor)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Área de Ações
                if (showDenyInput) {
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
                        TextButton(onClick = { showDenyInput = false }) { Text("Cancelar", color = GreyColor) }
                        Button(
                            onClick = { if(denyReason.isNotEmpty()) onDeny(denyReason) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                        ) { Text("Confirmar Negação") }
                    }

                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onBlock, colors = ButtonDefaults.buttonColors(containerColor = BlackColor), modifier = Modifier.weight(1f).padding(end = 4.dp), enabled = !isLoading) { Text("Bloquear") }
                        Button(onClick = { showDenyInput = true }, colors = ButtonDefaults.buttonColors(containerColor = RedColor), modifier = Modifier.weight(1f).padding(horizontal = 4.dp), enabled = !isLoading) { Text("Negar") }
                        Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = GreenSas), modifier = Modifier.weight(1f).padding(start = 4.dp), enabled = !isLoading) { Text("Aprovar") }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = GreyColor)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
