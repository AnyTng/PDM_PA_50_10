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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
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
import ipca.app.lojasas.ui.apoiado.menu.profile.ApoiadoProfileView // Importe a nova view
import ipca.app.lojasas.ui.funcionario.stock.ProductDetailsView
import ipca.app.lojasas.ui.funcionario.stock.ProductFormView
import ipca.app.lojasas.ui.funcionario.stock.ProductView
import ipca.app.lojasas.ui.funcionario.stock.ProductsView
import ipca.app.lojasas.ui.apoiado.menu.profile.ApoiadoProfileView
import ipca.app.lojasas.ui.funcionario.menu.validate.ValidateAccountsView
import ipca.app.lojasas.ui.apoiado.menu.document.SubmittedDocumentsView
import androidx.navigation.navArgument
import ipca.app.lojasas.ui.apoiado.menu.help.UrgentHelpView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignsView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignCreateView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignResultsView
import ipca.app.lojasas.ui.funcionario.menu.apoiados.ApoiadosListView // Adiciona este import
import ipca.app.lojasas.ui.funcionario.menu.apoiados.CreateApoiadoView
import ipca.app.lojasas.ui.funcionario.menu.profile.CollaboratorsListView
import ipca.app.lojasas.ui.funcionario.pedidosurgentes.UrgentRequestsView
import ipca.app.lojasas.ui.funcionario.cestas.CestasListView
import ipca.app.lojasas.ui.funcionario.cestas.CreateCestaView
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

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
            AskNotificationPermissionOnce()
            val navController = rememberNavController()
            LojaSocialIPCATheme {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val productNameArg = navBackStackEntry?.arguments?.getString("productName")?.let { Uri.decode(it) }

                // 1. CONFIGURAÇÃO DO FOOTER
                // REMOVIDO: "documentSubmission" desta lista
                val footerType = when (currentRoute) {
                    "funcionarioHome",
                    "menu",
                    "urgentRequests",
                    "cestasList",
                    "createCesta",
                    "createCestaUrgente/{pedidoId}/{apoiadoId}",
                    "stockProducts",
                    "stockProducts/{productName}",
                    "stockProduct/{productId}",
                    "stockProductEdit/{productId}",
                    "stockProductCreate?productName={productName}"-> FooterType.FUNCIONARIO
                    "apoiadoHome", "menuApoiado" -> FooterType.APOIADO
                    else -> null // "completeData" e "documentSubmission" cairão aqui (sem footer)
                }

                // 2. CONFIGURAÇÃO DO HEADER
                // REMOVIDO: "documentSubmission" desta lista
                val headerConfig = when (currentRoute) {
                    "apoiadoHome" -> HeaderConfig(title = "Home")
                    "funcionarioHome" -> HeaderConfig(title = "Calendário")
                    "stockProducts" -> HeaderConfig(title = "Stock")
                    "menu" -> HeaderConfig(title = "Menu")
                    "urgentRequests" -> HeaderConfig(title = "Pedidos Urgentes", showBack = true, onBack = { navController.popBackStack() })
                    "cestasList" -> HeaderConfig(title = "Cestas")
                    "createCesta" -> HeaderConfig(title = "Criar Cesta", showBack = true, onBack = { navController.popBackStack() })
                    "createCestaUrgente/{pedidoId}/{apoiadoId}" -> HeaderConfig(title = "Criar Cesta", showBack = true, onBack = { navController.popBackStack() })
                    "menuApoiado" -> HeaderConfig(title = "Menu")
                    "profileFuncionario" -> HeaderConfig(title = "Perfil Gestor", showBack = true, onBack = { navController.popBackStack() })
                    "profileApoiado" -> HeaderConfig(title = "Meu Perfil", showBack = true, onBack = { navController.popBackStack() })
                    "createProfile" -> HeaderConfig(title = "Criar Perfil", showBack = true, onBack = { navController.popBackStack() })
                    // "completeData" e "documentSubmission" cairão aqui (sem header)
                    "stockProducts/{productName}" -> HeaderConfig(
                        title = productNameArg ?: "Stock",
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
                    "stockProductCreate?productName={productName}" -> HeaderConfig(
                        title = "Novo Produto",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "campaigns" -> HeaderConfig(
                        title = "Campanhas",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "campaignCreate" -> HeaderConfig(
                        title = "Nova Campanha",
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    "campaignResults/{campaignName}" -> HeaderConfig(
                        title = "Resultados",
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
                        composable("apoiadoHome") {
                            ApoiadoHomeScreen(
                                navController = navController,
                                userId = Firebase.auth.currentUser?.uid ?: ""
                            )
                        }
                        composable("funcionarioHome") { CalendarView(navController = navController) }
                        composable("menu") { MenuView(navController = navController) }
                        composable("urgentRequests") { UrgentRequestsView(navController = navController) }
                        composable("cestasList") { CestasListView(navController = navController) }
                        composable("createCesta") { CreateCestaView(navController = navController, fromUrgent = false, pedidoId = null, apoiadoId = null) }
                        composable(
                            route = "createCestaUrgente/{pedidoId}/{apoiadoId}",
                            arguments = listOf(
                                navArgument("pedidoId") { type = NavType.StringType },
                                navArgument("apoiadoId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val pedidoId = backStackEntry.arguments?.getString("pedidoId").orEmpty()
                            val apoiadoId = backStackEntry.arguments?.getString("apoiadoId").orEmpty()
                            CreateCestaView(
                                navController = navController,
                                fromUrgent = true,
                                pedidoId = pedidoId,
                                apoiadoId = apoiadoId
                            )
                        }
                        composable("createProfile") { CreateProfileView(navController = navController) }
                        composable("createProfileApoiado") { CreateProfileApoiadoView(navController = navController) }
                        composable("documentSubmission") { DocumentSubmissionView(navController = navController) }
                        composable("menuApoiado") { ipca.app.lojasas.ui.apoiado.menu.MenuApoiadoView(navController = navController) }
                        composable("profileFuncionario") { ProfileView(navController = navController) }
                        composable("profileApoiado") { ApoiadoProfileView(navController = navController) }
                        composable("validateAccounts") { ValidateAccountsView(navController = navController) }
                        composable("accountBlocked") { BlockedAccountScreen(navController = navController) }
                        composable("submittedDocuments") {SubmittedDocumentsView(navController = navController)}
                        composable("campaigns") { CampaignsView(navController = navController) }
                        composable("createApoiado") { CreateApoiadoView(navController = navController) }
                        composable("apoiadosList") { ApoiadosListView(navController = navController) }

                        composable(
                            route = "urgent_help_screen/{numeroMecanografico}", // Rota com placeholder
                            arguments = listOf(navArgument("numeroMecanografico") { type = NavType.StringType })
                        ) { backStackEntry ->
                            // Extrai o argumento "a1" que vem da navegação
                            val numMec = backStackEntry.arguments?.getString("numeroMecanografico") ?: ""

                            // Passa "a1" para a View
                            UrgentHelpView(navController = navController, numeroMecanografico = numMec)
                        }



// 2. Rota de Criação
                        composable("campaignCreate") {
                            CampaignCreateView(navController = navController)
                        }
                        composable("createProfile") { CreateProfileView(navController = navController) }

// NOVA ROTA: Lista de Colaboradores
                        composable("collaboratorsList") {
                            CollaboratorsListView(navController = navController)
                        }
// 3. Rota de Resultados (passando o nome da campanha)
                        composable(
                            route = "campaignResults/{campaignName}",
                            arguments = listOf(navArgument("campaignName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("campaignName") ?: ""
                            CampaignResultsView(navController = navController, campaignName = name)
                        }
                        // --- NOVA ROTA PARA O FORMUL ÁRIO DE DADOS ---
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
                            route = "stockProducts/{productName}",
                            arguments = listOf(navArgument("productName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productName = backStackEntry.arguments?.getString("productName")?.let { Uri.decode(it) }.orEmpty()
                            ProductDetailsView(navController = navController, nomeProduto = productName)
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
                                prefillNomeProduto = null
                            )
                        }
                        composable(
                            route = "stockProductCreate?productName={productName}",
                            arguments = listOf(
                                navArgument("productName") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val productName = backStackEntry.arguments?.getString("productName").orEmpty()
                            ProductFormView(
                                navController = navController,
                                productId = null,
                                prefillNomeProduto = Uri.decode(productName).takeIf { it.isNotBlank() }
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

//Pedir permissão de notificações
@Composable
fun AskNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
