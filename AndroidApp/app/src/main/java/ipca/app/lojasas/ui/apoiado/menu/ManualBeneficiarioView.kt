package ipca.app.lojasas.ui.apoiado.menu

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ipca.app.lojasas.R
import java.net.URLEncoder

@Composable
fun ManualBeneficiarioView() {
    val beneficiarioUrl = stringResource(R.string.manual_beneficiario_url)
    val viewerUrl = remember(beneficiarioUrl) {
        val encodedUrl = URLEncoder.encode(beneficiarioUrl, "UTF-8")
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
