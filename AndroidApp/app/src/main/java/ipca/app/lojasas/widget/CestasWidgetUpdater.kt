package ipca.app.lojasas.widget

import android.content.Context
import android.content.Intent

/**
 * Helper para forçar atualização do widget a partir de qualquer ponto da app.
 *
 * Ex.: quando o utilizador faz login/logout.
 */
object CestasWidgetUpdater {

    /**
     * Pede ao [CestasWidgetProvider] para atualizar o header e a lista.
     */
    fun requestRefresh(context: Context) {
        val intent = Intent(context, CestasWidgetProvider::class.java).apply {
            action = CestasWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }
}
