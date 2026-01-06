package ipca.app.lojasas.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ApoiadoHome : Screen("apoiadoHome")
    object FuncionarioHome : Screen("funcionarioHome")
    object MenuFuncionario : Screen("menu")
    object AdminManual : Screen("adminManual")
    object BeneficiarioManual : Screen("beneficiarioManual")
    object Historico : Screen("historico")
    object UrgentRequests : Screen("urgentRequests")
    object CestasList : Screen("cestasList")
    object CestaDetails : Screen("cestaDetails/{cestaId}") {
        fun createRoute(cestaId: String): String = "cestaDetails/${Uri.encode(cestaId)}"
    }
    object CreateCesta : Screen("createCesta")
    object CreateCestaUrgente : Screen("createCestaUrgente/{pedidoId}/{apoiadoId}") {
        fun createRoute(pedidoId: String, apoiadoId: String): String {
            return "createCestaUrgente/${Uri.encode(pedidoId)}/${Uri.encode(apoiadoId)}"
        }
    }
    object CreateProfile : Screen("createProfile")
    object CreateProfileApoiado : Screen("createProfileApoiado")
    object DocumentSubmission : Screen("documentSubmission")
    object MenuApoiado : Screen("menuApoiado")
    object ProfileFuncionario : Screen("profileFuncionario")
    object ProfileApoiado : Screen("profileApoiado")
    object ValidateAccounts : Screen("validateAccounts")
    object AccountBlocked : Screen("accountBlocked")
    object SubmittedDocuments : Screen("submittedDocuments")
    object Campaigns : Screen("campaigns")
    object CreateApoiado : Screen("createApoiado")
    object ApoiadosList : Screen("apoiadosList")
    object UrgentHelp : Screen("urgent_help_screen/{numeroMecanografico}") {
        fun createRoute(numeroMecanografico: String): String {
            return "urgent_help_screen/${Uri.encode(numeroMecanografico)}"
        }
    }
    object CampaignCreate : Screen("campaignCreate")
    object CollaboratorsList : Screen("collaboratorsList")
    object CampaignResults : Screen("campaignResults/{campaignName}") {
        fun createRoute(campaignName: String): String {
            return "campaignResults/${Uri.encode(campaignName)}"
        }
    }
    object CompleteData : Screen("completeData/{docId}") {
        fun createRoute(docId: String): String = "completeData/${Uri.encode(docId)}"
    }
    object StockProducts : Screen("stockProducts")
    object StockExpiredProducts : Screen("stockExpiredProducts")
    object StockProductsByName : Screen("stockProducts/{productName}") {
        fun createRoute(productName: String): String = "stockProducts/${Uri.encode(productName)}"
    }
    object StockProduct : Screen("stockProduct/{productId}") {
        fun createRoute(productId: String): String = "stockProduct/${Uri.encode(productId)}"
    }
    object StockProductEdit : Screen("stockProductEdit/{productId}") {
        fun createRoute(productId: String): String = "stockProductEdit/${Uri.encode(productId)}"
    }
    object StockProductCreate : Screen("stockProductCreate?productName={productName}") {
        fun createRoute(productName: String? = null): String {
            val encoded = productName?.let { Uri.encode(it) }.orEmpty()
            return if (encoded.isBlank()) {
                "stockProductCreate"
            } else {
                "stockProductCreate?productName=$encoded"
            }
        }
    }

    companion object {
        private val allScreens = listOf(
            Login,
            ApoiadoHome,
            FuncionarioHome,
            MenuFuncionario,
            AdminManual,
            BeneficiarioManual,
            Historico,
            UrgentRequests,
            CestasList,
            CestaDetails,
            CreateCesta,
            CreateCestaUrgente,
            CreateProfile,
            CreateProfileApoiado,
            DocumentSubmission,
            MenuApoiado,
            ProfileFuncionario,
            ProfileApoiado,
            ValidateAccounts,
            AccountBlocked,
            SubmittedDocuments,
            Campaigns,
            CreateApoiado,
            ApoiadosList,
            UrgentHelp,
            CampaignCreate,
            CollaboratorsList,
            CampaignResults,
            CompleteData,
            StockProducts,
            StockExpiredProducts,
            StockProductsByName,
            StockProduct,
            StockProductEdit,
            StockProductCreate
        )

        fun fromRoute(route: String?): Screen? {
            if (route == null) return null
            return allScreens.firstOrNull { it.route == route }
        }
    }
}
