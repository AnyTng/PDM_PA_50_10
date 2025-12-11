package ipca.app.lojasas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ipca.app.lojasas.R // Importe o R do seu pacote
import ipca.app.lojasas.ui.login.LoginView
import ipca.app.lojasas.ui.theme.IntroFontFamily // Se definiu a fonte globalmente
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme

@Composable
fun Header(
    title: String = "TabName"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(color = Color(0xFF094E33))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, // <--- ADICIONAR ISTO
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 24.sp,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight(400),
                color = Color(0xFFFFFFFF),
            )
        )
        Image(
            painter = painterResource(id = R.drawable.sas),
            contentDescription = "image description",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderPreview() {
    LojaSocialIPCATheme {
        Header()
    }
}