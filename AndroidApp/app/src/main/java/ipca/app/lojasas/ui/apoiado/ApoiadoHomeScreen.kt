package ipca.app.lojasas.ui.apoiado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme

@Composable
fun ApoiadoHomeScreen(
    navController: NavController,
    viewModel: ApoiadoViewModel = viewModel() // Injetar ViewModel
) {
    val state by viewModel.uiState

    // Se estiver a carregar, mostra loading
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF094E33))
        }
        return
    }

    // --- LÓGICA DE DADOS INCOMPLETOS ---
    if (state.dadosIncompletos) {
        // Se faltam dados, mostra o formulário e bloqueia o resto
        CompleteDataView(
            docId = state.docId,
            onSuccess = {
                // Ao terminar com sucesso, recarrega o estado para ir para a Home normal
                viewModel.checkStatus()
            }
        )
    } else {
        // --- HOME NORMAL DO APOIADO ---
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                // --- CONTEÚDO PRINCIPAL ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Bem-vindo, Apoiado! A sua conta está ativa.")
                }

                // --- BOTÃO TERMINAR SESSÃO ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Terminar Sessão",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ApoiadoPreview() {
    LojaSocialIPCATheme {
        ApoiadoHomeScreen(
            navController = NavController(LocalContext.current)
        )
    }
}