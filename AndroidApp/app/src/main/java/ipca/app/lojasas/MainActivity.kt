package ipca.app.lojasas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.app.lojasas.ui.login.LoginView
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme
import ipca.app.lojasas.data.UserRoleRepository
import ipca.app.lojasas.data.destination
import ipca.app.lojasas.ui.apoiado.home.ApoiadoHomeScreen
import ipca.app.lojasas.ui.apoiado.menu.document.DocumentSubmissionView
import ipca.app.lojasas.ui.apoiado.home.BlockedAccountScreen
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView // Import necessário
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.funcionario.calendar.CalendarView
import ipca.app.lojasas.ui.funcionario.menu.MenuView
import ipca.app.lojasas.ui.funcionario.menu.profile.CreateProfileView
import ipca.app.lojasas.ui.funcionario.menu.profile.ProfileView
import ipca.app.lojasas.ui.apoiado.menu.profile.CreateProfileApoiadoView
import ipca.app.lojasas.ui.apoiado.menu.profile.ApoiadoProfileView
import ipca.app.lojasas.ui.funcionario.menu.validate.ValidateAccountsView
import ipca.app.lojasas.ui.apoiado.menu.document.SubmittedDocumentsView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val navController = rememberNavController()
            LojaSocialIPCATheme {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 1. CONFIGURAÇÃO DO FOOTER
                // REMOVIDO: "documentSubmission" desta lista
                val footerType = when (currentRoute) {
                    "funcionarioHome", "menu"   -> FooterType.FUNCIONARIO
                    "apoiadoHome", "menuApoiado" -> FooterType.APOIADO
                    else -> null // "completeData" e "documentSubmission" cairão aqui (sem footer)
                }

                // 2. CONFIGURAÇÃO DO HEADER
                // REMOVIDO: "documentSubmission" desta lista
                val headerConfig = when (currentRoute) {
                    "apoiadoHome" -> HeaderConfig(title = "Home")
                    "funcionarioHome" -> HeaderConfig(title = "Calendário")
                    "menu" -> HeaderConfig(title = "Menu")
                    "menuApoiado" -> HeaderConfig(title = "Menu")
                    "profileFuncionario" -> HeaderConfig(title = "Perfil Gestor", showBack = true, onBack = { navController.popBackStack() })
                    "profileApoiado" -> HeaderConfig(title = "Meu Perfil", showBack = true, onBack = { navController.popBackStack() })
                    "createProfile" -> HeaderConfig(title = "Criar Perfil", showBack = true, onBack = { navController.popBackStack() })
                    // "completeData" e "documentSubmission" cairão aqui (sem header)
                    else -> null
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        headerConfig?.let {
                            AppHeader(title = it.title, showBack = it.showBack, onBack = it.onBack)
                        }
                    },
                    bottomBar = {
                        footerType?.let {
                            Footer(navController = navController, type = it)
                        }
                    }
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") { LoginView(navController = navController) }
                        composable("apoiadoHome") { ApoiadoHomeScreen(navController = navController) }
                        composable("funcionarioHome") { CalendarView(navController = navController) }
                        composable("menu") { MenuView(navController = navController) }
                        composable("createProfile") { CreateProfileView(navController = navController) }
                        composable("createProfileApoiado") { CreateProfileApoiadoView(navController = navController) }
                        composable("documentSubmission") { DocumentSubmissionView(navController = navController) }
                        composable("menuApoiado") { ipca.app.lojasas.ui.apoiado.menu.MenuApoiadoView(navController = navController) }
                        composable("profileFuncionario") { ProfileView(navController = navController) }
                        composable("profileApoiado") { ApoiadoProfileView(navController = navController) }
                        composable("validateAccounts") { ValidateAccountsView(navController = navController) }
                        composable("accountBlocked") { BlockedAccountScreen(navController = navController) }
                        composable("submittedDocuments") {SubmittedDocumentsView(navController = navController)}
                        // --- NOVA ROTA PARA O FORMULÁRIO DE DADOS ---
                        composable("completeData/{docId}") { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("docId") ?: ""
                            CompleteDataView(
                                docId = docId,
                                onSuccess = {
                                    // Ao terminar, volta para a Home
                                    navController.navigate("apoiadoHome") {
                                        popUpTo("completeData/{docId}") { inclusive = true }
                                    }
                                },
                                navController = navController
                            )
                        }
                    }
                }
            }

            // ... (resto do código igual) ...
            LaunchedEffect(Unit) {
                val user = Firebase.auth.currentUser
                val email = user?.email
                if (email != null) {
                    UserRoleRepository.fetchUserRoleByEmail(
                        email = email,
                        onSuccess = { role ->
                            navController.navigate(role.destination()) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNotFound = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                        onError = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }
                    )
                }
            }
        }
    }
}
// ... (classes auxiliares HeaderConfig e Greeting mantêm-se)
private data class HeaderConfig(
    val title: String,
    val showBack: Boolean = false,
    val onBack: (() -> Unit)? = null
)