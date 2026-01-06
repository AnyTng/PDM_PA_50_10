// Ficheiro: lojasas/ui/apoiado/formulario/document/DocumentSubmissionView.kt

package ipca.app.lojasas.ui.apoiado.menu.document

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val GreenSas = Color(0xFF094E33)

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
        onBackClick = { navController.popBackStack() }, // Ação de voltar sem finalizar
        onUploadRequest = { docType, description ->
            currentDocType = docType
            tempDescription = description
            launcher.launch(arrayOf("application/pdf", "image/*"))
        },
        onPreviewRequest = { path -> openPreview(path) },
        onFinishRequest = {
            if (viewModel.hasAllMandatoryFiles()) {
                viewModel.finalizeSubmission {
                    Toast.makeText(context, "Pedido enviado para validação!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.ApoiadoHome.route) {
                        popUpTo(Screen.DocumentSubmission.route) { inclusive = true }
                    }
                }
            } else {
                Toast.makeText(context, "Por favor, submeta todos os documentos obrigatórios antes de finalizar.", Toast.LENGTH_LONG).show()
            }
        },
        checkMandatory = { viewModel.hasAllMandatoryFiles() }
    )
}

@Composable
fun DocumentSubmissionContent(
    state: SubmissionState,
    onBackClick: () -> Unit,
    onUploadRequest: (DocumentItem, String?) -> Unit,
    onPreviewRequest: (String) -> Unit,
    onFinishRequest: () -> Unit,
    checkMandatory: () -> Boolean
) {
    var showOtherDescriptionDialog by remember { mutableStateOf(false) }
    var otherDescriptionText by remember { mutableStateOf("") }
    var pendingDocType by remember { mutableStateOf<DocumentItem?>(null) }

    fun initiateUpload(docType: DocumentItem) {
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
            // --- HEADER PERSONALIZADO COM SETA E NÚMERO DA ENTREGA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenSas)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Entrega de Documentos",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Entrega nº ${state.currentDeliveryNumber}", // MOSTRA O NÚMERO
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        },
        bottomBar = {
            Button(
                onClick = onFinishRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checkMandatory()) GreenSas else Color.Gray
                ),
                enabled = !state.uploadProgress && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (checkMandatory()) "Finalizar e Enviar para Validação" else "Documentos Obrigatórios em Falta")
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {
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
                    // SECÇÃO: LISTA DE UPLOADS PENDENTES
                    item {
                        Text("Adicionar Documentos:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    items(state.docTypes) { docType ->
                        // Verifica se este tipo de documento JÁ foi enviado nesta entrega
                        val hasSubmitted = state.uploadedFiles.any { it.typeId == docType.id }
                        UploadTypeCard(
                            docType = docType,
                            hasSubmitted = hasSubmitted,
                            onClick = { initiateUpload(docType) }
                        )
                    }

                    // SECÇÃO: LISTA DO QUE JÁ FOI ENTREGUE
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Já entregue nesta fase",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSas
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = GreenSas) {
                                Text("${state.uploadedFiles.size}", color = Color.White)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }

                    if (state.uploadedFiles.isEmpty()) {
                        item {
                            Text("Ainda não adicionou documentos a esta entrega.", color = Color.Gray, fontSize = 14.sp)
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

            // Overlay de Loading
            if (state.uploadProgress || state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenSas)
                }
            }
        }

        // Dialog Descrição (Mantém-se igual)
        if (showOtherDescriptionDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Descrição") },
                text = {
                    OutlinedTextField(
                        value = otherDescriptionText,
                        onValueChange = { otherDescriptionText = it },
                        label = { Text("Nome do documento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (otherDescriptionText.isNotBlank()) {
                                showOtherDescriptionDialog = false
                                pendingDocType?.let { onUploadRequest(it, otherDescriptionText) }
                                otherDescriptionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) { Text("Adicionar") }
                },
                dismissButton = {
                    TextButton(onClick = { showOtherDescriptionDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ... (UploadTypeCard e UploadedFileRow mantêm-se iguais aos anteriores) ...
@Composable
fun UploadTypeCard(
    docType: DocumentItem,
    hasSubmitted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (hasSubmitted) GreenSas else Color.LightGray
    val backgroundColor = if (hasSubmitted) Color(0xFFE8F5E9) else Color.White // Fundo ligeiramente verde se já submetido

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                    if (hasSubmitted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = "OK", tint = GreenSas, modifier = Modifier.size(16.dp))
                    }
                }
                Text(docType.description, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.AddCircle, contentDescription = "Upload", tint = GreenSas, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun UploadedFileRow(file: UploadedFile, onPreviewClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onPreviewClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.customDescription ?: file.typeTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(dateFormat.format(Date(file.date)), fontSize = 10.sp, color = Color.Gray)
            }
            Icon(Icons.Default.Visibility, null, tint = GreenSas)
        }
    }
}
