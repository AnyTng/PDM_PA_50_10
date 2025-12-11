package ipca.app.lojasas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ipca.app.lojasas.R // Importe o R do seu pacote
import ipca.app.lojasas.ui.theme.IntroFontFamily // Se definiu a fonte globalmente

@Composable
fun Header(
    title: String = "TabName" // Pode mudar o titulo dinamicamente
) {
    Row(
        modifier = Modifier
            .fillMaxWidth() // Ocupa a largura toda
            .height(62.dp)
            .background(color = Color(0xFF094E33))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center, // Centraliza o texto
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 24.sp,
                fontFamily = IntroFontFamily, // Usa a fonte carregada
                fontWeight = FontWeight(400),
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
            )
        )
    }
}