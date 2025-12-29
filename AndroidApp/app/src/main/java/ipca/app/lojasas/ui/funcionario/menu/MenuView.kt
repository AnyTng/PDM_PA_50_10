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

    MenuViewContent(
        isAdmin = isAdmin,
        backgroundColor = backgroundColor,
        onNavigate = { route -> navController.navigate(route) },
        onLogout = {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            navController.navigate("login") { popUpTo(0) }
        },
        modifier = modifier
    )
}

/**
 * UI "pura" -> usa isto nos @Preview para não bater no Firebase/Firestore.
 */
@Composable
private fun MenuViewContent(
    isAdmin: Boolean,
    backgroundColor: Color,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- GRUPO PERFIL / ADMIN ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuRow(title = "Gerir o meu Perfil") { onNavigate("profileFuncionario") }

                    if (isAdmin) {
                        MenuDivider()
                        MenuRow(title = "Criar novo Colaborador") { onNavigate("createProfile") }
                        MenuDivider()
                        MenuRow(title = "Ver Colaboradores") { onNavigate("collaboratorsList") }
                    }
                    MenuDivider()
                }
            }

            // --- BLOCO EXTRA ADMIN (como tinhas “Historico”) ---
            if (isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MenuRow(title = "Historico") { /* onNavigate("...") */ }
                        MenuDivider()
                    }
                }
            }

            // --- OUTRAS OPÇÕES ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuRow(title = "Apoiados") { onNavigate("apoiadosList") }
                    MenuDivider()
                    MenuRow(title = "Pedidos Urgentes") { /* onNavigate("...") */ }
                    MenuDivider()
                    MenuRow(title = "Validar Beneficiario") { onNavigate("validateAccounts") }
                    MenuDivider()
                    MenuRow(title = "Ver Campanhas") { onNavigate("campaigns") }
                }
            }

            // --- BOTÃO TERMINAR SESSÃO ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
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

// ---------------- PREVIEWS ----------------

@Preview(showBackground = true, name = "Menu - Colaborador")
@Composable
private fun MenuViewPreview_Colaborador() {
    MaterialTheme {
        MenuViewContent(
            isAdmin = false,
            backgroundColor = Color(0xFFF2F2F2),
            onNavigate = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Menu - Admin")
@Composable
private fun MenuViewPreview_Admin() {
    MaterialTheme {
        MenuViewContent(
            isAdmin = true,
            backgroundColor = Color(0xFFF2F2F2),
            onNavigate = {},
            onLogout = {}
        )
    }
}
