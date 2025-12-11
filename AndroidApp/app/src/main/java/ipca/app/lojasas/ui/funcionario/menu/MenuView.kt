package ipca.app.lojasas.ui.funcionario.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType

@Composable
fun MenuView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Cor de fundo cinza claro igual à imagem
    val backgroundColor = Color(0xFFF2F2F2)

    Scaffold(
        topBar = {
            AppHeader(title = "Menu")
        },
        bottomBar = {
            Footer(
                navController = navController,
                type = FooterType.FUNCIONARIO
            )
        },
        containerColor = backgroundColor // Define a cor de fundo do Scaffold
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp), // Margens laterais
            verticalArrangement = Arrangement.spacedBy(24.dp) // Espaço entre o grupo principal e o botão sair
        ) {

            // --- GRUPO DE OPÇÕES PRINCIPAIS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuRow(title = "Gerir o meu Perfil") { /* Navegar */ }
                    MenuDivider()
                    MenuRow(title = "Criar novo Perfil") { /* Navegar */ }
                    MenuDivider()
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {

                    MenuRow(title = "Histórico de ações") { /* Navegar */ }
                    MenuDivider()
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuRow(title = "Apoiados") { /* Navegar */ }
                    MenuDivider()
                    MenuRow(title = "Pedidos de Apoio") { /* Navegar */ }
                    MenuDivider()
                    MenuRow(title = "Campanhas") { /* Navegar */ }
                }
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

// Componente para uma linha do menu (Texto + Seta)
@Composable
fun MenuRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir",
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Componente para a linha divisória
@Composable
fun MenuDivider() {
    HorizontalDivider(
        color = Color(0xFFE0E0E0),
        thickness = 1.dp
    )
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Menu Completo Preview")
@Composable
fun MenuViewPreview() {
    val navController = rememberNavController()
    MenuView(navController = navController)
}