package ipca.app.lojasas.ui.funcionario.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 1. IMPORT NECESSÁRIO PARA O ERRO DO innerTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.login.GreenIPCA

@Composable
fun ProfileView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = viewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()

    // Estado para controlar o Pop-up de confirmação
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "O meu Perfil",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            val footerType = if (state.role == UserRole.APOIADO) FooterType.APOIADO else FooterType.FUNCIONARIO
            Footer(navController = navController, type = footerType)
        },
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

                // --- IDENTIFICAÇÃO (Apenas Leitura) ---
                FormSection(title = "Identificação") {
                    ReadOnlyInput(
                        value = state.numMecanografico,
                        placeholder = "Nº Mecanográfico"
                    )
                }

                // --- NOME (Editável) ---
                FormSection(title = "Nome") {
                    FormInput(
                        value = state.nome,
                        onValueChange = { viewModel.onNomeChange(it) },
                        placeholder = "Nome"
                    )
                }

                // --- CONTACTO (Editável) ---
                FormSection(title = "Contacto") {
                    FormInput(
                        value = state.contacto,
                        onValueChange = { viewModel.onContactoChange(it) },
                        placeholder = "Contacto",
                        keyboardType = KeyboardType.Phone
                    )
                }

                // --- EMAIL (Apenas Leitura) ---
                FormSection(title = "Email") {
                    ReadOnlyInput(
                        value = state.email,
                        placeholder = "Email"
                    )
                }

                // --- NIF (Apenas Leitura) ---
                FormSection(title = "NIF") {
                    ReadOnlyInput(
                        value = state.nif,
                        placeholder = "NIF"
                    )
                }

                // --- MORADA E CÓDIGO POSTAL (Editável) ---
                FormSection(title = "Morada") {
                    FormInput(
                        value = state.morada,
                        onValueChange = { viewModel.onMoradaChange(it) },
                        placeholder = "Morada"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FormInput(
                        value = state.codPostal,
                        onValueChange = { viewModel.onCodPostalChange(it) },
                        placeholder = "Código Postal"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- BOTÃO APAGAR CONTA ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteDialog = true } // Abre o Dialog
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apagar Conta",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Espaço extra para o FAB
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // --- DIALOG DE CONFIRMAÇÃO ---
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = "Apagar Conta") },
                text = { Text("Tem a certeza que deseja apagar a sua conta? Esta ação é irreversível.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteAccount {
                                // Navega para o Login e limpa tudo da pilha
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
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

// Componente auxiliar para campos de leitura (cinzentos e não editáveis)
@Composable
fun ReadOnlyInput(value: String, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = {},
        enabled = false, // Desativa a edição
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.Gray),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)) // Fundo mais escuro para indicar ReadOnly
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
                }
                // Agora isto já deve funcionar porque importamos o BasicTextField
                innerTextField()
            }
        }
    )
}