package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.theme.GreenSas // Ou use Color(0xFF094E33) diretamente se não estiver no theme

@Composable
fun CalendarView(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = Color(0xFF094E33))
        } else {
            Text(text = "Conteúdo do Calendário Aqui")
            // A tua lógica de calendário entra aqui...
        }

        // --- POP-UP OBRIGATÓRIO ---
        if (state.showMandatoryPasswordChange) {
            MandatoryPasswordChangeDialog(
                isLoading = state.isLoading,
                errorMessage = state.error,
                onConfirm = { old, new ->
                    viewModel.changePassword(old, new) {
                        // Ação opcional ao concluir com sucesso (ex: Toast)
                    }
                }
            )
        }
    }
}

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

    // onDismissRequest vazio impede fechar clicando fora ou no botão "back"
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "Atualização de Segurança",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF094E33)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("É obrigatório alterar a sua palavra-passe no primeiro acesso ou após uma redefinição.")

                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                }
                if (localError != null) {
                    Text(localError!!, color = Color.Red, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Senha Atual") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("Nova Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Repetir Nova Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localError = null
                    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                        localError = "Preencha todos os campos."
                    } else if (newPass != confirmPass) {
                        localError = "As novas senhas não coincidem."
                    } else if (newPass.length < 6) {
                        localError = "A nova senha deve ter pelo menos 6 caracteres."
                    } else {
                        onConfirm(oldPass, newPass)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF094E33)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                Text("Alterar Senha")
            }
        },
        dismissButton = null, // Sem botão de cancelar
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    )
}