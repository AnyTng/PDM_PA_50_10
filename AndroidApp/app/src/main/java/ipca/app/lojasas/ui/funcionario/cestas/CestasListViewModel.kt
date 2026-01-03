package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import java.util.Date

data class CestaItem(
    val id: String,
    val apoiadoId: String,
    val funcionarioId: String,
    val dataAgendada: Date? = null,
    val dataRecolha: Date? = null,
    val estado: String = "",
    val faltas: Int = 0,
    val origem: String? = null,
    val tipoApoio: String? = null
)

data class CestasListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cestas: List<CestaItem> = emptyList()
)

class CestasListViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var listener: ListenerRegistration? = null

    var uiState = mutableStateOf(CestasListState())
        private set

    init {
        listenCestas()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    private fun listenCestas() {
        uiState.value = uiState.value.copy(isLoading = true, error = null)

        listener?.remove()
        listener = db.collection("cestas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty().map { doc ->
                    val dataAgendada = doc.getTimestamp("dataAgendada")?.toDate() ?: (doc.get("dataAgendada") as? Date)
                    val dataRecolha = doc.getTimestamp("dataRecolha")?.toDate() ?: (doc.get("dataRecolha") as? Date)
                    val faltas = (doc.getLong("faltas") ?: 0L).toInt()
                    CestaItem(
                        id = doc.id,
                        apoiadoId = doc.getString("apoiadoID")?.trim().orEmpty(),
                        funcionarioId = doc.getString("funcionarioID")?.trim().orEmpty(),
                        dataAgendada = dataAgendada,
                        dataRecolha = dataRecolha,
                        estado = doc.getString("estadoCesta")?.trim().orEmpty(),
                        faltas = faltas,
                        origem = doc.getString("origem"),
                        tipoApoio = doc.getString("tipoApoio")
                    )
                }

                uiState.value = uiState.value.copy(isLoading = false, cestas = list)
            }
    }

    fun cancelarCesta(cestaId: String) {
        val now = Date()
        val cestaRef = db.collection("cestas").document(cestaId)

        // Ao cancelar: cesta -> Cancelada e produtos voltam a Disponível.
        db.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            // Libertar produtos reservados
            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                val prodSnap = txn.get(prodRef)
                if (prodSnap.exists()) {
                    val reservaId = prodSnap.getString("cestaReservaId")?.trim().orEmpty()
                    val estado = prodSnap.getString("estadoProduto")?.trim().orEmpty()

                    val podeLibertar = reservaId.isBlank() || reservaId == cestaId ||
                            estado.equals("Reservados", ignoreCase = true)

                    if (podeLibertar) {
                        txn.update(
                            prodRef,
                            mapOf(
                                "estadoProduto" to "Disponivel",
                                "cestaReservaId" to FieldValue.delete(),
                                "reservadoEm" to FieldValue.delete()
                            )
                        )
                    }
                }
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Cancelada",
                    "dataCancelada" to now
                )
            )

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
    }

    fun marcarEntregue(cesta: CestaItem) {
        val now = Date()
        val cestaRef = db.collection("cestas").document(cesta.id)

        // Ao marcar como entregue: cesta -> Entregue e produtos -> Entregue.
        db.runTransaction { txn ->
            val cestaSnap = txn.get(cestaRef)
            val produtoIds = (cestaSnap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to "Entregue",
                        "cestaEntregaId" to cesta.id,
                        "entregueEm" to now,
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            txn.update(
                cestaRef,
                mapOf(
                    "estadoCesta" to "Entregue",
                    "dataRecolha" to now,
                    "dataEntregue" to now
                )
            )

            // Atualiza o último levantamento do apoiado (para ordenação na seleção)
            if (cesta.apoiadoId.isNotBlank()) {
                txn.update(db.collection("apoiados").document(cesta.apoiadoId), mapOf("ultimoLevantamento" to now))
            }

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
    }

    /**
     * Reagendar SEM contar como falta.
     */
    fun reagendarEntrega(cesta: CestaItem, novaData: Date) {
        db.collection("cestas").document(cesta.id)
            .update(
                mapOf(
                    "dataAgendada" to novaData,
                    "dataRecolha" to novaData,
                    "dataReagendada" to Date()
                )
            )
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    fun reagendarComFalta(cesta: CestaItem, novaData: Date) {
        val novasFaltas = cesta.faltas + 1
        val novoEstado = if (novasFaltas >= 3) "Nao_Levantou" else "Agendada"

        val updates = mutableMapOf<String, Any>(
            "faltas" to novasFaltas,
            "estadoCesta" to novoEstado,
            "dataAgendada" to novaData,
            // Mantemos também dataRecolha para compatibilidade com o Home do apoiado
            "dataRecolha" to novaData,
            "dataUltimaFalta" to Date()
        )

        db.collection("cestas").document(cesta.id)
            .update(updates)
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
            }
    }

    /**
     * 3ª falta: não permite escolher dia, passa para Nao_Levantou.
     * Também devolve os produtos para Disponível para não ficarem presos em "Reservados".
     */
    fun registarTerceiraFaltaSemReagendar(cesta: CestaItem) {
        val now = Date()
        val cestaRef = db.collection("cestas").document(cesta.id)

        db.runTransaction { txn ->
            val snap = txn.get(cestaRef)
            val produtoIds = (snap.get("produtos") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            val novasFaltas = cesta.faltas + 1
            txn.update(
                cestaRef,
                mapOf(
                    "faltas" to novasFaltas,
                    "estadoCesta" to "Nao_Levantou",
                    "dataUltimaFalta" to now
                )
            )

            // Libertar produtos
            produtoIds.forEach { pid ->
                val prodRef = db.collection("produtos").document(pid)
                txn.update(
                    prodRef,
                    mapOf(
                        "estadoProduto" to "Disponivel",
                        "cestaReservaId" to FieldValue.delete(),
                        "reservadoEm" to FieldValue.delete()
                    )
                )
            }

            null
        }.addOnFailureListener { e ->
            uiState.value = uiState.value.copy(error = e.message)
        }
    }
}
