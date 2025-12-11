package ipca.app.lojasas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
        // Define o espaçamento automático entre os elementos
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.calendarmonth),
            contentDescription = "Calendário",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(30.dp)
                .height(33.dp) // Arredondei para simplificar
                .clickable { onCalendarClick() }
        )

        Image(
            painter = painterResource(id = R.drawable.shoppingbag),
            contentDescription = "Saco",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp) // Simplifiquei width/height
        )

        Image(
            painter = painterResource(id = R.drawable.grocery),
            contentDescription = "Mercearia",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.dehaze),
            contentDescription = "Menu",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FooterPreview() {
    Footer()
}

