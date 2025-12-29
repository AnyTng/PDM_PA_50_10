package ipca.app.lojasas.ui.funcionario.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.Date

// Tipos de Eventos para colorir no calendário
enum class EventType {
    CAMPAIGN_START,
    CAMPAIGN_END,
    PRODUCT_EXPIRY,
    BASKET_DELIVERY
}

// Modelo único para o Calendário
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: Date,
    val type: EventType,
    val description: String = ""
)

data class CalendarState(
    val showMandatoryPasswordChange: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val docId: String = "",
    val events: List<CalendarEvent> = emptyList(), // Lista de todos os eventos
    val selectedDate: Date = Date() // Data selecionada no UI
)

class CalendarViewModel : ViewModel() {

    var uiState = mutableStateOf(CalendarState())
        private set

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        checkMudarPassStatus()
        loadAllEvents()
    }

    // --- CARREGAMENTO DE EVENTOS ---
    fun loadAllEvents() {
        uiState.value = uiState.value.copy(isLoading = true)

        val eventsList = mutableListOf<CalendarEvent>()

        // Usamos um contador simples ou Tasks do Firebase para esperar pelas 3 chamadas
        // Aqui faremos sequencial para simplificar a lógica sem Coroutines complexas

        // 1. Carregar Campanhas
        db.collection("campanha").get().addOnSuccessListener { campSnap ->
            campSnap.documents.forEach { doc ->
                val nome = doc.getString("nomeCampanha") ?: "Campanha"
                val inicio = doc.getTimestamp("dataInicio")?.toDate()
                val fim = doc.getTimestamp("dataFim")?.toDate()

                if (inicio != null) {
                    eventsList.add(CalendarEvent(doc.id + "_start", "Início: $nome", inicio, EventType.CAMPAIGN_START))
                }
                if (fim != null) {
                    eventsList.add(CalendarEvent(doc.id + "_end", "Fim: $nome", fim, EventType.CAMPAIGN_END))
                }
            }
            loadProducts(eventsList)
        }.addOnFailureListener { loadProducts(eventsList) }
    }

    private fun loadProducts(currentList: MutableList<CalendarEvent>) {
        // 2. Carregar Validade de Produtos
        db.collection("produtos").get().addOnSuccessListener { prodSnap ->
            prodSnap.documents.forEach { doc ->
                val validade = doc.getTimestamp("validade")?.toDate()
                val nome = doc.getString("nomeProduto") ?: "Produto"
                val marca = doc.getString("marca") ?: ""

                // Só adicionamos se tiver validade e se não estiver "Vendido/Doado" (opcional)
                if (validade != null) {
                    currentList.add(
                        CalendarEvent(
                            id = doc.id,
                            title = "Validade: $nome ($marca)",
                            date = validade,
                            type = EventType.PRODUCT_EXPIRY
                        )
                    )
                }
            }
            loadCestas(currentList)
        }.addOnFailureListener { loadCestas(currentList) }
    }

    private fun loadCestas(currentList: MutableList<CalendarEvent>) {
        // 3. Carregar Entregas de Cestas
        db.collection("cestas").get().addOnSuccessListener { cestaSnap ->
            cestaSnap.documents.forEach { doc ->
                // Baseado na imagem image_7bc068.png
                val dataRecolha = doc.getTimestamp("dataRecolha")?.toDate()
                val apoiadoId = doc.getString("apoiadoID") ?: "Desconhecido"
                val estado = doc.getString("estadoCesta") ?: ""

                if (dataRecolha != null) {
                    currentList.add(
                        CalendarEvent(
                            id = doc.id,
                            title = "Entrega Cesta ($estado)",
                            description = "Apoiado: $apoiadoId",
                            date = dataRecolha,
                            type = EventType.BASKET_DELIVERY
                        )
                    )
                }
            }

            // Finalizar: Atualizar Estado
            uiState.value = uiState.value.copy(
                isLoading = false,
                events = currentList
            )
        }.addOnFailureListener {
            // Mesmo com erro nas cestas, mostra o que já carregou
            uiState.value = uiState.value.copy(
                isLoading = false,
                events = currentList,
                error = "Erro parcial ao carregar eventos."
            )
        }
    }

    fun selectDate(date: Date) {
        uiState.value = uiState.value.copy(selectedDate = date)
    }

    // --- LÓGICA DE PASSWORD (Mantida do original) ---
    private fun checkMudarPassStatus() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("funcionarios").whereEqualTo("uid", user.uid).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val mudarPass = doc.getBoolean("mudarPass") ?: false
                        uiState.value = uiState.value.copy(showMandatoryPasswordChange = mudarPass, docId = doc.id)
                    }
                }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        val state = uiState.value
        if (user != null && email != null && state.docId.isNotEmpty()) {
            uiState.value = state.copy(isLoading = true, error = null)
            val credential = EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential).addOnSuccessListener {
                user.updatePassword(newPass).addOnSuccessListener {
                    db.collection("funcionarios").document(state.docId).update("mudarPass", false)
                        .addOnSuccessListener {
                            uiState.value = state.copy(isLoading = false, showMandatoryPasswordChange = false)
                            onSuccess()
                        }
                }.addOnFailureListener { e -> uiState.value = state.copy(isLoading = false, error = e.message) }
            }.addOnFailureListener { uiState.value = state.copy(isLoading = false, error = "Senha incorreta.") }
        }
    }
}