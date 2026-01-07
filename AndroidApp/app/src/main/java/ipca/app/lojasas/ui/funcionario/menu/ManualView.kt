package ipca.app.lojasas.ui.funcionario.menu

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

private const val ADMIN_MANUAL_URL =
    "https://firebasestorage.googleapis.com/v0/b/app-loja-social-ipca.firebasestorage.app/o/DocumentationHelp%2FManual_administrador.pdf?alt=media&token=50be5a82-c7ef-441a-aeb8-f96d9fa415a7"
private const val COLAB_MANUAL_URL =
    "https://firebasestorage.googleapis.com/v0/b/app-loja-social-ipca.firebasestorage.app/o/DocumentationHelp%2FManual_colaborador.pdf?alt=media&token=307cecb1-8ebf-4bad-9090-b46f7480170f"

@Composable
fun ManualView() {
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("funcionarios")
            .whereEqualTo("uid", user.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val role = docs.documents[0].getString("role") ?: ""
                    isAdmin = role.equals("Admin", ignoreCase = true)
                }
            }
    }

    val viewerUrl = remember(isAdmin) {
        val url = if (isAdmin) ADMIN_MANUAL_URL else COLAB_MANUAL_URL
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
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
