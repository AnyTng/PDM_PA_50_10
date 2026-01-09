package ipca.app.lojasas.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Service obrigatório para widgets do tipo "collection" (ListView).
 */
class CestasWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CestasWidgetViewFactory(applicationContext, intent)
    }
}
