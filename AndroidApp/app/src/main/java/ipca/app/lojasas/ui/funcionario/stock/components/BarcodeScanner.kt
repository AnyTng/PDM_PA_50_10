package ipca.app.lojasas.ui.funcionario.stock.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun rememberBarcodeScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val scanner = remember(activity) {
        activity?.let { GmsBarcodeScanning.getClient(it) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                onError("Permissão de câmara recusada.")
                return@rememberLauncherForActivityResult
            }
            if (scanner == null) {
                onError("Não foi possível iniciar o scanner.")
                return@rememberLauncherForActivityResult
            }
            startScan(scanner, onScanned, onError)
        }

    return scan@{
        if (scanner == null) {
            onError("Scanner não inicializado.")
            return@scan
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startScan(scanner, onScanned, onError)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun startScan(
    scanner: GmsBarcodeScanner,
    onScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    scanner.startScan()
        .addOnSuccessListener { barcode ->
            val value = barcode.rawValue?.trim()
            if (value.isNullOrBlank()) {
                onError("Código inválido.")
            } else {
                onScanned(value)
            }
        }
        .addOnCanceledListener { }
        .addOnFailureListener { e ->
            onError(e.message ?: "Erro ao ler código.")
        }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}