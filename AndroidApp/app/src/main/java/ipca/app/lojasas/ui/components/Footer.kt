package ipca.app.lojasas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ipca.app.lojasas.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

enum class FooterType { FUNCIONARIO, APOIADO }

@Composable
fun Footer(
    navController: NavController, // 1. Recebe o controlador aqui
    type: FooterType = FooterType.FUNCIONARIO
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    fun isActiveRoute(vararg routes: String): Boolean {
        return currentRoute != null && routes.any { it == currentRoute }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(
                color = Color(0xFF094E33),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val itemModifier = Modifier.weight(1f)

        when (type) {
            FooterType.FUNCIONARIO -> {
                val calendarActive = isActiveRoute("funcionarioHome")
                val cestasActive = isActiveRoute(
                    "cestasList",
                    "cestaDetails/{cestaId}",
                    "createCesta",
                    "createCestaUrgente/{pedidoId}/{apoiadoId}"
                )
                val stockActive = isActiveRoute(
                    "stockProducts",
                    "stockProducts/{productName}",
                    "stockProduct/{productId}",
                    "stockProductEdit/{productId}",
                    "stockProductCreate?productName={productName}"
                )
                val menuActive = isActiveRoute("menu")

                FooterIcon(
                    painterRes = R.drawable.calendarmonth,
                    contentDescription = "Calendário",
                    onClick = {
                        // Navega para a Home do Funcionário e limpa a pilha para não acumular ecrãs
                        navController.navigate("funcionarioHome") {
                            popUpTo("funcionarioHome") { inclusive = true }
                        }
                    },
                    enabled = !calendarActive,
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.shoppingbag,
                    contentDescription = "Saco",
                    onClick = {
                        // Lista/Gestão de Cestas
                        navController.navigate("cestasList") {
                            launchSingleTop = true
                        }
                    },
                    enabled = !cestasActive,
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.grocery,
                    contentDescription = "Mercearia",
                    onClick = {
                        navController.navigate("stockProducts") {
                            launchSingleTop = true
                        }
                    },
                    enabled = !stockActive,
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = { navController.navigate("menu") },
                    enabled = !menuActive,
                    modifier = itemModifier
                )
            }

            FooterType.APOIADO -> {
                val homeActive = isActiveRoute("apoiadoHome")
                val menuActive = isActiveRoute("menuApoiado")

                HeartHomeIcon(
                    onClick = {
                        navController.navigate("apoiadoHome") {
                            popUpTo("apoiadoHome") { inclusive = true }
                        }
                    },
                    enabled = !homeActive,
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = {
                        // ADICIONAR NAVEGAÇÃO AQUI
                        navController.navigate("menuApoiado")
                    },
                    enabled = !menuActive,
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
private fun FooterIcon(
    painterRes: Int,
    contentDescription: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = painterRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(44.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
        )
    }
}

@Composable
private fun HeartHomeIcon(
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "Início",
            tint = Color.White,
            modifier = Modifier
                .size(42.dp)
                .alpha(if (enabled) 1f else 0.5f)
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .alpha(if (enabled) 1f else 0.5f)
        )
    }
}




@Preview(showBackground = true)
@Composable
fun FooterFuncionarioPreview() {
    // O rememberNavController cria uma instância válida para desenhar a UI,
    // mesmo que a navegação não funcione dentro da pré-visualização estática.
    val navController = rememberNavController()
    Footer(
        navController = navController,
        type = FooterType.FUNCIONARIO
    )
}

@Preview(showBackground = true)
@Composable
fun FooterApoiadoPreview() {
    val navController = rememberNavController()
    Footer(
        navController = navController,
        type = FooterType.APOIADO
    )
}
