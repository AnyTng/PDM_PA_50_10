package ipca.app.lojasas.ui.apoiado.formulario

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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteDataView(
    docId: String,
    onSuccess: () -> Unit,
    navController: NavController,
) {
    val viewModel: CompleteDataViewModel = viewModel()
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

    var expandedNacionalidade by remember { mutableStateOf(false) }

    // --- LÓGICA DE VALIDAÇÃO ---
    // O botão só fica ativo se isto for true
    val isFormValid = remember(state) {
        val basicFieldsValid = state.nacionalidade.isNotBlank() &&
                state.dataNascimento != null &&
                state.relacaoIPCA.isNotBlank() &&
                state.necessidades.isNotEmpty()

        if (!basicFieldsValid) return@remember false

        if (state.relacaoIPCA == "Estudante") {
            // Se for estudante, exige curso, grau, e (se tiver bolsa) o valor
            val studentValid = state.curso.isNotBlank() && state.graoEnsino.isNotBlank()
            val bolsaValid = if (state.bolsaEstudos) state.valorBolsa.isNotBlank() else true
            studentValid && bolsaValid
        } else {
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Completar / Corrigir Dados",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF094E33)
        )
        Text("Verifique os seus dados e corrija o necessário.", color = Color.Gray)

        if (state.isLoading) {
            CircularProgressIndicator(color = Color(0xFF094E33))
        }
        if (state.error != null) {
            Text(state.error!!, color = Color.Red, fontSize = 14.sp)
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
                    focusedBorderColor = Color(0xFF094E33),
                    focusedLabelColor = Color(0xFF094E33),
                    cursorColor = Color(0xFF094E33)
                )
            )

            if (filteredNationalities.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expandedNacionalidade,
                    onDismissRequest = { expandedNacionalidade = false },
                    modifier = Modifier.background(Color.White)
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

        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            label = { Text("Data de Nascimento *") },
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null, Modifier.clickable { datePickerDialog.show() })
            },
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        // --- RELAÇÃO IPCA ---
        Text("Relação com o IPCA *:", fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Estudante", "Funcionário", "Docente").forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.relacaoIPCA == role,
                        onClick = { viewModel.onRelacaoChange(role) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF094E33))
                    )
                    Text(role, fontSize = 14.sp)
                }
            }
        }

        // --- CAMPOS ESPECÍFICOS DE ESTUDANTE ---
        if (state.relacaoIPCA == "Estudante") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
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
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF094E33))
                                )
                                Text(grau, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bolsa (AGORA AQUI DENTRO)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.bolsaEstudos,
                            onCheckedChange = { viewModel.onBolsaChange(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF094E33))
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
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF094E33))
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
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF094E33))
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
                containerColor = Color(0xFF094E33),
                disabledContainerColor = Color.LightGray
            ),
            // Só ativa se não estiver a carregar E o formulário for válido
            enabled = !state.isLoading && isFormValid
        ) {
            if (state.isLoading) CircularProgressIndicator(color = Color.White)
            else Text("Guardar Dados")
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}