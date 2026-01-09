package ipca.app.lojasas.ui.apoiado.menu.document

import ipca.app.lojasas.ui.theme.*
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.apoiado.SubmittedFile
import ipca.app.lojasas.ui.components.AppHeader
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun SubmittedDocumentsView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: SubmittedDocumentsViewModel = hiltViewModel()
    val state by viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppHeader(
                title = "Histórico Documentos",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GreyBg)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenSas
                )
            } else if (state.groupedDocuments.isEmpty()) {
                Text(
                    text = "Ainda não submeteu documentos.",
                    color = GreyColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.groupedDocuments.forEach { (entregaNum, files) ->
                        // --- CABEÇALHO DA ENTREGA ---
                        item {
                            Text(
                                text = "Entrega nº $entregaNum",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSas,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            HorizontalDivider(color = DividerGreenLight, thickness = 1.dp)
                        }

                        // --- LISTA DE FICHEIROS DA ENTREGA ---
                        items(files) { file ->
                            SubmittedFileCard(
                                file = file,
                                onClick = {
                                    viewModel.getFileUri(file.storagePath) { uri ->
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Erro ao abrir aplicação externa.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Erro ao obter ficheiro.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = RedColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SubmittedFileCard(
    file: SubmittedFile,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = GreenSas,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BlackColor
                )
                Text(
                    text = file.fileName,
                    fontSize = 12.sp,
                    color = GreyColor
                )
                Text(
                    text = dateFormat.format(file.date),
                    fontSize = 12.sp,
                    color = GreyColor
                )
            }

            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Ver",
                tint = GreyColor
            )
        }
    }
}
