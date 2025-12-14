// Ficheiro: lojasas/ui/apoiado/ApoiadoHomeScreen.kt

package ipca.app.lojasas.ui.apoiado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme

@Composable
fun ApoiadoHomeScreen(
    navController: NavController,
    viewModel: ApoiadoViewModel = viewModel()
) {
    val state by viewModel.uiState

    // Recarrega o estado sempre que o ecrã ganha foco (caso volte de trás)
    LaunchedEffect(Unit) {
        viewModel.checkStatus()
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF094E33))
        }
        return
    }

    // --- PRIORIDADE DE NAVEGAÇÃO / BLOQUEIO ---

    // 1. Mudar Password (Prioridade Máxima)
    if (state.showMandatoryPasswordChange) {
        MandatoryPasswordChangeDialog(
            isLoading = state.isLoading,
            errorMessage = state.error,
            onConfirm = { old, new ->
                viewModel.changePassword(old, new) { viewModel.checkStatus() }
            }
        )
        return // Retorna para não desenhar o resto
    }

    // 2. Dados Incompletos
    else if (state.dadosIncompletos) {
        CompleteDataView(
            docId = state.docId,
            onSuccess = { viewModel.checkStatus() },
            navController = navController
        )
        return
    }

    // 3. Falta Documentos (Obrigatório)
    else if (state.faltaDocumentos) {
        // Redireciona automaticamente
        LaunchedEffect(Unit) {
            navController.navigate("documentSubmission") {
                // Remove a Home da pilha para que o botão "Voltar" do telemóvel saia da app ou vá para login,
                // impedindo que ele volte para a Home sem documentos.
                popUpTo("apoiadoHome") { inclusive = true }
            }
        }
        // Mostra um loading enquanto navega
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF094E33))
        }
        return
    }

    // --- CONTEÚDO NORMAL DA HOME ---
    // (Só chega aqui se tudo estiver false)
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Bem-vindo, Apoiado!\n(Situação Regularizada)", style = MaterialTheme.typography.headlineMedium)
            }

            // Botão Logout (código existente...)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) {}
                        navController.navigate("login") { popUpTo(0) }
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Terminar Sessão", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// Componente do Diálogo (Pode ser copiado aqui ou movido para components/)
@Composable
fun MandatoryPasswordChangeDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: (String, String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { }, // Bloqueia fechar
        title = { Text("Atualização de Segurança", fontWeight = FontWeight.Bold, color = Color(0xFF094E33)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("É obrigatório alterar a sua palavra-passe.")
                if (errorMessage != null) Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                if (localError != null) Text(localError!!, color = Color.Red, fontSize = 12.sp)

                OutlinedTextField(
                    value = oldPass, onValueChange = { oldPass = it },
                    label = { Text("Senha Atual") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true
                )
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it },
                    label = { Text("Nova Senha") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true
                )
                OutlinedTextField(
                    value = confirmPass, onValueChange = { confirmPass = it },
                    label = { Text("Repetir Nova Senha") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localError = null
                    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) localError = "Preencha todos os campos."
                    else if (newPass != confirmPass) localError = "As novas senhas não coincidem."
                    else if (newPass.length < 6) localError = "Mínimo 6 caracteres."
                    else onConfirm(oldPass, newPass)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF094E33)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("Alterar")
            }
        },
        dismissButton = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    )
}