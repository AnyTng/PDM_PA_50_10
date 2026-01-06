package ipca.app.lojasas.ui.apoiado.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import ipca.app.lojasas.core.navigation.Screen

@Composable
fun MenuApoiadoView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: MenuApoiadoViewModel = viewModel()
    val isApproved by viewModel.isApproved
    val isBlock by viewModel.isBlock

    // 1. Ler o número mecanográfico do ViewModel
    val numeroMecanografico by viewModel.numeroMecanografico

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
                    MenuApoiadoRow(title = "O meu Perfil") { navController.navigate(Screen.ProfileApoiado.route) }
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
                    // 2. Usar a variável lida aqui.
                    // Nota: Verificamos se não está vazia para evitar erro de navegação
                    MenuApoiadoRow(title = "Fazer Pedido de Ajuda Urgente",enabled = !isBlock) {
                        if (numeroMecanografico.isNotEmpty()) {
                            navController.navigate(Screen.UrgentHelp.createRoute(numeroMecanografico))
                        }
                    }

                    MenuApoiadoDivider()

                    MenuApoiadoRow(
                        title = "Entregar Documentos",
                        enabled = !isApproved && !isBlock,
                        onClick = { navController.navigate(Screen.DocumentSubmission.route) }
                    )

                    MenuApoiadoDivider()
                    MenuApoiadoRow(title = "Documentos Entregues",enabled = !isBlock) { navController.navigate(Screen.SubmittedDocuments.route) }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuApoiadoRow(title = "Manual de Utilização do Beneficiario") {
                        navController.navigate(Screen.BeneficiarioManual.route)
                    }
                    MenuApoiadoDivider()
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
                        navController.navigate(Screen.Login.route) {
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

// ... (Resto do código MenuApoiadoRow e MenuApoiadoDivider mantém-se igual)
@Composable
fun MenuApoiadoRow(
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) Color.Black else Color.Gray.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = contentColor,
            fontWeight = FontWeight.Normal
        )

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
