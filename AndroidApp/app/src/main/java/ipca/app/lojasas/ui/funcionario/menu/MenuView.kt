package ipca.app.lojasas.ui.funcionario.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MenuView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Cor de fundo cinza claro igual à imagem
    val backgroundColor = Color(0xFFF2F2F2)

    Scaffold(
        containerColor = backgroundColor, // Define a cor de fundo do Scaffold
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- GRUPO DE OPÇÕES PRINCIPAIS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuRow(title = "Gerir o meu Perfil") { navController.navigate("profileFuncionario") }
                    MenuDivider()
                    MenuRow(title = "Criar novo Colaborador") { navController.navigate("createProfile")}
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
                    MenuRow(title = "Apoiados") { navController.navigate("apoiadosList") }
                    MenuDivider()
                    MenuRow(title = "Pedidos Urgentes") { /* Navegar */ }
                    MenuDivider()
                    MenuRow(title = "Validar Contas") { navController.navigate("validateAccounts") }
                    MenuDivider()
                    MenuRow(title = "Ver Campanhas") { navController.navigate("campaigns") }
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
                        // --- AQUI ESTÁ A CORREÇÃO ---

                        // 1. Limpa a autenticação no Firebase
                        try {
                            FirebaseAuth.getInstance().signOut()
                        } catch (e: Exception) {
                            // Caso não estejas a usar Firebase ou dê erro, o código continua
                            e.printStackTrace()
                        }

                        // 2. Navega para o Login e limpa o histórico para trás
                        navController.navigate("login") {
                            popUpTo(0) // Garante que a pilha de navegação é limpa
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