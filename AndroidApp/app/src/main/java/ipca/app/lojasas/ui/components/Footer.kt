package ipca.app.lojasas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ipca.app.lojasas.R

enum class FooterType { FUNCIONARIO, APOIADO }

@Composable
fun Footer(
    type: FooterType = FooterType.FUNCIONARIO,
    onCalendarClick: () -> Unit = {},
    onBagClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Color(0xFF094E33),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (type) {
            FooterType.FUNCIONARIO -> {
                FooterIcon(
                    painterRes = R.drawable.calendarmonth,
                    contentDescription = "Calendário",
                    onClick = onCalendarClick
                )
                FooterIcon(
                    painterRes = R.drawable.shoppingbag,
                    contentDescription = "Saco",
                    onClick = onBagClick
                )
                FooterIcon(
                    painterRes = R.drawable.grocery,
                    contentDescription = "Mercearia",
                    onClick = onInventoryClick
                )
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = onMenuClick
                )
            }
            FooterType.APOIADO -> {
                FooterIcon(
                    painterRes = R.drawable.shoppingbag,
                    contentDescription = "Saco",
                    onClick = onBagClick
                )
                HeartHomeIcon(onClick = onHomeClick)
                FooterIcon(
                    painterRes = R.drawable.dehaze,
                    contentDescription = "Menu",
                    onClick = onMenuClick
                )
            }
        }
    }
}

@Composable
private fun FooterIcon(
    painterRes: Int,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    Image(
        painter = painterResource(id = painterRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
    )
}

@Composable
private fun HeartHomeIcon(
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = "Início",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FooterFuncionarioPreview() {
    Footer(type = FooterType.FUNCIONARIO)
}

@Preview(showBackground = true)
@Composable
fun FooterApoiadoPreview() {
    Footer(type = FooterType.APOIADO)
}
