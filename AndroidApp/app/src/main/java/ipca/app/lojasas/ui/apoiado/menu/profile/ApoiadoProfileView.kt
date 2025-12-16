package ipca.app.lojasas.ui.apoiado.menu.profile


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.funcionario.menu.profile.* // Importa componentes reutilizáveis (FormInput, etc)
import ipca.app.lojasas.ui.login.GreenIPCA

@Composable
fun ApoiadoProfileView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = viewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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
                    containerColor = GreenIPCA,
                    contentColor = Color.White,
                    shape = CircleShape,
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
                CircularProgressIndicator(color = GreenIPCA)
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

                // --- IDENTIFICAÇÃO ---
                FormSection(title = "Identificação") {
                    ReadOnlyInput(value = state.numMecanografico, placeholder = "Nº Mecanográfico")
                }

                // --- DADOS PESSOAIS ---
                FormSection(title = "Dados Pessoais") {
                    FormInput(value = state.nome, onValueChange = { viewModel.onNomeChange(it) }, placeholder = "Nome")
                    Spacer(modifier = Modifier.height(8.dp))

                    // NIF/Passaporte (Apenas Leitura aqui, pois é chave)
                    val docLabel = if(state.documentType == "Passaporte") "Passaporte" else "NIF"
                    ReadOnlyInput(value = state.documentNumber, placeholder = docLabel)
                }

                // --- CONTACTOS ---
                FormSection(title = "Contactos") {
                    FormInput(value = state.contacto, onValueChange = { viewModel.onContactoChange(it) }, placeholder = "Telemóvel", keyboardType = KeyboardType.Phone)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReadOnlyInput(value = state.email, placeholder = "Email")
                }

                // --- MORADA ---
                FormSection(title = "Morada") {
                    FormInput(value = state.morada, onValueChange = { viewModel.onMoradaChange(it) }, placeholder = "Rua / Localidade")
                    Spacer(modifier = Modifier.height(8.dp))
                    FormInput(value = state.codPostal, onValueChange = { viewModel.onCodPostalChange(it) }, placeholder = "Código Postal")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- OPÇÕES DE CONTA ---
                ProfileOptionCard(text = "Alterar Senha", textColor = Color.Black) { showPasswordDialog = true }
                ProfileOptionCard(text = "Apagar Conta", textColor = Color.Red) { showDeleteDialog = true }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // --- DIALOGS (Reutilizados do pacote profile) ---
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
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Apagar Conta") },
                text = { Text("Tem a certeza? Esta ação é irreversível.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount { navController.navigate("login") { popUpTo(0) } }
                    }) { Text("Apagar", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun ProfileOptionCard(text: String, textColor: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}