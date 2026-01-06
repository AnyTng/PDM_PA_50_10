package ipca.app.lojasas.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView
import ipca.app.lojasas.ui.apoiado.home.ApoiadoHomeScreen
import ipca.app.lojasas.ui.apoiado.home.BlockedAccountScreen
import ipca.app.lojasas.ui.apoiado.menu.MenuApoiadoView
import ipca.app.lojasas.ui.apoiado.menu.document.DocumentSubmissionView
import ipca.app.lojasas.ui.apoiado.menu.document.SubmittedDocumentsView
import ipca.app.lojasas.ui.apoiado.menu.help.UrgentHelpView
import ipca.app.lojasas.ui.apoiado.menu.profile.ApoiadoProfileView
import ipca.app.lojasas.ui.apoiado.menu.profile.CreateProfileApoiadoView
import ipca.app.lojasas.ui.funcionario.calendar.CalendarView
import ipca.app.lojasas.ui.funcionario.cestas.CestaDetailsView
import ipca.app.lojasas.ui.funcionario.cestas.CestasListView
import ipca.app.lojasas.ui.funcionario.cestas.CreateCestaView
import ipca.app.lojasas.ui.funcionario.menu.MenuView
import ipca.app.lojasas.ui.funcionario.menu.apoiados.ApoiadosListView
import ipca.app.lojasas.ui.funcionario.menu.apoiados.CreateApoiadoView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignCreateView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignResultsView
import ipca.app.lojasas.ui.funcionario.menu.campaigns.CampaignsView
import ipca.app.lojasas.ui.funcionario.menu.pedidosurgentes.UrgentRequestsView
import ipca.app.lojasas.ui.funcionario.menu.profile.CollaboratorsListView
import ipca.app.lojasas.ui.funcionario.menu.profile.CreateProfileView
import ipca.app.lojasas.ui.funcionario.menu.profile.ProfileView
import ipca.app.lojasas.ui.funcionario.menu.validate.ValidateAccountsView
import ipca.app.lojasas.ui.funcionario.stock.expired.ExpiredProductsView
import ipca.app.lojasas.ui.funcionario.stock.ProductDetailsView
import ipca.app.lojasas.ui.funcionario.stock.ProductFormView
import ipca.app.lojasas.ui.funcionario.stock.ProductView
import ipca.app.lojasas.ui.funcionario.stock.ProductsView
import ipca.app.lojasas.ui.login.LoginView

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) { LoginView(navController = navController) }
        composable(Screen.ApoiadoHome.route) {
            ApoiadoHomeScreen(
                navController = navController,
                userId = Firebase.auth.currentUser?.uid ?: ""
            )
        }
        composable(Screen.FuncionarioHome.route) { CalendarView(navController = navController) }
        composable(Screen.MenuFuncionario.route) { MenuView(navController = navController) }
        composable(Screen.UrgentRequests.route) { UrgentRequestsView(navController = navController) }
        composable(Screen.CestasList.route) { CestasListView(navController = navController) }
        composable(
            route = Screen.CestaDetails.route,
            arguments = listOf(navArgument("cestaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cestaId = backStackEntry.arguments?.getString("cestaId")
                ?.let { Uri.decode(it) }
                .orEmpty()
            CestaDetailsView(cestaId = cestaId)
        }
        composable(Screen.CreateCesta.route) {
            CreateCestaView(navController = navController, fromUrgent = false, pedidoId = null, apoiadoId = null)
        }
        composable(
            route = Screen.CreateCestaUrgente.route,
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
        composable(Screen.CreateProfile.route) { CreateProfileView(navController = navController) }
        composable(Screen.CreateProfileApoiado.route) { CreateProfileApoiadoView(navController = navController) }
        composable(Screen.DocumentSubmission.route) { DocumentSubmissionView(navController = navController) }
        composable(Screen.MenuApoiado.route) { MenuApoiadoView(navController = navController) }
        composable(Screen.ProfileFuncionario.route) { ProfileView(navController = navController) }
        composable(Screen.ProfileApoiado.route) { ApoiadoProfileView(navController = navController) }
        composable(Screen.ValidateAccounts.route) { ValidateAccountsView(navController = navController) }
        composable(Screen.AccountBlocked.route) { BlockedAccountScreen(navController = navController) }
        composable(Screen.SubmittedDocuments.route) { SubmittedDocumentsView(navController = navController) }
        composable(Screen.Campaigns.route) { CampaignsView(navController = navController) }
        composable(Screen.CreateApoiado.route) { CreateApoiadoView(navController = navController) }
        composable(Screen.ApoiadosList.route) { ApoiadosListView(navController = navController) }

        composable(
            route = Screen.UrgentHelp.route,
            arguments = listOf(navArgument("numeroMecanografico") { type = NavType.StringType })
        ) { backStackEntry ->
            val numMec = backStackEntry.arguments?.getString("numeroMecanografico") ?: ""
            UrgentHelpView(navController = navController, numeroMecanografico = numMec)
        }

        composable(Screen.CampaignCreate.route) {
            CampaignCreateView(navController = navController)
        }

        composable(Screen.CollaboratorsList.route) {
            CollaboratorsListView(navController = navController)
        }

        composable(
            route = Screen.CampaignResults.route,
            arguments = listOf(navArgument("campaignName") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("campaignName") ?: ""
            CampaignResultsView(navController = navController, campaignName = name)
        }

        composable(Screen.CompleteData.route) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId") ?: ""
            CompleteDataView(
                docId = docId,
                onSuccess = {
                    navController.navigate(Screen.ApoiadoHome.route) {
                        popUpTo(Screen.CompleteData.route) { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable(Screen.StockProducts.route) {
            ProductsView(navController = navController)
        }
        composable(Screen.StockExpiredProducts.route) {
            ExpiredProductsView(navController = navController)
        }
        composable(
            route = Screen.StockProductsByName.route,
            arguments = listOf(navArgument("productName") { type = NavType.StringType })
        ) { backStackEntry ->
            val productName = backStackEntry.arguments?.getString("productName")?.let { Uri.decode(it) }.orEmpty()
            ProductDetailsView(navController = navController, nomeProduto = productName)
        }
        composable(
            route = Screen.StockProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            ProductView(navController = navController, productId = productId)
        }
        composable(
            route = Screen.StockProductEdit.route,
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
            route = Screen.StockProductCreate.route,
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
