package ipca.app.lojasas.ui.apoiado.menu

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

private const val BENEFICIARIO_MANUAL_URL =
    "https://firebasestorage.googleapis.com/v0/b/app-loja-social-ipca.firebasestorage.app/o/DocumentationHelp%2FManual_beneficiario.pdf?alt=media&token=57ac4cd0-e695-40b3-96b9-49d3df529966"

@Composable
fun ManualBeneficiarioView() {
    val viewerUrl = remember {
        val encodedUrl = URLEncoder.encode(BENEFICIARIO_MANUAL_URL, "UTF-8")
        "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = WebViewClient()
                    loadUrl(viewerUrl)
                }
            },
            update = { webView ->
                if (webView.url != viewerUrl) {
                    webView.loadUrl(viewerUrl)
                }
            }
        )
    }
}
