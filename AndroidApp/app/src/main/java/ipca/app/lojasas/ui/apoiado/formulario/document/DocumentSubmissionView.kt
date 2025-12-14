package ipca.app.lojasas.ui.apoiado.formulario.document

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val GreenSas = Color(0xFF094E33)

// --- 1. ECRÃ COM LÓGICA (ViewModel, Firebase) ---
@Composable
fun DocumentSubmissionView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: DocumentSubmissionViewModel = viewModel()
    val state by viewModel.uiState
    val context = LocalContext.current

    var currentDocType by remember { mutableStateOf<DocumentItem?>(null) }
    var tempDescription by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                currentDocType?.let { docType ->
                    viewModel.uploadDocument(context, it, docType, tempDescription)
                    tempDescription = null
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.loadSubmissionStatus()
    }

    fun openPreview(storagePath: String) {
        viewModel.getFileUrl(storagePath) { uri ->
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Não foi possível abrir o ficheiro.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Erro ao obter o ficheiro.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DocumentSubmissionContent(
        state = state,
        onUploadRequest = { docType, description ->
            currentDocType = docType
            tempDescription = description
            launcher.launch(arrayOf("application/pdf", "image/*"))
        },
        onPreviewRequest = { path -> openPreview(path) },
        onFinishRequest = {
            if (viewModel.hasAllMandatoryFiles()) {
                viewModel.finalizeSubmission {
                    Toast.makeText(context, "Entrega concluída!", Toast.LENGTH_SHORT).show()
                    navController.navigate("apoiadoHome") {
                        popUpTo("documentSubmission") { inclusive = true }
                    }
                }
            } else {
                Toast.makeText(context, "Por favor, submeta todos os documentos obrigatórios.", Toast.LENGTH_LONG).show()
            }
        },
        checkMandatory = { viewModel.hasAllMandatoryFiles() }
    )
}

// --- 2. CONTEÚDO VISUAL ---
@Composable
fun DocumentSubmissionContent(
    state: SubmissionState,
    onUploadRequest: (DocumentItem, String?) -> Unit,
    onPreviewRequest: (String) -> Unit,
    onFinishRequest: () -> Unit,
    checkMandatory: () -> Boolean
) {
    var showOtherDescriptionDialog by remember { mutableStateOf(false) }
    var otherDescriptionText by remember { mutableStateOf("") }
    var pendingDocType by remember { mutableStateOf<DocumentItem?>(null) }

    fun initiateUpload(docType: DocumentItem) {
        // Impede clique se já estiver a carregar
        if (state.uploadProgress) return

        if (docType.id == "outros") {
            pendingDocType = docType
            showOtherDescriptionDialog = true
        } else {
            onUploadRequest(docType, null)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenSas)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Entrega de Documentos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                onClick = onFinishRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checkMandatory()) GreenSas else Color.Gray
                ),
                // Desativa o botão se estiver a carregar
                enabled = !state.uploadProgress && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Concluir e Voltar")
            }
        }
    ) { innerPadding ->

        // BOX PRINCIPAL para permitir o Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {

            // CONTEÚDO DA LISTA
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (state.error != null) {
                    Text(state.error!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    item {
                        Text("Selecione o tipo para adicionar:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    items(state.docTypes) { docType ->
                        val hasSubmitted = state.uploadedFiles.any { it.typeId == docType.id }
                        UploadTypeCard(
                            docType = docType,
                            hasSubmitted = hasSubmitted,
                            onClick = { initiateUpload(docType) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Documentos Submetidos (${state.uploadedFiles.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenSas
                        )
                        Text(
                            text = "(Clique num documento para visualizar)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }

                    if (state.uploadedFiles.isEmpty()) {
                        item {
                            Text("Ainda não submeteu documentos.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        items(state.uploadedFiles) { file ->
                            UploadedFileRow(
                                file = file,
                                onPreviewClick = { onPreviewRequest(file.storagePath) }
                            )
                        }
                    }
                }
            }

            // --- OVERLAY DE CARREGAMENTO (Tela Esbranquiçada) ---
            if (state.uploadProgress || state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f)) // Fundo branco transparente
                        .clickable(enabled = false) {}, // Bloqueia cliques por baixo
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GreenSas)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.uploadProgress) "A carregar documento..." else "A finalizar...",
                            color = GreenSas,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dialog Descrição
        if (showOtherDescriptionDialog) {
            AlertDialog(
                onDismissRequest = { /* Não fecha ao clicar fora para evitar erros */ },
                title = { Text("Descrição do Documento") },
                text = {
                    Column {
                        Text("Escreva uma pequena descrição sobre este documento:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherDescriptionText,
                            onValueChange = { otherDescriptionText = it },
                            label = { Text("Descrição") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (otherDescriptionText.isNotBlank()) {
                                showOtherDescriptionDialog = false
                                pendingDocType?.let {
                                    onUploadRequest(it, otherDescriptionText)
                                }
                                otherDescriptionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Selecionar Ficheiro")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOtherDescriptionDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// ... (Resto dos componentes auxiliares UploadTypeCard, UploadedFileRow mantêm-se iguais) ...
@Composable
fun UploadTypeCard(
    docType: DocumentItem,
    hasSubmitted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (hasSubmitted) GreenSas else Color.LightGray

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(docType.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    if (docType.isMandatory) Text(" *", color = Color.Red, fontWeight = FontWeight.Bold)
                }
                Text(docType.description, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }
            Icon(Icons.Default.AddCircle, contentDescription = "Upload", tint = GreenSas, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun UploadedFileRow(
    file: UploadedFile,
    onPreviewClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(file.date))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onPreviewClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!file.customDescription.isNullOrEmpty()) file.customDescription else file.typeTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSas
                )
                Text(file.fileName, fontSize = 12.sp, color = Color.Black, maxLines = 1)
                Text(dateString, fontSize = 10.sp, color = Color.Gray)
            }

            IconButton(onClick = { onPreviewClick() }) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Visualizar",
                    tint = GreenSas,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Document Submission Preview")
@Composable
fun DocumentSubmissionPreview() {
    val fakeState = SubmissionState(
        docTypes = listOf(
            DocumentItem("despesas", "Despesas Permanentes", "Recibos de habitação, etc.", true),
            DocumentItem("outros", "Outros Documentos", "Outros comprovativos.", false)
        ),
        uploadedFiles = listOf(),
        uploadProgress = true // Testa o loading
    )

    DocumentSubmissionContent(
        state = fakeState,
        onUploadRequest = { _, _ -> },
        onPreviewRequest = {},
        onFinishRequest = {},
        checkMandatory = { true }
    )
}