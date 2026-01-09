package ipca.app.lojasas.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.MainActivity
import ipca.app.lojasas.R
import ipca.app.lojasas.data.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG_WIDGET = "CestasWidget"

/**
 * Dados mínimos que o widget precisa para renderizar a lista.
 */
private data class WidgetCestaItem(
    val cestaId: String,
    val dataEntrega: Date,
    val estado: String
)

/**
 * Factory responsável por alimentar o ListView do widget.
 *
 * Nota: onDataSetChanged() é chamado num thread de background, por isso aqui podemos
 * fazer chamadas blocking (Tasks.await) de forma controlada.
 */
class CestasWidgetViewFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val items = mutableListOf<WidgetCestaItem>()

    private val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    override fun onCreate() {
        // no-op
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun onDataSetChanged() {
        items.clear()

        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG_WIDGET, "Widget: utilizador sem sessão")
            return
        }

        val email = user.email?.trim().orEmpty()
        val uid = user.uid.trim()
        if (email.isBlank() || uid.isBlank()) {
            return
        }

        val role = resolveUserRole(email)
        if (role == null) {
            Log.w(TAG_WIDGET, "Widget: role não encontrado para $email")
            return
        }

        val fetched = try {
            when (role) {
                UserRole.APOIADO -> fetchUpcomingCestasForApoiado(uid)
                UserRole.FUNCIONARIO, UserRole.ADMIN -> fetchUpcomingCestasForFuncionario(uid)
            }
        } catch (e: Exception) {
            Log.e(TAG_WIDGET, "Widget: erro ao carregar cestas", e)
            emptyList()
        }

        items.addAll(fetched.sortedBy { it.dataEntrega })
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return RemoteViews(context.packageName, R.layout.widget_cesta_item)
        }

        val item = items[position]

        val rv = RemoteViews(context.packageName, R.layout.widget_cesta_item)
        rv.setTextViewText(R.id.widget_item_date, dateFormatter.format(item.dataEntrega))

        val estadoLabel = item.estado.trim().ifBlank { "-" }
        rv.setTextViewText(
            R.id.widget_item_desc,
            context.getString(R.string.widget_item_cesta) + " • " + estadoLabel
        )

        // Click do item: passamos a cestaId via Fill-In Intent.
        val fillIn = Intent().apply {
            putExtra(MainActivity.EXTRA_OPEN_CESTA_ID, item.cestaId)
        }
        rv.setOnClickFillInIntent(R.id.widget_item_root, fillIn)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private fun resolveUserRole(email: String): UserRole? {
        val key = "role_${email.trim()}"
        val cached = prefs.getString(key, null)
        if (!cached.isNullOrBlank()) {
            runCatching { return UserRole.valueOf(cached) }
        }

        val role = fetchRoleFromFirestore(email)
        if (role != null) {
            prefs.edit().putString(key, role.name).apply()
        }
        return role
    }

    private fun fetchRoleFromFirestore(email: String): UserRole? {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) return null

        // 1) Funcionários (inclui Admin)
        val funcionariosTask = firestore.collection("funcionarios")
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()

        val funcionarios = Tasks.await(funcionariosTask, 10, TimeUnit.SECONDS)
        if (!funcionarios.isEmpty) {
            val doc = funcionarios.documents.first()
            val roleStr = doc.getString("role")
            return if (roleStr != null && roleStr.equals("Admin", ignoreCase = true)) {
                UserRole.ADMIN
            } else {
                UserRole.FUNCIONARIO
            }
        }

        // 2) Apoiados
        val apoiadosTask = firestore.collection("apoiados")
            .whereEqualTo("emailApoiado", normalizedEmail)
            .limit(1)
            .get()

        val apoiados = Tasks.await(apoiadosTask, 10, TimeUnit.SECONDS)
        if (!apoiados.isEmpty) {
            return UserRole.APOIADO
        }

        return null
    }

    private fun fetchUpcomingCestasForApoiado(uid: String): List<WidgetCestaItem> {
        val apoiadoTask = firestore.collection("apoiados")
            .whereEqualTo("uid", uid.trim())
            .limit(1)
            .get()

        val apoiadoSnap = Tasks.await(apoiadoTask, 10, TimeUnit.SECONDS)
        val doc = apoiadoSnap.documents.firstOrNull() ?: return emptyList()

        val docId = doc.id
        val numMec = doc.getString("numMecanografico")
            ?: doc.getString("numeroMecanografico")
            ?: ""

        val keys = listOf(docId, numMec)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (keys.isEmpty()) return emptyList()

        val query = if (keys.size == 1) {
            firestore.collection("cestas").whereEqualTo("apoiadoID", keys.first())
        } else {
            firestore.collection("cestas").whereIn("apoiadoID", keys)
        }

        val snap = Tasks.await(query.get(), 10, TimeUnit.SECONDS)
        return snap.documents.mapNotNull { cestaDoc ->
            val estado = cestaDoc.getString("estadoCesta")?.trim().orEmpty()
            if (isCancelled(estado) || isMissed(estado) || isCompleted(estado)) {
                return@mapNotNull null
            }
            val data = snapshotDate(cestaDoc, "dataRecolha")
                ?: snapshotDate(cestaDoc, "dataAgendada")
                ?: return@mapNotNull null

            WidgetCestaItem(
                cestaId = cestaDoc.id,
                dataEntrega = data,
                estado = estado
            )
        }
    }

    private fun fetchUpcomingCestasForFuncionario(uid: String): List<WidgetCestaItem> {
        val funcionarioTask = firestore.collection("funcionarios")
            .whereEqualTo("uid", uid.trim())
            .limit(1)
            .get()

        val funcSnap = Tasks.await(funcionarioTask, 10, TimeUnit.SECONDS)
        val funcDoc = funcSnap.documents.firstOrNull() ?: return emptyList()
        val funcionarioId = funcDoc.id

        val cestasTask = firestore.collection("cestas")
            .whereEqualTo("funcionarioID", funcionarioId)
            .get()

        val snap = Tasks.await(cestasTask, 10, TimeUnit.SECONDS)
        return snap.documents.mapNotNull { cestaDoc ->
            val estado = cestaDoc.getString("estadoCesta")?.trim().orEmpty()
            if (isCancelled(estado) || isMissed(estado) || isCompleted(estado)) {
                return@mapNotNull null
            }
            val data = snapshotDate(cestaDoc, "dataAgendada")
                ?: snapshotDate(cestaDoc, "dataRecolha")
                ?: return@mapNotNull null

            WidgetCestaItem(
                cestaId = cestaDoc.id,
                dataEntrega = data,
                estado = estado
            )
        }
    }

    private fun snapshotDate(doc: DocumentSnapshot, field: String): Date? {
        return doc.getTimestamp(field)?.toDate() ?: doc.getDate(field)
    }

    // Mesma lógica usada no ApoiadoViewModel
    private fun isCompleted(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        return eLower.contains("entreg") ||
            // Evitar falso positivo em "nao levantou".
            eLower.contains("levantad") ||
            eLower.contains("conclu") ||
            eLower.contains("finaliz")
    }

    private fun isMissed(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        val normalized = eLower.replace('_', ' ')
        return normalized.contains("não levant") ||
            normalized.contains("nao levant") ||
            normalized.contains("faltou")
    }

    private fun isCancelled(estado: String): Boolean {
        val eLower = estado.trim().lowercase(Locale.getDefault())
        return eLower.contains("cancel")
    }
}
