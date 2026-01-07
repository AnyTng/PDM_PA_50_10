package ipca.app.lojasas.ui.funcionario.menu.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning // Importante
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.theme.GreenSas


@Composable
fun ProfileView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: FuncionarioProfileViewModel = hiltViewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Agora usamos o estado vindo do ViewModel
    val isAdmin = state.isAdmin

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!state.isLoading) {
                FloatingActionButton(
                    onClick = {
                        viewModel.saveProfile {
                            navController.popBackStack()
                        }
                    },
                    containerColor = GreenSas,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Atualizar",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenSas)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F8F8))
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                if (state.error != null) {
                    Text(text = state.error!!, color = Color.Red, fontSize = 14.sp)
                }

                // --- IDENTIFICAÇÃO (Apenas Leitura) ---
                FormSection(title = "Identificação") {
                    ReadOnlyInput(value = state.numMecanografico, placeholder = "Nº Mecanográfico")
                }

                // --- NOME (Editável) ---
                FormSection(title = "Nome") {
                    FormInput(value = state.nome, onValueChange = { viewModel.onNomeChange(it) }, placeholder = "Nome")
                }

                // --- CONTACTO (Editável) ---
                FormSection(title = "Contacto") {
                    FormInput(value = state.contacto, onValueChange = { viewModel.onContactoChange(it) }, placeholder = "Contacto", keyboardType = KeyboardType.Phone)
                }

                // --- EMAIL (Apenas Leitura) ---
                FormSection(title = "Email") {
                    ReadOnlyInput(value = state.email, placeholder = "Email")
                }

                // --- DOCUMENTO (Dinâmico: NIF ou Passaporte) ---
                val documentLabel = if (state.documentType == "Passaporte") "Passaporte" else "NIF"
                FormSection(title = documentLabel) {
                    ReadOnlyInput(value = state.documentNumber, placeholder = documentLabel)
                }

                // --- MORADA E CÓDIGO POSTAL (Editável) ---
                FormSection(title = "Morada") {
                    FormInput(value = state.morada, onValueChange = { viewModel.onMoradaChange(it) }, placeholder = "Morada")
                    Spacer(modifier = Modifier.height(8.dp))
                    FormInput(value = state.codPostal, onValueChange = { viewModel.onCodPostalChange(it) }, placeholder = "Código Postal")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- BOTÕES DE AÇÃO ---
                ProfileOptionCard(
                    text = "Trocar Senha",
                    textColor = Color.Black,
                    onClick = { showPasswordDialog = true }
                )

                ProfileOptionCard(
                    text = "Apagar Conta",
                    textColor = Color.Red,
                    onClick = { showDeleteDialog = true }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // --- DIALOGS ---
        if (showPasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showPasswordDialog = false },
                onConfirm = { old, new ->
                    viewModel.changePassword(
                        oldPass = old,
                        newPass = new,
                        onSuccess = {
                            showPasswordDialog = false
                            Toast.makeText(context, "Senha alterada com sucesso!", Toast.LENGTH_LONG).show()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (showDeleteDialog) {
            if (isAdmin) {
                // --- DIÁLOGO PARA ADMIN (Apenas Aviso) ---
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000)) },
                    title = { Text(text = "Ação Não Permitida", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Por motivos de segurança um administrador não pode apagar a própria conta. " +
                                    "Para isso deve procurar outro administrador para o fazer ou para o despromover para que o possa fazer. " +
                                    "Caso não haja um outro administrador, deve promover um colaborador ao cargo.",
                            textAlign = TextAlign.Justify
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showDeleteDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                        ) {
                            Text("Entendi")
                        }
                    },
                    containerColor = Color.White
                )
            } else {
                // --- DIÁLOGO PARA COLABORADOR/APOIADO (Confirmação de Apagar) ---
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = "Apagar Conta") },
                    text = { Text("Tem a certeza que deseja apagar a sua conta? Esta ação é irreversível.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteAccount {
                                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Apagar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                        ) {
                            Text("Cancelar")
                        }
                    },
                    containerColor = Color.White
                )
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun ProfileOptionCard(
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar Senha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = Color.Red, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Senha Antiga") },
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
                    if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                        error = "Preencha todos os campos."
                    } else if (newPass != confirmPass) {
                        error = "As novas senhas não coincidem."
                    } else if (newPass.length < 6) {
                        error = "A nova senha deve ter pelo menos 6 caracteres."
                    } else {
                        onConfirm(oldPass, newPass)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
            ) {
                Text("Alterar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Black)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun ReadOnlyInput(value: String, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = {},
        enabled = false,
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Gray),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
                }
                innerTextField()
            }
        }
    )
}

@Preview
@Composable
fun ProfilePreview() {
    ProfileView(navController = rememberNavController())
}
