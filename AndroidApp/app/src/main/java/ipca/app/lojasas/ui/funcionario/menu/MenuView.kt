package ipca.app.lojasas.ui.funcionario.menu

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen

@Composable
fun MenuView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: MenuFuncionarioViewModel = hiltViewModel()
    val isAdmin by viewModel.isAdmin

    MenuViewContent(
        isAdmin = isAdmin,
        GreyBg = GreyBg,
        onNavigate = { route -> navController.navigate(route) },
        onLogout = {
            try {
                viewModel.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            navController.navigate(Screen.Login.route) { popUpTo(0) }
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
    GreyBg: Color,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // --- GRUPO PERFIL / ADMIN ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MenuRow(title = "Gerir o meu Perfil") { onNavigate(Screen.ProfileFuncionario.route) }

                        if (isAdmin) {
                            MenuDivider()
                            MenuRow(title = "Criar novo Colaborador") { onNavigate(Screen.CreateProfile.route) }
                            MenuDivider()
                            MenuRow(title = "Ver Colaboradores") { onNavigate(Screen.CollaboratorsList.route) }
                        }
                        MenuDivider()
                    }
                }
            }

            if (isAdmin) {
                // --- BLOCO EXTRA ADMIN (como tinhas “Historico”) ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WhiteColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            MenuRow(title = "Histórico") { onNavigate(Screen.Historico.route) }
                            MenuDivider()
                        }
                    }
                }
            }

            // --- OUTRAS OPÇÕES ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MenuRow(title = "Beneficiario") { onNavigate(Screen.ApoiadosList.route) }
                        MenuDivider()
                        MenuRow(title = "Pedidos Urgentes") { onNavigate(Screen.UrgentRequests.route) }
                        MenuDivider()
                        MenuRow(title = "Validar Beneficiario") { onNavigate(Screen.ValidateAccounts.route) }
                        MenuDivider()
                        MenuRow(title = "Ver Campanhas") { onNavigate(Screen.Campaigns.route) }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (isAdmin) {
                            MenuRow(title = "Manual de Utilização do Administrador") { onNavigate(Screen.AdminManual.route) }
                            MenuDivider()
                        }
                        if (!isAdmin) {
                            MenuRow(title = "Manual de Utilização do Colaborador") { onNavigate(Screen.AdminManual.route) }
                            MenuDivider()
                        }
                    }
                }
            }

            // --- BOTÃO TERMINAR SESSÃO ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
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
                            color = RedColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
            color = BlackColor,
            fontWeight = FontWeight.Normal
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir",
            tint = BlackColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Componente para a linha divisória
@Composable
fun MenuDivider() {
    HorizontalDivider(
        color = DividerGreenLight,
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
            GreyBg = GreyBg,
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
            GreyBg = GreyBg,
            onNavigate = {},
            onLogout = {}
        )
    }
}
