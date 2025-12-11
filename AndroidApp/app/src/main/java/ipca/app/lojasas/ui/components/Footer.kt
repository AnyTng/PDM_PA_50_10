package ipca.app.lojasas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ipca.app.lojasas.R

@Composable
fun Footer(
    onCalendarClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
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
        horizontalArrangement = Arrangement.Center, // Centralizado como no Figma
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icone do Calendário
        Image(
            painter = painterResource(id = R.drawable.calendarmonth), // O teu SVG importado
            contentDescription = "Calendário",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(30.dp)
                .height(33.dp)
                .clickable { onCalendarClick() }
        )
        // Ícone do Saco de Compras (Novo)
        Image(
            painter = painterResource(id = R.drawable.shoppingbag), // Nome do ficheiro XML que criaste
            contentDescription = "Loja",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(30.dp)
                .height(33.dp)
                //.clickable { onShopClick() }
        )
        Image(
            painter = painterResource(id = R.drawable.grocery), // Nome do ficheiro XML que criaste
            contentDescription = "Loja",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(30.dp)
                .height(33.dp)
            //.clickable { onShopClick() }
        )
        Image(
            painter = painterResource(id = R.drawable.dehaze), // Nome do ficheiro XML que criaste
            contentDescription = "Loja",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(30.dp)
                .height(33.dp)
            //.clickable { onShopClick() }
        )
    }
}