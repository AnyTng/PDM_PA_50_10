package ipca.app.lojasas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.MainActivity
import ipca.app.lojasas.R
import ipca.app.lojasas.data.UserRole
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Widget 4x3 que lista as próximas cestas do utilizador.
 *
 * - Apoiado: próximas cestas a receber
 * - Funcionário/Admin: próximas cestas a doar
 */
class CestasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CestasWidgetProvider::class.java))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            // Também tentamos atualizar os textos do header/empty.
            ids.forEach { id -> updateWidget(context, manager, id) }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Atualização imediata (layout + bindings). Os textos finais do header/empty são
        // ajustados logo a seguir num thread de background.
        val views = buildRemoteViews(
            context = context,
            appWidgetId = appWidgetId,
            subtitleText = context.getString(R.string.widget_subtitle_loading),
            emptyText = context.getString(R.string.widget_empty_login)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)

        // Ajuste do header/empty de acordo com sessão/role.
        EXECUTOR.execute {
            try {
                val (subtitle, empty) = resolveHeaderTexts(context)
                val updated = buildRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    subtitleText = subtitle,
                    emptyText = empty
                )
                appWidgetManager.updateAppWidget(appWidgetId, updated)
            } catch (e: Exception) {
                Log.w(TAG, "Widget: falha ao atualizar header", e)
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        subtitleText: String,
        emptyText: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cestas)

        // Header texts
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
        views.setTextViewText(R.id.widget_subtitle, subtitleText)
        views.setTextViewText(R.id.widget_empty, emptyText)

        // Collection adapter
        val serviceIntent = Intent(context, CestasWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // Clique no header: abre a app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header_open, openAppPending)

        // Refresh manual (ícone no header)
        val refreshIntent = Intent(context, CestasWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        val refreshPending = PendingIntent.getBroadcast(
            context,
            20_000 + appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

        // Clique em item: abre detalhes da cesta
        val templateIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_FROM_WIDGET, true)
        }
        val templatePending = PendingIntent.getActivity(
            context,
            10_000 + appWidgetId,
            templateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, templatePending)

        return views
    }

    private fun resolveHeaderTexts(context: Context): Pair<String, String> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            return context.getString(R.string.widget_subtitle_login) to
                context.getString(R.string.widget_empty_login)
        }

        val email = user.email?.trim().orEmpty()
        if (email.isBlank()) {
            return context.getString(R.string.widget_subtitle_login) to
                context.getString(R.string.widget_empty_login)
        }

        // Tenta cache primeiro (mesma cache usada no MainActivity)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val cached = prefs.getString("role_${email}", null)
        val cachedRole = cached?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
        val role = cachedRole ?: fetchRoleFromFirestore(email)

        if (role != null && cachedRole == null) {
            prefs.edit().putString("role_${email}", role.name).apply()
        }

        return when (role) {
            UserRole.APOIADO -> context.getString(R.string.widget_subtitle_receber) to
                context.getString(R.string.widget_empty_receber)
            UserRole.FUNCIONARIO, UserRole.ADMIN -> context.getString(R.string.widget_subtitle_doar) to
                context.getString(R.string.widget_empty_doar)
            null -> context.getString(R.string.widget_subtitle_loading) to
                context.getString(R.string.widget_empty_login)
        }
    }

    private fun fetchRoleFromFirestore(email: String): UserRole? {
        val firestore = FirebaseFirestore.getInstance()
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) return null

        // Funcionários (inclui Admin)
        val funcionarios = Tasks.await(
            firestore.collection("funcionarios")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get(),
            10,
            TimeUnit.SECONDS
        )
        if (!funcionarios.isEmpty) {
            val doc = funcionarios.documents.first()
            val roleStr = doc.getString("role")
            return if (roleStr != null && roleStr.equals("Admin", ignoreCase = true)) {
                UserRole.ADMIN
            } else {
                UserRole.FUNCIONARIO
            }
        }

        // Apoiados
        val apoiados = Tasks.await(
            firestore.collection("apoiados")
                .whereEqualTo("emailApoiado", normalizedEmail)
                .limit(1)
                .get(),
            10,
            TimeUnit.SECONDS
        )
        if (!apoiados.isEmpty) {
            return UserRole.APOIADO
        }

        return null
    }

    companion object {
        private const val TAG = "CestasWidgetProvider"

        private val EXECUTOR = Executors.newSingleThreadExecutor()

        const val ACTION_REFRESH = "ipca.app.lojasas.action.REFRESH_WIDGET"
        // (mantido por compatibilidade caso queiras reutilizar noutro sítio)
        const val EXTRA_CESTA_ID = "ipca.app.lojasas.extra.CESTA_ID"
    }
}
