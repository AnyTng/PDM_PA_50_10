package ipca.app.lojasas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
import ipca.app.lojasas.ui.apoiado.ApoiadoHomeScreen
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.funcionario.calendar.CalendarView
import ipca.app.lojasas.ui.funcionario.menu.MenuView
import ipca.app.lojasas.ui.funcionario.profile.CreateProfileView // Adicionar import
import ipca.app.lojasas.ui.funcionario.profile.ProfileView // <--- Import

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
                val footerType = when (currentRoute) {
                    "funcionarioHome", "menu", "createProfile", "profile" -> FooterType.FUNCIONARIO
                    "apoiadoHome" -> FooterType.APOIADO
                    else -> null
                }

                val headerConfig = when (currentRoute) {
                    "apoiadoHome" -> HeaderConfig(title = "Home")
                    "funcionarioHome" -> HeaderConfig(title = "Calendário")
                    "menu" -> HeaderConfig(title = "Menu")
                    "createProfile" -> HeaderConfig(
                        title = "Criar Perfil",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "profile" -> HeaderConfig(
                        title = "O meu Perfil",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    else -> null
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        headerConfig?.let {
                            AppHeader(
                                title = it.title,
                                showBack = it.showBack,
                                onBack = it.onBack
                            )
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
                        composable("login") {
                            LoginView(navController = navController)
                        }

                        // Se o ApoiadoHomeScreen também precisar de navegação, adiciona lá também
                        composable("apoiadoHome") {
                            ApoiadoHomeScreen(navController = navController)
                        }

                        // CORREÇÃO AQUI: Passar o navController
                        composable("funcionarioHome") {
                            CalendarView(navController = navController)
                        }

                        composable("menu") {
                            MenuView(navController = navController)
                        }

                        composable("createProfile") {
                            CreateProfileView(navController = navController)
                        }
                        composable("profile") {
                            ProfileView(navController = navController)
                        }
                    }
                }
            }

            // Verifica se o utilizador já está logado
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
                        onNotFound = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onError = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class HeaderConfig(
    val title: String,
    val showBack: Boolean = false,
    val onBack: (() -> Unit)? = null
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! (Login efetuado)",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LojaSocialIPCATheme {
        Greeting("Android")
    }
}
