package ipca.app.lojasas.ui.apoiado.formulario

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.navigation.NavController
import ipca.app.lojasas.utils.Validators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteDataView(
    docId: String,
    onSuccess: () -> Unit,
    navController: NavController,
    // Se fornecido, mostra um aviso no topo indicando que o formulário está a ser pedido por expiração de validade.
    validadeExpiradaEm: java.util.Date? = null,
) {
    val viewModel: CompleteDataViewModel = hiltViewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(docId) {
        viewModel.loadData(docId)
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            viewModel.onDataNascimentoChange(calendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Impede escolher datas que resultem em idade < MIN_AGE_YEARS
    datePickerDialog.datePicker.maxDate = Validators.maxBirthDateForMinAgeMillis()

    var expandedNacionalidade by remember { mutableStateOf(false) }

    // --- LÓGICA DE VALIDAÇÃO ---
    // O botão só fica ativo se isto for true
    val isFormValid = remember(state) {
        val birthValid = state.dataNascimento != null &&
                Validators.isAgeAtLeast(state.dataNascimento!!, Validators.MIN_AGE_YEARS)

        val basicFieldsValid = state.nacionalidade.isNotBlank() &&
                birthValid &&
                state.relacaoIPCA.isNotBlank() &&
                state.necessidades.isNotEmpty()

        if (!basicFieldsValid) return@remember false

        if (state.relacaoIPCA == "Estudante") {
            // Se for estudante, exige curso, grau, e (se tiver bolsa) o valor
            val studentValid = state.curso.isNotBlank() && state.graoEnsino.isNotBlank()
            val bolsaValid = if (state.bolsaEstudos) {
                val parsed = state.valorBolsa.trim().replace(',', '.').toDoubleOrNull()
                parsed != null && parsed > 0
            } else true
            studentValid && bolsaValid
        } else {
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
            .imePadding()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Completar / Corrigir Dados",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GreenSas
        )
        Text("Verifique os seus dados e corrija o necessário.", color = GreyColor)

        // ✅ Aviso quando o formulário está a ser solicitado por expiração de validade.
        if (validadeExpiradaEm != null) {
            val formattedDate = remember(validadeExpiradaEm) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(validadeExpiradaEm)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = WarningBg),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrangeDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "A sua conta expirou no dia $formattedDate, volte a submeter o pedido",
                        color = WarningOrangeDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(color = GreenSas)
        }
        if (state.error != null) {
            Text(state.error!!, color = RedColor, fontSize = 14.sp)
        }

        // --- NACIONALIDADE ---
        val filteredNationalities = state.availableNationalities.filter {
            it.contains(state.nacionalidade, ignoreCase = true) &&
                    !it.equals(state.nacionalidade, ignoreCase = true)
        }

        ExposedDropdownMenuBox(
            expanded = expandedNacionalidade,
            onExpandedChange = { expandedNacionalidade = !expandedNacionalidade }
        ) {
            OutlinedTextField(
                value = state.nacionalidade,
                onValueChange = {
                    viewModel.onNacionalidadeChange(it)
                    expandedNacionalidade = true
                },
                label = { Text("Nacionalidade *") }, // Asterisco para indicar obrigatório
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = {
                    if (filteredNationalities.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNacionalidade)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSas,
                    focusedLabelColor = GreenSas,
                    cursorColor = GreenSas
                )
            )

            if (filteredNationalities.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expandedNacionalidade,
                    onDismissRequest = { expandedNacionalidade = false },
                    modifier = Modifier.background(WhiteColor)
                ) {
                    filteredNationalities.forEach { nation ->
                        DropdownMenuItem(
                            text = { Text(text = nation) },
                            onClick = {
                                viewModel.onNacionalidadeChange(nation)
                                expandedNacionalidade = false
                            }
                        )
                    }
                }
            }
        }

        // --- DATA DE NASCIMENTO ---
        val dateText = if (state.dataNascimento != null) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(state.dataNascimento)
        } else ""

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                label = { Text("Data de Nascimento *") },
                readOnly = true,
                trailingIcon = {
                    // Removemos o clickable daqui, pois a Box vai tratar de tudo
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

        if (state.dataNascimento != null && !Validators.isAgeAtLeast(state.dataNascimento!!, Validators.MIN_AGE_YEARS)) {
            Text(
                text = "Deve ter pelo menos ${Validators.MIN_AGE_YEARS} anos para submeter.",
                color = RedColor,
                fontSize = 12.sp
            )
        }

        Divider()

        // --- RELAÇÃO IPCA ---
        Text("Relação com o IPCA *:", fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Estudante", "Funcionário", "Docente").forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.relacaoIPCA == role,
                        onClick = { viewModel.onRelacaoChange(role) },
                        colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                    )
                    Text(role, fontSize = 14.sp)
                }
            }
        }

        // --- CAMPOS ESPECÍFICOS DE ESTUDANTE ---
        if (state.relacaoIPCA == "Estudante") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Curso
                    OutlinedTextField(
                        value = state.curso,
                        onValueChange = { viewModel.onCursoChange(it) },
                        label = { Text("Curso (ex: LESI) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grau
                    Text("Grau de Ensino *:")
                    Row {
                        listOf("CTeSP", "Licenciatura", "Mestrado").forEach { grau ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.graoEnsino == grau,
                                    onClick = { viewModel.onGraoChange(grau) },
                                    colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
                                )
                                Text(grau, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = LightGreyColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bolsa (AGORA AQUI DENTRO)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.bolsaEstudos,
                            onCheckedChange = { viewModel.onBolsaChange(it) },
                            colors = CheckboxDefaults.colors(checkedColor = GreenSas)
                        )
                        Text("Tem Bolsa de Estudos?")
                    }

                    if (state.bolsaEstudos) {
                        OutlinedTextField(
                            value = state.valorBolsa,
                            onValueChange = { viewModel.onValorBolsaChange(it) },
                            label = { Text("Valor da Bolsa (€) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }
        }

        Divider()

        // --- APOIO DE EMERGÊNCIA ---
        // (Apoio de emergência mantém-se acessível a todos, conforme lógica original)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.apoioEmergencia,
                onCheckedChange = { viewModel.onApoioEmergenciaChange(it) },
                colors = CheckboxDefaults.colors(checkedColor = GreenSas)
            )
            Text("Apoio de Emergência Social?")
        }

        Divider()

        // --- NECESSIDADES ---
        Text("Necessidades (Selecione pelo menos uma) *:", fontWeight = FontWeight.Bold)
        val options = listOf("Produtos Alimentares", "Produtos de higiene", "Produtos de Limpeza")
        options.forEach { item ->
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.toggleNecessidade(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.necessidades.contains(item),
                    onCheckedChange = { viewModel.toggleNecessidade(item) },
                    colors = CheckboxDefaults.colors(checkedColor = GreenSas)
                )
                Text(item)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- BOTÃO SUBMETER ---
        Button(
            onClick = {
                if (isFormValid) {
                    viewModel.submitData(docId) { onSuccess() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenSas,
                disabledContainerColor = LightGreyColor
            ),
            // Só ativa se não estiver a carregar E o formulário for válido
            enabled = !state.isLoading && isFormValid
        ) {
            if (state.isLoading) CircularProgressIndicator(color = WhiteColor)
            else Text("Guardar Dados")
        }
        Spacer(modifier = Modifier.height(50.dp))
        }
    }
}
