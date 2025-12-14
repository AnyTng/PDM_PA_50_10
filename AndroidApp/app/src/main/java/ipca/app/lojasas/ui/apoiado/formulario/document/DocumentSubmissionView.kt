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
import androidx.compose.material.icons.filled.CheckCircle
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
// Este componente não pode ter @Preview porque usa ViewModel()
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

    // Função de lógica para abrir o ficheiro
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

    // Chama o conteúdo visual passando apenas os dados e funções
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
                // CHAMA A NOVA FUNÇÃO PARA ATUALIZAR A BD
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

// --- 2. CONTEÚDO VISUAL (Sem Lógica de Firebase) ---
// Este componente é que é visualizado no Preview
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
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Concluir e Voltar")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
                .padding(horizontal = 16.dp)
        ) {

            if (state.uploadProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = GreenSas
                )
            }
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

        if (showOtherDescriptionDialog) {
            AlertDialog(
                onDismissRequest = { showOtherDescriptionDialog = false },
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

// --- COMPONENTES AUXILIARES ---

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
            .clickable { onPreviewClick() }, // Clique no cartão todo
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

// --- 3. PREVIEW ---
@Preview(showBackground = true, name = "Document Submission Preview")
@Composable
fun DocumentSubmissionPreview() {
    // Dados falsos para teste visual
    val fakeState = SubmissionState(
        docTypes = listOf(
            DocumentItem("despesas", "Despesas Permanentes", "Recibos de habitação, etc.", true),
            DocumentItem("outros", "Outros Documentos", "Outros comprovativos.", false)
        ),
        uploadedFiles = listOf(
            UploadedFile("1", "despesas", "Despesas Permanentes", "recibo_jan.pdf", "", System.currentTimeMillis(), null),
            UploadedFile("2", "outros", "Outros Documentos", "declaracao.jpg", "", System.currentTimeMillis(), "Declaração de Junta")
        )
    )

    // Agora chamamos o Componente Visual (Content) e não a View com lógica
    DocumentSubmissionContent(
        state = fakeState,
        onUploadRequest = { _, _ -> },
        onPreviewRequest = {},
        onFinishRequest = {},
        checkMandatory = { true }
    )
}