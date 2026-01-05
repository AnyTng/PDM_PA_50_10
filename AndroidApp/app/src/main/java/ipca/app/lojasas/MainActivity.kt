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
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.core.navigation.AppNavGraph
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme
import ipca.app.lojasas.data.UserRoleRepository
import ipca.app.lojasas.data.destination
import ipca.app.lojasas.R
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
import android.Manifest
import android.content.Context
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
                val currentScreen = Screen.fromRoute(currentRoute)
                val productNameArg = navBackStackEntry?.arguments?.getString("productName")?.let { Uri.decode(it) }

                // 1. CONFIGURAÇÃO DO FOOTER
                val footerType = when (currentScreen) {
                    Screen.FuncionarioHome,
                    Screen.MenuFuncionario,
                    Screen.CestasList,
                    Screen.CreateCestaUrgente,
                    Screen.StockProducts,
                    Screen.StockProductsByName,
                    Screen.StockProduct,
                    Screen.StockProductEdit,
                    Screen.StockProductCreate -> FooterType.FUNCIONARIO
                    Screen.ApoiadoHome,
                    Screen.MenuApoiado -> FooterType.APOIADO
                    else -> null // itens sem footer vao cair aq
                }
                val useNativeFooter = true

                // 2. CONFIGURAÇÃO DO HEADER
                val headerConfig = when (currentScreen) {
                    Screen.ApoiadoHome -> HeaderConfig(title = stringResource(R.string.header_home))
                    Screen.FuncionarioHome -> HeaderConfig(title = stringResource(R.string.header_calendar))
                    Screen.StockProducts -> HeaderConfig(title = stringResource(R.string.header_stock))
                    Screen.MenuFuncionario -> HeaderConfig(title = stringResource(R.string.header_menu))
                    Screen.UrgentRequests -> HeaderConfig(
                        title = stringResource(R.string.header_urgent_requests),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CestasList -> HeaderConfig(title = stringResource(R.string.header_cestas))
                    Screen.CestaDetails -> HeaderConfig(
                        title = stringResource(R.string.header_cesta_details),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CreateCesta -> HeaderConfig(
                        title = stringResource(R.string.header_create_cesta),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CreateCestaUrgente -> HeaderConfig(
                        title = stringResource(R.string.header_create_cesta),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.MenuApoiado -> HeaderConfig(title = stringResource(R.string.header_menu))
                    Screen.ProfileFuncionario -> HeaderConfig(
                        title = stringResource(R.string.header_profile_funcionario),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.ProfileApoiado -> HeaderConfig(
                        title = stringResource(R.string.header_profile_apoiado),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CreateProfile -> HeaderConfig(
                        title = stringResource(R.string.header_create_profile),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.UrgentHelp -> HeaderConfig(
                        title = stringResource(R.string.header_urgent_help),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    // "completeData" e "documentSubmission" cairão aqui (sem header)
                    Screen.StockProductsByName -> HeaderConfig(
                        title = productNameArg ?: stringResource(R.string.header_stock),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.StockExpiredProducts -> HeaderConfig(
                        title = stringResource(R.string.header_expired_products),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.StockProduct -> HeaderConfig(
                        title = stringResource(R.string.header_product_details),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.StockProductEdit -> HeaderConfig(
                        title = stringResource(R.string.header_edit_product),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.StockProductCreate -> HeaderConfig(
                        title = stringResource(R.string.header_new_product),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.Campaigns -> HeaderConfig(
                        title = stringResource(R.string.header_campaigns),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CampaignCreate -> HeaderConfig(
                        title = stringResource(R.string.header_new_campaign),
                        showBack = true,
                        onBack = { navController.popBackStack() }
                    )
                    Screen.CampaignResults -> HeaderConfig(
                        title = stringResource(R.string.header_campaign_results),
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
                            Footer(navController = navController, type = it, useNative = useNativeFooter)
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }


            LaunchedEffect(Unit) {
                val user = Firebase.auth.currentUser
                val email = user?.email
                if (email != null) {
                    // OTIMIZAÇÃO: Verifica cache primeiro
                    val prefs = this@MainActivity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val cachedRoleStr = prefs.getString("role_${email}", null)

                    if (cachedRoleStr != null) {
                        try {
                            val role = UserRole.valueOf(cachedRoleStr)
                            navController.navigate(role.destination()) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            // Se falhar o parse (raro), ignora e busca na rede
                        }
                    }

                    // Busca na rede (se não houver cache ou como fallback/update silencioso se quiséssemos)
                    // Mas para velocidade, se temos cache, o código acima já navegou.
                    // Se NÃO temos cache, executamos o fetch:
                    if (cachedRoleStr == null) {
                        UserRoleRepository.fetchUserRoleByEmail(
                            email = email,
                            onSuccess = { role ->
                                // Salva na cache para a próxima vez
                                prefs.edit().putString("role_${email}", role.name).apply()

                                navController.navigate(role.destination()) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onNotFound = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
// Configuração do header
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
