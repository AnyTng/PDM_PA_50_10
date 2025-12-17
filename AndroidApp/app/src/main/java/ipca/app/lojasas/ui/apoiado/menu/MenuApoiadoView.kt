package ipca.app.lojasas.ui.apoiado.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Import necessário
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Import necessário
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MenuApoiadoView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // 1. Instanciar o ViewModel
    val viewModel: MenuApoiadoViewModel = viewModel()
    val isApproved by viewModel.isApproved // Estado: true se estiver aprovado

    val backgroundColor = Color(0xFFF2F2F2)

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

            // --- GRUPO: CONTA ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuApoiadoRow(title = "O meu Perfil") { navController.navigate("profileApoiado") }
                    MenuApoiadoDivider()
                }
            }

            // --- GRUPO: AÇÃO SOCIAL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuApoiadoRow(title = "Meus Pedidos") { /* Navegar */ }
                    MenuApoiadoDivider()

                    // 2. LÓGICA DO BOTÃO DESATIVADO
                    // Se estiver aprovado (isApproved == true), o enabled será false
                    MenuApoiadoRow(
                        title = "Entregar Documentos",
                        enabled = !isApproved,
                        onClick = { navController.navigate("documentSubmission") }
                    )

                    MenuApoiadoDivider()
                    MenuApoiadoRow(title = "Documentos Entregues") { navController.navigate("submittedDocuments") }
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

// 3. ATUALIZAÇÃO DO COMPONENTE MenuApoiadoRow
@Composable
fun MenuApoiadoRow(
    title: String,
    enabled: Boolean = true, // Novo parâmetro (padrão é true/ativo)
    onClick: () -> Unit
) {
    // Definir as cores baseadas no estado
    val contentColor = if (enabled) Color.Black else Color.Gray.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick) // Só clica se enabled = true
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = contentColor, // Aplica a cor (Preto ou Cinza)
            fontWeight = FontWeight.Normal
        )

        // Se estiver desativado, removemos a seta ou pintamos de cinza
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ir",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MenuApoiadoDivider() {
    HorizontalDivider(
        color = Color(0xFFE0E0E0),
        thickness = 1.dp
    )
}