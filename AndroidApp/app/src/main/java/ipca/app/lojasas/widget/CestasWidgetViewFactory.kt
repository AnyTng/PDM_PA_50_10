package ipca.app.lojasas.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
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
    val estado: String,
    val apoiadoId: String = "",
    val apoiadoNome: String? = null
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
    private var currentRole: UserRole? = null

    private val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    override fun onCreate() {
        // no-op
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun onDataSetChanged() {
        items.clear()
        currentRole = null

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

        currentRole = role

        val fetched = try {
            when (role) {
                UserRole.APOIADO -> fetchUpcomingCestasForApoiado(uid)
                // Para Funcionário/Admin mostramos as próximas cestas a doar (todas),
                // de forma consistente com a listagem de cestas do backoffice.
                UserRole.FUNCIONARIO, UserRole.ADMIN -> fetchUpcomingCestasForStaff()
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
        val desc = when (currentRole) {
            UserRole.APOIADO -> context.getString(R.string.widget_item_cesta) + " • " + estadoLabel
            UserRole.FUNCIONARIO, UserRole.ADMIN -> {
                val nome = item.apoiadoNome?.trim().orEmpty()
                val recipient = when {
                    nome.isNotBlank() && item.apoiadoId.isNotBlank() -> "$nome (${item.apoiadoId})"
                    nome.isNotBlank() -> nome
                    item.apoiadoId.isNotBlank() -> item.apoiadoId
                    else -> context.getString(R.string.widget_item_apoiado_unknown)
                }
                recipient + " • " + estadoLabel
            }
            null -> context.getString(R.string.widget_item_cesta) + " • " + estadoLabel
        }
        rv.setTextViewText(R.id.widget_item_desc, desc)

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

    /**
     * Para Funcionário/Admin: mostrar as próximas cestas a doar.
     *
     * Nota: não filtramos por funcionarioID porque na app o backoffice mostra "todas as cestas".
     */
    private fun fetchUpcomingCestasForStaff(): List<WidgetCestaItem> {
        // Pequena margem para incluir cestas "de hoje" mesmo que a hora já tenha passado.
        val now = Date()
        val lowerBound = Date(now.time - 24L * 60L * 60L * 1000L)

        // Tentamos uma query "leve" (filtrada por data). Caso falhe por algum motivo
        // (ex.: dados antigos sem campo dataAgendada, ou inconsistências), fazemos fallback
        // para uma query mais ampla para não deixar o widget vazio.
        val snap = try {
            Tasks.await(
                firestore.collection("cestas")
                    .whereGreaterThanOrEqualTo("dataAgendada", lowerBound)
                    .orderBy("dataAgendada")
                    .limit(50)
                    .get(),
                15,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            Log.w(TAG_WIDGET, "Widget: fallback query (cestas)", e)
            Tasks.await(
                firestore.collection("cestas")
                    .limit(80)
                    .get(),
                15,
                TimeUnit.SECONDS
            )
        }

        val rawItems = snap.documents.mapNotNull { cestaDoc ->
            val estado = cestaDoc.getString("estadoCesta")?.trim().orEmpty()
            if (isCancelled(estado) || isMissed(estado) || isCompleted(estado)) {
                return@mapNotNull null
            }

            val data = snapshotDate(cestaDoc, "dataAgendada")
                ?: snapshotDate(cestaDoc, "dataRecolha")
                ?: return@mapNotNull null

            val apoiadoId = cestaDoc.getString("apoiadoID")?.trim().orEmpty()

            WidgetCestaItem(
                cestaId = cestaDoc.id,
                dataEntrega = data,
                estado = estado,
                apoiadoId = apoiadoId
            )
        }.sortedBy { it.dataEntrega }

        // Para o widget, não precisamos de uma lista enorme.
        val top = rawItems.take(20)

        // Fetch nomes dos apoiados (batch por chunks de 10) para mostrar "para quem".
        val nameMap = fetchApoiadoNames(top.map { it.apoiadoId })
        return top.map { item ->
            item.copy(apoiadoNome = nameMap[item.apoiadoId])
        }
    }

    private fun fetchApoiadoNames(apoiadoIds: List<String>): Map<String, String> {
        val ids = apoiadoIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        val chunks = ids.chunked(10)
        chunks.forEach { chunk ->
            val snap = Tasks.await(
                firestore.collection("apoiados")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get(),
                10,
                TimeUnit.SECONDS
            )
            snap.documents.forEach { doc ->
                val nome = doc.getString("nome")?.trim().orEmpty()
                if (nome.isNotBlank()) {
                    result[doc.id] = nome
                }
            }
        }
        return result
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
