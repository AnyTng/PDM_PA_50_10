package ipca.app.lojasas.ui.apoiado.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView
import ipca.app.lojasas.ui.funcionario.calendar.MandatoryPasswordChangeDialog


// Cores...
val TextGreen = Color(0xFF094E33)
val WarningOrange = Color(0xFFD88C28)
val CardDarkBlue = Color(0xFF0F4C5C)
val RedError = Color(0xFFB00020)

@Composable
fun ApoiadoHomeScreen(
    navController: NavController,
    viewModel: ApoiadoViewModel = viewModel()
) {
    val state by viewModel.uiState

    // 1. Verificar estado ao iniciar
    LaunchedEffect(Unit) {
        viewModel.checkStatus()
    }

    // 2. Navegação automática baseada no estado da conta
    LaunchedEffect(state.estadoConta, state.faltaDocumentos, state.dadosIncompletos, state.docId) {

        // --- ALTERAÇÃO AQUI: Se dados incompletos, NAVEGAR para a nova rota ---
        if (state.dadosIncompletos && state.docId.isNotEmpty()) {
            navController.navigate("completeData/${state.docId}")
            // Não fazemos popUpTo aqui para permitir que o utilizador "veja" a transição ou por lógica de navegação
        }

        if (state.estadoConta == "Bloqueado") {
            navController.navigate("accountBlocked") {
                popUpTo("apoiadoHome") { inclusive = true }
            }
        }

        if (state.estadoConta == "Falta_Documentos" && state.faltaDocumentos) {
            navController.navigate("documentSubmission")
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextGreen)
        }
        return
    }

    if (state.showMandatoryPasswordChange) {
        MandatoryPasswordChangeDialog(
            isLoading = state.isLoading,
            errorMessage = state.error,
            onConfirm = { old, new -> viewModel.changePassword(old, new) { viewModel.checkStatus() } }
        )
        return
    }

    // --- REMOVIDO O BLOCO QUE CHAMAVA CompleteDataView AQUI ---
    // (A navegação acima trata disso agora)

    // 4. DASHBOARD NORMAL
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HeaderSection(state.nome, state.estadoConta, state.faltaDocumentos, navController)
            SectionDivider(text = "Estado da Conta")

            // ... (Resto do código do when(state.estadoConta) mantém-se igual) ...
            when (state.estadoConta) {
                "Aprovado" -> {
                    ActionCard(
                        title = "Conta Aprovada",
                        subtitle = "Válida até: ${state.validadeConta ?: "N/A"}",
                        buttonText = "Ver Cartão",
                        backgroundColor = TextGreen,
                        onClick = { }
                    )
                }
                "Negado" -> {
                    DeniedCard(
                        reason = state.motivoNegacao ?: "Sem motivo especificado.",
                        onTryAgain = {
                            viewModel.resetToTryAgain {
                                // O reset define 'dadosIncompletos=true',
                                // O LaunchedEffect em cima vai detetar isso e navegar para o formulário.
                            }
                        }
                    )
                }
                "Em_Analise", "Analise" -> {
                    ActionCard(
                        title = "Em Análise",
                        subtitle = "O seu processo está a ser avaliado.",
                        buttonText = "Detalhes",
                        backgroundColor = CardDarkBlue,
                        onClick = { }
                    )
                }
                else -> {
                    if (state.faltaDocumentos) {
                        ActionCard(
                            title = "Documentos em Falta",
                            subtitle = "Submeta os comprovativos.",
                            buttonText = "Enviar",
                            backgroundColor = WarningOrange,
                            onClick = { navController.navigate("documentSubmission") }
                        )
                    } else {
                        ActionCard(
                            title = "Bem-vindo",
                            subtitle = "Aguarde atualização de estado.",
                            buttonText = "Info",
                            backgroundColor = Color.Gray,
                            onClick = {}
                        )
                    }
                }
            }

            if (state.estadoConta == "Aprovado") {
                SectionDivider(text = "Ações Disponíveis")
                ActionCard(
                    title = "Pedido de Ajuda",
                    subtitle = "Solicitar apoio alimentar ou outro.",
                    buttonText = "Solicitar",
                    backgroundColor = CardDarkBlue,
                    onClick = { }
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
// ... (Resto das funções auxiliares BlockedAccountScreen, DeniedCard, etc mantêm-se iguais)

// ... COMPONENTES AUXILIARES ...

@Composable
fun BlockedAccountScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Bloqueado",
            tint = RedError,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Conta Bloqueada", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RedError)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A sua conta encontra-se bloqueada. Por favor, dirija-se aos serviços do SAS do IPCA para resolver a situação.",
            fontSize = 16.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") { popUpTo(0) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Terminar Sessão")
        }
    }
}

@Composable
fun DeniedCard(reason: String, onTryAgain: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RedError),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Pedido Negado", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Motivo: $reason", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Tentar de Novo", color = RedError, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HeaderSection(nome: String, estado: String, faltaDocumentos: Boolean, navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Olá, $nome", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextGreen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Estado: ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                val statusColor = when(estado) {
                    "Aprovado" -> TextGreen
                    "Negado", "Bloqueado" -> RedError
                    "Analise", "Em_Analise" -> CardDarkBlue
                    else -> if (faltaDocumentos) WarningOrange else Color.Gray
                }
                val statusText = when(estado) {
                    "Aprovado" -> "Regularizado"
                    "Negado" -> "Não Aprovado"
                    "Analise", "Em_Analise" -> "Em Análise"
                    else -> if (faltaDocumentos) "Faltam Documentos" else "Pendente"
                }
                Text(text = statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
        Button(
            onClick = { navController.navigate("profileApoiado") },
            colors = ButtonDefaults.buttonColors(containerColor = TextGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Ver perfil", fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
        Text(text = text, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, buttonText: String, backgroundColor: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = subtitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.align(Alignment.BottomEnd).height(32.dp)
            ) {
                Text(text = buttonText, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
            }
        }
    }
}