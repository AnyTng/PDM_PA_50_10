package ipca.app.lojasas.ui.funcionario.menu.apoiados

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CreateApoiadoView(
    navController: NavController,
    viewModel: CreateApoiadoViewModel = viewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- ESTADO DO POP-UP DE AVISO ---
    // Começa a true para aparecer logo ao abrir
    var showInfoDialog by remember { mutableStateOf(true) }

    // Configuração DatePicker
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

    // Efeitos de Sucesso/Erro
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Apoiado criado com sucesso (Estado: Aprovado)!", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // --- DIALOG DE AVISO ---
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = GreenSas) },
            title = {
                Text(text = "Atenção", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Apenas registe apoiados que foram aprovados com a documentação em papel!\n\nApós a submissão, o estado do utilizador será automaticamente definido como 'Aprovado'.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                ) {
                    Text("Entendido")
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            AppHeader(title = "Registar Apoiado", showBack = true, onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F2F2))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- 1. DADOS DE CONTA ---
                SectionCard("Dados de Conta") {
                    CustomTextField(value = state.nome, onValueChange = { viewModel.onNomeChange(it) }, label = "Nome Completo")
                    CustomTextField(value = state.email, onValueChange = { viewModel.onEmailChange(it) }, label = "Email", keyboardType = KeyboardType.Email)
                    CustomTextField(value = state.numMecanografico, onValueChange = { viewModel.onNumMecanograficoChange(it) }, label = "Nº Mecanográfico (ID)")

                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password Provisória") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenSas, focusedLabelColor = GreenSas)
                    )
                }

                // --- 2. IDENTIFICAÇÃO E CONTACTOS ---
                SectionCard("Identificação e Contactos") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Tipo Documento
                        Column(Modifier.weight(0.4f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = state.documentType == "NIF", onClick = { viewModel.onDocumentTypeChange("NIF") }, colors = RadioButtonDefaults.colors(selectedColor = GreenSas))
                                Text("NIF", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = state.documentType == "Passaporte", onClick = { viewModel.onDocumentTypeChange("Passaporte") }, colors = RadioButtonDefaults.colors(selectedColor = GreenSas))
                                Text("Pass.", fontSize = 12.sp)
                            }
                        }
                        // Numero Documento
                        CustomTextField(
                            value = state.documentNumber,
                            onValueChange = { viewModel.onDocumentNumberChange(it) },
                            label = "Nº Documento",
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    CustomTextField(value = state.nacionalidade, onValueChange = { viewModel.onNacionalidadeChange(it) }, label = "Nacionalidade")

                    // --- DATA DE NASCIMENTO ---
                    val dateText = state.dataNascimento?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: ""

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            label = { Text("Data de Nascimento") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.Black,
                                disabledTrailingIconColor = Color.Gray
                            )
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .clickable { datePickerDialog.show() }
                        )
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    CustomTextField(value = state.contacto, onValueChange = { viewModel.onContactoChange(it) }, label = "Telemóvel", keyboardType = KeyboardType.Phone)
                    CustomTextField(value = state.morada, onValueChange = { viewModel.onMoradaChange(it) }, label = "Morada Completa")
                    CustomTextField(value = state.codPostal, onValueChange = { viewModel.onCodPostalChange(it) }, label = "Código Postal")
                }

                // --- 3. DADOS ACADÉMICOS / PROFISSIONAIS ---
                SectionCard("Vínculo com o IPCA") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

                    if (state.relacaoIPCA == "Estudante") {
                        CustomTextField(value = state.curso, onValueChange = { viewModel.onCursoChange(it) }, label = "Curso")

                        Text("Grau de Ensino:", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("CTeSP", "Licenciatura", "Mestrado").forEach { grau ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = state.graoEnsino == grau, onClick = { viewModel.onGraoChange(grau) }, colors = RadioButtonDefaults.colors(selectedColor = GreenSas))
                                    Text(grau, fontSize = 12.sp)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.bolsaEstudos, onCheckedChange = { viewModel.onBolsaChange(it) }, colors = CheckboxDefaults.colors(checkedColor = GreenSas))
                            Text("Bolsa de Estudos?")
                        }
                        if (state.bolsaEstudos) {
                            CustomTextField(value = state.valorBolsa, onValueChange = { viewModel.onValorBolsaChange(it) }, label = "Valor Bolsa (€)", keyboardType = KeyboardType.Number)
                        }
                    }
                }

                // --- 4. NECESSIDADES E EMERGÊNCIA ---
                SectionCard("Apoio e Necessidades") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.apoioEmergencia, onCheckedChange = { viewModel.onApoioEmergenciaChange(it) }, colors = CheckboxDefaults.colors(checkedColor = GreenSas))
                        Text("Apoio de Emergência Social?", fontWeight = FontWeight.Bold)
                    }
                    //Text("(Se marcado, não exige documentos imediatos)", fontSize = 12.sp, color = Color.Gray)

                    Divider(Modifier.padding(vertical = 8.dp))
                    Text("Tipos de Cabaz:", fontWeight = FontWeight.Bold)
                    val options = listOf("Produtos Alimentares", "Produtos de higiene", "Produtos de Limpeza")
                    options.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNecessidade(item) }) {
                            Checkbox(
                                checked = state.necessidades.contains(item),
                                onCheckedChange = { viewModel.toggleNecessidade(item) },
                                colors = CheckboxDefaults.colors(checkedColor = GreenSas)
                            )
                            Text(item)
                        }
                    }
                }

                // --- BOTÃO CONFIRMAR ---
                Button(
                    onClick = { viewModel.createApoiado() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) CircularProgressIndicator(color = Color.White)
                    else Text("Criar Conta Aprovada", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

// --- Componentes Auxiliares Locais ---
// (SectionCard e CustomTextField mantêm-se iguais)
@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSas)
            HorizontalDivider(color = GreenSas.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenSas,
            focusedLabelColor = GreenSas,
            cursorColor = GreenSas
        )
    )
}