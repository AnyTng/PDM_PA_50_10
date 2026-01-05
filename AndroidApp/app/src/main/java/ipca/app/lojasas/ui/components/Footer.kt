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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import ipca.app.lojasas.ui.theme.GreenSas
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

enum class FooterType { FUNCIONARIO, APOIADO }

private sealed class FooterIconSpec {
    data class Drawable(val resId: Int) : FooterIconSpec()
    object HeartHome : FooterIconSpec()
}

private data class FooterItem(
    val icon: FooterIconSpec,
    val contentDescription: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun Footer(
    navController: NavController, // 1. Recebe o controlador aqui
    type: FooterType = FooterType.FUNCIONARIO,
    useNative: Boolean = false
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    fun isActiveRoute(vararg routes: String): Boolean {
        return currentRoute != null && routes.any { it == currentRoute }
    }

    val items = when (type) {
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
                "stockExpiredProducts",
                "stockProduct/{productId}",
                "stockProductEdit/{productId}",
                "stockProductCreate?productName={productName}"
            )
            val menuActive = isActiveRoute("menu")

            listOf(
                FooterItem(
                    icon = FooterIconSpec.Drawable(R.drawable.calendarmonth),
                    contentDescription = "Calendário",
                    selected = calendarActive,
                    onClick = {
                        // Navega para a Home do Funcionário e limpa a pilha para não acumular ecrãs
                        navController.navigate("funcionarioHome") {
                            popUpTo("funcionarioHome") { inclusive = true }
                        }
                    }
                ),
                FooterItem(
                    icon = FooterIconSpec.Drawable(R.drawable.shoppingbag),
                    contentDescription = "Saco",
                    selected = cestasActive,
                    onClick = {
                        // Lista/Gestão de Cestas
                        navController.navigate("cestasList") {
                            launchSingleTop = true
                        }
                    }
                ),
                FooterItem(
                    icon = FooterIconSpec.Drawable(R.drawable.grocery),
                    contentDescription = "Mercearia",
                    selected = stockActive,
                    onClick = {
                        navController.navigate("stockProducts") {
                            launchSingleTop = true
                        }
                    }
                ),
                FooterItem(
                    icon = FooterIconSpec.Drawable(R.drawable.dehaze),
                    contentDescription = "Menu",
                    selected = menuActive,
                    onClick = { navController.navigate("menu") }
                )
            )
        }

        FooterType.APOIADO -> {
            val homeActive = isActiveRoute("apoiadoHome")
            val menuActive = isActiveRoute("menuApoiado")

            listOf(
                FooterItem(
                    icon = FooterIconSpec.HeartHome,
                    contentDescription = "Início",
                    selected = homeActive,
                    onClick = {
                        navController.navigate("apoiadoHome") {
                            popUpTo("apoiadoHome") { inclusive = true }
                        }
                    }
                ),
                FooterItem(
                    icon = FooterIconSpec.Drawable(R.drawable.dehaze),
                    contentDescription = "Menu",
                    selected = menuActive,
                    onClick = {
                        // ADICIONAR NAVEGAÇÃO AQUI
                        navController.navigate("menuApoiado")
                    }
                )
            )
        }
    }

    if (useNative) {
        NativeFooter(items = items)
    } else {
        LegacyFooter(items = items)
    }
}

@Composable
private fun LegacyFooter(items: List<FooterItem>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(
                color = GreenSas,
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

        items.forEach { item ->
            when (val icon = item.icon) {
                is FooterIconSpec.Drawable -> FooterIcon(
                    painterRes = icon.resId,
                    contentDescription = item.contentDescription,
                    onClick = item.onClick,
                    enabled = !item.selected,
                    modifier = itemModifier
                )

                FooterIconSpec.HeartHome -> HeartHomeIcon(
                    contentDescription = item.contentDescription,
                    onClick = item.onClick,
                    enabled = !item.selected,
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
private fun NativeFooter(items: List<FooterItem>) {
    val selectedColor = GreenSas
    val unselectedColor = GreenSas.copy(alpha = 0.55f)
    val indicatorColor = GreenSas.copy(alpha = 0.18f)

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = {
                    if (!item.selected) {
                        item.onClick()
                    }
                },
                icon = {
                    when (val icon = item.icon) {
                        is FooterIconSpec.Drawable -> FooterNavIcon(
                            painterRes = icon.resId,
                            contentDescription = item.contentDescription
                        )

                        FooterIconSpec.HeartHome -> HeartHomeNavIcon(
                            contentDescription = item.contentDescription
                        )
                    }
                },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}

@Composable
private fun FooterNavIcon(
    painterRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = painterRes),
        contentDescription = contentDescription,
        modifier = modifier.size(30.dp)
    )
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
    contentDescription: String,
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
            contentDescription = contentDescription,
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

@Composable
private fun HeartHomeNavIcon(
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val iconSize = 30.dp
    val heartSize = iconSize * 0.43f

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = contentDescription,
            tint = LocalContentColor.current,
            modifier = Modifier.size(iconSize)
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = LocalContentColor.current,
            modifier = Modifier.size(heartSize)
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
