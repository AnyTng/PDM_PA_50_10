package ipca.app.lojasas.ui.apoiado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView
import ipca.app.lojasas.ui.funcionario.calendar.MandatoryPasswordChangeDialog


// Importe o ficheiro de temas se necessário ou defina as cores aqui
val TextGreen = Color(0xFF094E33)
val WarningOrange = Color(0xFFD88C28) // Cor aproximada da imagem (Laranja/Ocre)
val CardDarkBlue = Color(0xFF0F4C5C)  // Cor aproximada do cartão azul

@Composable
fun ApoiadoHomeScreen(
    navController: NavController,
    viewModel: ApoiadoViewModel = viewModel()
) {
    val state by viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.checkStatus()
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextGreen)
        }
        return
    }

    // 1. Mudar Password (Prioridade Máxima - Mantém-se Bloqueante)
    if (state.showMandatoryPasswordChange) {
        MandatoryPasswordChangeDialog(
            isLoading = state.isLoading,
            errorMessage = state.error,
            onConfirm = { old, new -> viewModel.changePassword(old, new) { viewModel.checkStatus() } }
        )
        return
    }

    // 2. Dados Incompletos (Formulário Inicial - Mantém-se Bloqueante)
    else if (state.dadosIncompletos) {
        CompleteDataView(
            docId = state.docId,
            onSuccess = { viewModel.checkStatus() },
            navController = navController
        )
        return
    }

    // 3. DASHBOARD PRINCIPAL (Já não bloqueia por falta de documentos)
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- HEADER: Olá (Nome) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Olá, ${state.nome}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGreen
                    )
                    // Estado do perfil
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Estado do Perfil: ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (state.faltaDocumentos) {
                            Text(
                                text = "Faltam Documentos ⓘ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        } else {
                            Text(
                                text = "Regularizado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGreen
                            )
                        }
                    }
                }

                // Botão "Ver Perfil"
                Button(
                    onClick = { /* Navegar para Perfil */ },
                    colors = ButtonDefaults.buttonColors(containerColor = TextGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Ver perfil", fontSize = 12.sp)
                }
            }

            // --- DIVISOR: A Acontecer Agora ---
            SectionDivider(text = "A Acontecer Agora")

            // --- CARTÃO DE AVISO (Só aparece se faltarem documentos) ---
            if (state.faltaDocumentos) {
                ActionCard(
                    title = "Documentos em Falta",
                    subtitle = "Submeta os comprovativos para validar o seu apoio.",
                    buttonText = "Enviar",
                    backgroundColor = WarningOrange,
                    onClick = { navController.navigate("documentSubmission") }
                )
            } else {
                // Caso não falte documentos, mostra algo positivo ou vazio
                ActionCard(
                    title = "Tudo em dia!",
                    subtitle = "O seu processo está em análise.",
                    buttonText = "Detalhes",
                    backgroundColor = TextGreen,
                    onClick = { }
                )
            }

            // --- OUTROS CARTÕES (Ex: Pedido de Ajuda) ---
            ActionCard(
                title = "Pedido de Ajuda Urgente",
                subtitle = "Em Análise",
                buttonText = "Ver Mais",
                backgroundColor = CardDarkBlue,
                onClick = { /* Navegar para detalhes do pedido */ }
            )

            // --- DIVISOR: Anteriormente ---
            SectionDivider(text = "Anteriormente")

            // Espaço extra no fundo para não bater no Footer
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- COMPONENTES VISUAIS AUXILIARES ---

@Composable
fun SectionDivider(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    buttonText: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp) // Altura fixa para ficar igual à imagem
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Títulos no topo esquerdo
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Ícone de fundo ou decoração poderia entrar aqui
            }

            // Subtítulo ou Texto secundário (no lugar de "Nome do Documento" na imagem)
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)
            )

            // Botão Branco Pequeno
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .height(32.dp)
            ) {
                Text(
                    text = buttonText,
                    color = Color.Black, // Texto preto no botão branco
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}