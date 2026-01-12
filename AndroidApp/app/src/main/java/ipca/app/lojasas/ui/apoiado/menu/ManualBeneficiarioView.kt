package ipca.app.lojasas.ui.apoiado.menu

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ipca.app.lojasas.R
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.WhiteColor
import ipca.app.lojasas.utils.downloadManualPdf
import java.net.URLEncoder

@Composable
fun ManualBeneficiarioView() {
    val manualUrl = stringResource(R.string.manual_beneficiario_url)
    val viewerUrl = remember(manualUrl) {
        val encodedUrl = URLEncoder.encode(manualUrl, "UTF-8")
        "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Button(
            onClick = { downloadManualPdf(context, manualUrl, "Manual_beneficiario.pdf") },
            enabled = manualUrl.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenSas,
                contentColor = WhiteColor,
                disabledContainerColor = GreenSas.copy(alpha = 0.4f),
                disabledContentColor = WhiteColor.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.manual_download))
        }

        Spacer(modifier = Modifier.height(12.dp))

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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


