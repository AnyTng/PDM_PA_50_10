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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ipca.app.lojasas.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

enum class FooterType { FUNCIONARIO, APOIADO }

@Composable
fun Footer(
    navController: NavController, // 1. Recebe o controlador aqui
    type: FooterType = FooterType.FUNCIONARIO
) {
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
                FooterIcon(
                    painterRes = R.drawable.calendarmonth,
                    contentDescription = "Calendário",
                    onClick = {
                        // Navega para a Home do Funcionário e limpa a pilha para não acumular ecrãs
                        navController.navigate("funcionarioHome") {
                            popUpTo("funcionarioHome") { inclusive = true }
                        }
                    },
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.shoppingbag,
                    contentDescription = "Saco",
                    onClick = {
                        // Exemplo: navController.navigate("saco")
                    },
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.grocery,
                    contentDescription = "Mercearia",
                    onClick = {
                        // Exemplo: navController.navigate("inventario")
                    },
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = { navController.navigate("menu") },
                    modifier = itemModifier
                )
            }

            FooterType.APOIADO -> {
                FooterIcon(
                    painterRes = R.drawable.shoppingbag,
                    contentDescription = "Saco",
                    onClick = {
                        // Exemplo: navController.navigate("saco")
                    },
                    modifier = itemModifier
                )
                HeartHomeIcon(
                    onClick = {
                        navController.navigate("apoiadoHome") {
                            popUpTo("apoiadoHome") { inclusive = true }
                        }
                    },
                    modifier = itemModifier
                )
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = {
                        // ADICIONAR NAVEGAÇÃO AQUI
                        navController.navigate("menuApoiado")
                    },
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
                .clickable { onClick() }
        )
    }
}

@Composable
private fun HeartHomeIcon(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "Início",
            tint = Color.White,
            modifier = Modifier.size(42.dp)
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
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
