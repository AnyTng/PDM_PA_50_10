package ipca.app.lojasas

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.app.lojasas.ui.login.LoginView
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme
import ipca.app.lojasas.data.UserRoleRepository
import ipca.app.lojasas.data.destination
import ipca.app.lojasas.ui.apoiado.home.ApoiadoHomeScreen
import ipca.app.lojasas.ui.apoiado.formulario.document.DocumentSubmissionView
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import ipca.app.lojasas.ui.funcionario.calendar.CalendarView
import ipca.app.lojasas.ui.funcionario.menu.MenuView
import ipca.app.lojasas.ui.funcionario.menu.profile.CreateProfileView // Adicionar import
import ipca.app.lojasas.ui.funcionario.menu.profile.ProfileView // <--- Import
import ipca.app.lojasas.ui.apoiado.menu.profile.CreateProfileApoiadoView
import ipca.app.lojasas.ui.apoiado.menu.profile.ApoiadoProfileView // Importe a nova view
import ipca.app.lojasas.ui.funcionario.stock.ProductDetailsView
import ipca.app.lojasas.ui.funcionario.stock.ProductFormView
import ipca.app.lojasas.ui.funcionario.stock.ProductView
import ipca.app.lojasas.ui.funcionario.stock.ProductsView


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
                val subCategoryArg = navBackStackEntry?.arguments?.getString("subCategory")?.let { Uri.decode(it) }


                // 1. CONFIGURAÇÃO DO FOOTER
                val footerType = when (currentRoute) {
                    "funcionarioHome",
                    "menu",
                    "createProfile",
                    "profileFuncionario",
                    "stockProducts",
                    "stockProducts/{subCategory}",
                    "stockProduct/{productId}",
                    "stockProductEdit/{productId}",
                    "stockProductCreate?subCategory={subCategory}" -> FooterType.FUNCIONARIO
                    "apoiadoHome", "menuApoiado", "profileApoiado", "documentSubmission" -> FooterType.APOIADO
                    else -> null
                }

                // 2. CONFIGURAÇÃO DO HEADER
                val headerConfig = when (currentRoute) {
                    "apoiadoHome" -> HeaderConfig(title = "Home")
                    "funcionarioHome" -> HeaderConfig(title = "Calendário")
                    "stockProducts" -> HeaderConfig(title = "Stock")
                    "menu" -> HeaderConfig(title = "Menu")
                    "menuApoiado" -> HeaderConfig(title = "Menu")

                    // Rota Funcionario
                    "profileFuncionario" -> HeaderConfig(
                        title = "Perfil Gestor",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    // Rota Apoiado
                    "profileApoiado" -> HeaderConfig(
                        title = "Meu Perfil",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )

                    "createProfile" -> HeaderConfig(
                        title = "Criar Perfil",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "documentSubmission" -> HeaderConfig(
                        title = "Entrega Docs",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "stockProducts/{subCategory}" -> HeaderConfig(
                        title = subCategoryArg ?: "Stock",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "stockProduct/{productId}" -> HeaderConfig(
                        title = "Detalhes",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "stockProductEdit/{productId}" -> HeaderConfig(
                        title = "Editar Produto",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "stockProductCreate?subCategory={subCategory}" -> HeaderConfig(
                        title = "Novo Produto",
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
                        /*composable("profile") {
                            ProfileView(navController = navController)
                        }*/
                        composable("createProfileApoiado") {
                            CreateProfileApoiadoView(navController = navController)
                        }
                        composable("documentSubmission") {
                            DocumentSubmissionView(navController = navController)
                        }
                        composable("menuApoiado") {
                            // Importe o MenuApoiadoView que criamos no passo 1
                            ipca.app.lojasas.ui.apoiado.menu.MenuApoiadoView(navController = navController)
                        }
                        // Perfil do Funcionario (Usa a view antiga)
                        composable("profileFuncionario") {
                            ProfileView(navController = navController)
                        }

                        // Perfil do Apoiado (Usa a NOVA view)
                        composable("profileApoiado") {
                            ApoiadoProfileView(navController = navController)
                        }

                        composable("stockProducts") {
                            ProductsView(navController = navController)
                        }
                        composable(
                            route = "stockProducts/{subCategory}",
                            arguments = listOf(navArgument("subCategory") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val subCategory = backStackEntry.arguments?.getString("subCategory")?.let { Uri.decode(it) }.orEmpty()
                            ProductDetailsView(navController = navController, subCategoria = subCategory)
                        }
                        composable(
                            route = "stockProduct/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                            ProductView(navController = navController, productId = productId)
                        }
                        composable(
                            route = "stockProductEdit/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                            ProductFormView(
                                navController = navController,
                                productId = productId,
                                prefillSubCategoria = null
                            )
                        }
                        composable(
                            route = "stockProductCreate?subCategory={subCategory}",
                            arguments = listOf(
                                navArgument("subCategory") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val subCategory = backStackEntry.arguments?.getString("subCategory").orEmpty()
                            ProductFormView(
                                navController = navController,
                                productId = null,
                                prefillSubCategoria = Uri.decode(subCategory).takeIf { it.isNotBlank() }
                            )
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
