package ipca.app.lojasas.ui.funcionario.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MenuView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFFF2F2F2)

    // Estado para saber se é Admin
    var isAdmin by remember { mutableStateOf(false) }

    // Verificar permissões ao iniciar
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("funcionarios")
                .document(user.uid) // Assume-se que o ID do doc é o UID ou faz-se query
                .get() // Nota: No teu CreateProfile usas numMecanografico como ID, precisamos procurar pelo UID
                .addOnSuccessListener {
                    // Como o ID do documento é o NumMecanografico, temos de fazer query pelo campo uid
                }

            // Query segura pelo UID
            FirebaseFirestore.getInstance().collection("funcionarios")
                .whereEqualTo("uid", user.uid)
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val role = docs.documents[0].getString("role")
                        isAdmin = role.equals("Admin", ignoreCase = true)
                    }
                }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
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

                    // APENAS ADMIN VÊ ISTO
                    if (isAdmin) {
                        MenuDivider()
                        MenuRow(title = "Criar novo Colaborador") { navController.navigate("createProfile") }
                        MenuDivider()
                        // Nova rota para ver colaboradores
                        MenuRow(title = "Ver Colaboradores") { navController.navigate("collaboratorsList") }
                    }
                    MenuDivider()
                }
            }
            if (isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MenuRow(title = "Historico") { /* Navegar */ }
                        MenuDivider()

                    }
                }
            }
            // ... (Resto do código mantém-se igual: Histórico, Apoiados, Logout) ...
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
                    MenuRow(title = "Validar Beneficiario") { navController.navigate("validateAccounts") }
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
                        try {
                            FirebaseAuth.getInstance().signOut()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
/*
// --- PREVIEWS ---
@Preview
@Composable
fun MenuViewPreview() {
    MenuView(navController = NavController(LocalContext.current))
}*/