package ipca.app.lojasas.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import ipca.app.lojasas.R

fun downloadManualPdf(
    context: Context,
    url: String,
    fallbackFileName: String
) {
    if (url.isBlank()) {
        Toast.makeText(
            context,
            context.getString(R.string.manual_download_error),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val baseName = resolveManualFileName(url, fallbackFileName)
    val fileName = appendTimestamp(baseName)
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(baseName)
        .setDescription(context.getString(R.string.manual_download_description))
        .setMimeType("application/pdf")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    try {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(
            context,
            context.getString(R.string.manual_download_started),
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.manual_download_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun resolveManualFileName(url: String, fallbackFileName: String): String {
    val lastSegment = Uri.parse(url).lastPathSegment ?: return fallbackFileName
    val decoded = Uri.decode(lastSegment)
    val fileName = decoded.substringAfterLast('/')
    val candidate = if (fileName.isNotBlank()) fileName else fallbackFileName
    return if (candidate.endsWith(".pdf", ignoreCase = true)) candidate else "$candidate.pdf"
}

private fun appendTimestamp(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    val timestamp = System.currentTimeMillis()
    return if (dotIndex > 0) {
        val base = fileName.substring(0, dotIndex)
        val ext = fileName.substring(dotIndex)
        "${base}_$timestamp$ext"
    } else {
        "${fileName}_$timestamp.pdf"
    }
}
