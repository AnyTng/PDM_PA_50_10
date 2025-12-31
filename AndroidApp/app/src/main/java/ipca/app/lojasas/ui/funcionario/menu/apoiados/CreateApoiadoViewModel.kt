package ipca.app.lojasas.ui.funcionario.menu.apoiados

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import ipca.app.lojasas.utils.Validators
import java.util.Date

data class CreateApoiadoState(
    // Dados de Conta
    var numMecanografico: String = "",
    var nome: String = "",
    var email: String = "",
    var password: String = "",

    // Identificação e Contactos
    var contacto: String = "",
    var documentNumber: String = "", // NIF ou Passaporte
    var documentType: String = "NIF",
    var nacionalidade: String = "",
    var dataNascimento: Long? = null,
    var morada: String = "",
    var codPostal: String = "",

    // Dados Sócio-Económicos
    var relacaoIPCA: String = "Estudante",
    var curso: String = "",
    var graoEnsino: String = "Licenciatura",
    var apoioEmergencia: Boolean = false,
    var bolsaEstudos: Boolean = false,
    var valorBolsa: String = "",
    var necessidades: List<String> = emptyList(),

    // Estado da UI
    var isLoading: Boolean = false,
    var isSuccess: Boolean = false,
    var error: String? = null
)

class CreateApoiadoViewModel : ViewModel() {

    var uiState = mutableStateOf(CreateApoiadoState())
        private set

    // DB usa a instância padrão (o Funcionário autenticado) para ter permissão de escrita
    private val db = Firebase.firestore

    // --- Setters para a View ---
    fun onNumMecanograficoChange(v: String) { uiState.value = uiState.value.copy(numMecanografico = v) }
    fun onNomeChange(v: String) { uiState.value = uiState.value.copy(nome = v) }
    fun onEmailChange(v: String) { uiState.value = uiState.value.copy(email = v) }
    fun onPasswordChange(v: String) { uiState.value = uiState.value.copy(password = v) }

    fun onContactoChange(v: String) { uiState.value = uiState.value.copy(contacto = v) }
    fun onDocumentNumberChange(v: String) { uiState.value = uiState.value.copy(documentNumber = v) }
    fun onDocumentTypeChange(v: String) { uiState.value = uiState.value.copy(documentType = v) }
    fun onNacionalidadeChange(v: String) { uiState.value = uiState.value.copy(nacionalidade = v) }
    fun onDataNascimentoChange(v: Long?) { uiState.value = uiState.value.copy(dataNascimento = v) }
    fun onMoradaChange(v: String) { uiState.value = uiState.value.copy(morada = v) }
    fun onCodPostalChange(v: String) { uiState.value = uiState.value.copy(codPostal = v) }

    fun onRelacaoChange(v: String) { uiState.value = uiState.value.copy(relacaoIPCA = v) }
    fun onCursoChange(v: String) { uiState.value = uiState.value.copy(curso = v) }
    fun onGraoChange(v: String) { uiState.value = uiState.value.copy(graoEnsino = v) }
    fun onApoioEmergenciaChange(v: Boolean) { uiState.value = uiState.value.copy(apoioEmergencia = v) }
    fun onBolsaChange(v: Boolean) { uiState.value = uiState.value.copy(bolsaEstudos = v) }
    fun onValorBolsaChange(v: String) { uiState.value = uiState.value.copy(valorBolsa = v) }

    fun toggleNecessidade(item: String) {
        val currentList = uiState.value.necessidades.toMutableList()
        if (currentList.contains(item)) currentList.remove(item) else currentList.add(item)
        uiState.value = uiState.value.copy(necessidades = currentList)
    }

    fun createApoiado() {
        val s = uiState.value

        // --- Validações (campos obrigatórios + regras de formato) ---
        val email = s.email.trim()
        val nome = s.nome.trim()
        val numMec = s.numMecanografico.trim()
        val morada = s.morada.trim()
        val nacionalidade = s.nacionalidade.trim()

        if (email.isBlank() || s.password.isBlank() || nome.isBlank() || numMec.isBlank()) {
            uiState.value = s.copy(error = "Preencha os campos obrigatórios (Email, Senha, Nome, Nº Mec.).")
            return
        }

        if (!Validators.isValidEmail(email)) {
            uiState.value = s.copy(error = "Email inválido.")
            return
        }

        if (s.password.length < 6) {
            uiState.value = s.copy(error = "A password deve ter pelo menos 6 caracteres.")
            return
        }

        if (!Validators.isValidMecanografico(numMec)) {
            uiState.value = s.copy(error = "O Nº Mecanográfico deve começar com uma letra seguida de números (ex: f12345).")
            return
        }

        val birthMillis = s.dataNascimento
        if (birthMillis == null) {
            uiState.value = s.copy(error = "A data de nascimento é obrigatória.")
            return
        }
        if (!Validators.isAgeAtLeast(birthMillis, Validators.MIN_AGE_YEARS)) {
            uiState.value = s.copy(error = "O apoiado deve ter pelo menos ${Validators.MIN_AGE_YEARS} anos.")
            return
        }

        if (nacionalidade.isBlank()) {
            uiState.value = s.copy(error = "A nacionalidade é obrigatória.")
            return
        }

        val contactoNorm = Validators.normalizePhonePT(s.contacto)
        if (contactoNorm == null) {
            uiState.value = s.copy(error = "Contacto inválido. Use 9 dígitos (ex: 912345678) ou +351...")
            return
        }

        if (morada.isBlank()) {
            uiState.value = s.copy(error = "A morada é obrigatória.")
            return
        }

        val codPostalNorm = Validators.normalizePostalCodePT(s.codPostal)
        if (codPostalNorm == null) {
            uiState.value = s.copy(error = "Código Postal inválido. Formato esperado: 1234-567")
            return
        }

        val documentNumberTrim = s.documentNumber.trim()
        if (documentNumberTrim.isBlank()) {
            uiState.value = s.copy(error = "O número de documento é obrigatório.")
            return
        }

        val normalizedDocNumber = if (s.documentType == "NIF") {
            if (!Validators.isValidNif(documentNumberTrim)) {
                uiState.value = s.copy(error = "NIF inválido.")
                return
            }
            Validators.normalizeNif(documentNumberTrim)!!
        } else {
            // Passaporte: regra simples (não vazio e tamanho mínimo)
            if (documentNumberTrim.length < 5) {
                uiState.value = s.copy(error = "Nº Passaporte inválido.")
                return
            }
            documentNumberTrim
        }

        if (s.relacaoIPCA == "Estudante" && s.curso.trim().isBlank()) {
            uiState.value = s.copy(error = "O curso é obrigatório para estudantes.")
            return
        }

        if (s.bolsaEstudos) {
            val bolsa = s.valorBolsa.trim().replace(',', '.').toDoubleOrNull()
            if (bolsa == null || bolsa <= 0) {
                uiState.value = s.copy(error = "O valor da bolsa deve ser um número válido (> 0).")
                return
            }
        }

        if (s.necessidades.isEmpty()) {
            uiState.value = s.copy(error = "Selecione pelo menos uma necessidade.")
            return
        }

        // Guarda valores normalizados no state (para persistência coerente)
        val normalizedState = s.copy(
            email = email,
            nome = nome,
            numMecanografico = numMec,
            contacto = contactoNorm,
            documentNumber = normalizedDocNumber,
            nacionalidade = nacionalidade,
            morada = morada,
            codPostal = codPostalNorm,
            valorBolsa = s.valorBolsa.trim()
        )

        uiState.value = normalizedState.copy(isLoading = true, error = null)

        // 1. Configurar uma App secundária para criar o user sem fazer login automático na App principal
        val secondaryAppName = "TempCreateUserApp"
        val secondaryApp = try {
            FirebaseApp.getInstance(secondaryAppName)
        } catch (e: IllegalStateException) {
            // Se não existir, inicializamos com as mesmas opções da app principal
            val currentApp = Firebase.app
            FirebaseApp.initializeApp(
                currentApp.applicationContext,
                currentApp.options,
                secondaryAppName
            )
        }

        // 2. Usar o Auth dessa app secundária
        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

        secondaryAuth.createUserWithEmailAndPassword(normalizedState.email, normalizedState.password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                // Opcional: Fazer logout da app secundária para limpar
                secondaryAuth.signOut()

                // 3. Guardar dados no Firestore usando a instância PRINCIPAL (db)
                //    que ainda tem o Funcionário logado.
                saveFirestoreData(uid)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro Auth: ${e.message}")
            }
    }

    private fun saveFirestoreData(uid: String) {
        val s = uiState.value

        // Preparar mapa de dados completo
        val apoiadoMap = hashMapOf(
            "uid" to uid,
            "role" to "Apoiado",
            "email" to s.email,
            "emailApoiado" to s.email,
            "nome" to s.nome,
            "numMecanografico" to s.numMecanografico,

            // Contactos e Identidade
            "contacto" to s.contacto,
            "documentNumber" to s.documentNumber,
            "documentType" to s.documentType,
            "nacionalidade" to s.nacionalidade,
            "dataNascimento" to Date(s.dataNascimento!!),
            "morada" to s.morada,
            "codPostal" to s.codPostal,

            // Dados Sócio-Económicos
            "relacaoIPCA" to s.relacaoIPCA,
            "curso" to if(s.relacaoIPCA == "Estudante") s.curso else "",
            "graoEnsino" to if(s.relacaoIPCA == "Estudante") s.graoEnsino else "",
            "apoioEmergenciaSocial" to s.apoioEmergencia,
            "bolsaEstudos" to s.bolsaEstudos,
            "valorBolsa" to if(s.bolsaEstudos) s.valorBolsa else "",
            "necessidade" to s.necessidades,

            // Estados da Conta
            "estadoConta" to "Aprovado",
            "faltaDocumentos" to false,
            "dadosIncompletos" to false,
            "mudarPass" to true,
            "role" to "Apoiado"
        )

        // Guardar na coleção 'apoiados'
        db.collection("apoiados").document(s.numMecanografico)
            .set(apoiadoMap)
            .addOnSuccessListener {
                // Criar registo na coleção 'users'
                //db.collection("users").document(uid).set(mapOf("role" to "Apoiado", "email" to s.email))

                uiState.value = s.copy(isLoading = false, isSuccess = true)
            }
            .addOnFailureListener { e ->
                uiState.value = s.copy(isLoading = false, error = "Erro BD: ${e.message}")
            }
    }
}