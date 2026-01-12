package ipca.app.lojasas.ui.funcionario.menu.validate

import ipca.app.lojasas.ui.theme.*
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
                        Text("Decisão", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                VerdictDialogHeader(
                    title = "Decisão sobre o Beneficiário",
                    subtitle = "${details.nome} • ${details.id}",
                    onDismiss = onDismiss
                )

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    VerdictSummaryCard(details = details)
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailSection(title = "Identificação") {
                            DetailRow("Nome", details.nome)
                            DetailRow("Nº Mecanográfico", details.id)
                            DetailRow("Nacionalidade", details.nacionalidade)
                            DetailRow("Data de Nascimento", details.dataNascimento?.let { dateFormatter.format(it) } ?: "N/A")
                            DetailRow(details.documentType, details.documentNumber)
                        }

                        DetailSection(title = "Contactos & Morada") {
                            DetailRow("Email", details.email)
                            DetailRow("Telemóvel", details.contacto)
                            DetailRow("Morada", details.morada)
                        }

                        DetailSection(title = "Situação Sócio-Económica") {
                            DetailRow("Relação IPCA", details.tipo)

                            if (details.tipo == "Estudante") {
                                DetailRow("Curso", details.curso ?: "N/A")
                                DetailRow("Grau", details.grauEnsino ?: "N/A")
                                DetailRow("Bolsa de Estudos?", if (details.bolsaEstudos) "Sim" else "Não")
                                if (details.bolsaEstudos) {
                                    DetailRow("Valor Bolsa", "${details.valorBolsa} €")
                                }
                            }

                            DetailRow(
                                "Apoio Emergência Social?",
                                if (details.apoioEmergencia) "Sim (Prioritário)" else "Não"
                            )

                            if (details.necessidades.isNotEmpty()) {
                                DetailRow("Necessidades Declaradas", details.necessidades.joinToString(", "))
                            } else {
                                DetailRow("Necessidades", "Nenhuma selecionada")
                            }
                        }

                        DetailSection(title = "Documentos Submetidos") {
                            if (documents.isEmpty()) {
                                Text(
                                    "Nenhum documento encontrado.",
                                    color = GreyColor,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    documents.forEach { doc ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenFile(doc.url) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                            border = BorderStroke(1.dp, DividerGreenLight),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = GreenSas
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(doc.typeTitle, fontWeight = FontWeight.Bold)
                                                    Text(doc.fileName, fontSize = 12.sp, color = GreyColor)
                                                    VerdictInfoChip(text = "Entrega ${doc.entrega}", color = GreenSas)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (showDenyInput) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, DividerGreenLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Motivo da Negação", fontWeight = FontWeight.Bold, color = TextDark)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = denyReason,
                                    onValueChange = { denyReason = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Escreva a justificação...", color = GreyColor) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenSas,
                                        unfocusedBorderColor = DividerGreenLight,
                                        focusedLabelColor = GreenSas,
                                        unfocusedLabelColor = GreenSas,
                                        focusedPlaceholderColor = GreyColor,
                                        unfocusedPlaceholderColor = GreyColor,
                                        cursorColor = GreenSas,
                                        focusedContainerColor = WhiteColor,
                                        unfocusedContainerColor = WhiteColor
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { showDenyInput = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancelar", color = GreyColor)
                                    }
                                    Button(
                                        onClick = { if (denyReason.isNotEmpty()) onDeny(denyReason) },
                                        enabled = denyReason.isNotEmpty() && !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Confirmar") }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onBlock,
                                colors = ButtonDefaults.buttonColors(containerColor = BlackColor),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Bloquear") }
                            Button(
                                onClick = { showDenyInput = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Negar") }
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Aprovar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerdictDialogHeader(
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
                Text(subtitle, fontSize = 12.sp, color = WhiteColor.copy(alpha = 0.85f))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun VerdictSummaryCard(details: ApoiadoDetails) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(details.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text("Nº Mecanográfico: ${details.id}", fontSize = 12.sp, color = GreyColor)
                Text(details.email, fontSize = 12.sp, color = GreyColor)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VerdictInfoChip(text = "Pendente", color = WarningOrange)
                if (details.dadosIncompletos) {
                    VerdictInfoChip(text = "Dados incompletos", color = WarningOrangeDark)
                }
                if (details.apoioEmergencia) {
                    VerdictInfoChip(text = "Prioritário", color = StatusOrange)
                }
            }
        }
    }
}

@Composable
private fun VerdictInfoChip(text: String, color: Color) {
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
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun DetailSection(
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
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = GreyColor)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
