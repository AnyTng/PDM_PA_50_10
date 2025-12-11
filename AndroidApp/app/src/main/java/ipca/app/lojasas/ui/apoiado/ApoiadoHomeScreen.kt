package ipca.app.lojasas.ui.apoiado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme
import androidx.navigation.NavController

@Composable
fun ApoiadoHomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Home",
                showBack = false,
                onBack = null
            )
        },
        bottomBar = {
            Footer(
                navController = navController,
                type = FooterType.APOIADO
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp) // Adicionei margem lateral para o botão não colar às bordas
        ) {

            // --- CONTEÚDO PRINCIPAL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Ocupa a largura toda
                    .weight(1f),    // <--- MUDANÇA AQUI: Ocupa o espaço vertical restante
                contentAlignment = Alignment.Center
            ) {
                Text(text = "hello, Apoiado")
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

@Preview(showBackground = true)
@Composable
private fun ApoiadoPreview() {
    LojaSocialIPCATheme {
        ApoiadoHomeScreen(
            navController = NavController(LocalContext.current)
        )
    }
}
