package ipca.app.lojasas.ui.apoiado

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme

@Composable
fun ApoiadoHomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "hello, Apoiado")
    }
}

@Preview(showBackground = true)
@Composable
private fun ApoiadoPreview() {
    LojaSocialIPCATheme {
        ApoiadoHomeScreen()
    }
}
