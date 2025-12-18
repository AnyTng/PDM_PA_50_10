package ipca.app.lojasas.ui.funcionario.menu.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.theme.IntroFontFamily
import ipca.app.lojasas.ui.login.GreenIPCA

@Composable
fun CreateProfileView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: CreateProfileViewModel = viewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Opcional: Adicionar Header se não existir no NavHost
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createProfile {
                        navController.popBackStack()
                    }
                },
                containerColor = GreenIPCA,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // --- IDENTIFICAÇÃO ---
            FormSection(title = "Identificação") {
                FormInput(
                    value = state.numMecanografico,
                    onValueChange = { viewModel.onNumMecanograficoChange(it) },
                    placeholder = "Nº Mecanográfico (ex: f12345)",
                    keyboardType = KeyboardType.Text,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            // --- NOME ---
            FormSection(title = "Nome") {
                FormInput(
                    value = state.nome,
                    onValueChange = { viewModel.onNomeChange(it) },
                    placeholder = "Nome",
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            // --- CONTACTO ---
            FormSection(title = "Contacto") {
                FormInput(
                    value = state.contacto,
                    onValueChange = { viewModel.onContactoChange(it) },
                    placeholder = "Contacto",
                    keyboardType = KeyboardType.Phone,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            // --- EMAIL E PASSE ---
            FormSection(title = "Email e Passe") {
                FormInput(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    placeholder = "Mail",
                    keyboardType = KeyboardType.Email,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    placeholder = "Passe",
                    isPassword = true,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            // --- DOCUMENTO E MORADA ---
            FormSection(title = "Documento e Morada") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleRadioButton(
                        selected = state.documentType == "NIF",
                        text = "NIF",
                        onClick = { viewModel.onDocumentTypeChange("NIF") }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    RoleRadioButton(
                        selected = state.documentType == "Passaporte",
                        text = "Passaporte",
                        onClick = { viewModel.onDocumentTypeChange("Passaporte") }
                    )
                }

                FormInput(
                    value = state.documentNumber,
                    onValueChange = { viewModel.onDocumentNumberChange(it) },
                    placeholder = if (state.documentType == "NIF") "NIF" else "Nº Passaporte",
                    keyboardType = if (state.documentType == "NIF") KeyboardType.Number else KeyboardType.Text,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.morada,
                    onValueChange = { viewModel.onMoradaChange(it) },
                    placeholder = "Morada",
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(8.dp))

                FormInput(
                    value = state.codPostal,
                    onValueChange = { viewModel.onCodPostalChange(it) },
                    placeholder = "CodPostal",
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // REMOVIDA A SECÇÃO "TIPO?" (Role Selection)

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ... (Manter os componentes auxiliares FormSection, FormInput, RoleRadioButton iguais)
@Composable
fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = IntroFontFamily)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), thickness = 1.dp, color = Color.Gray)
        content()
    }
}

@Composable
fun FormInput(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, isPassword: Boolean = false, imeAction: ImeAction = ImeAction.Next, keyboardActions: KeyboardActions = KeyboardActions.Default) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
                innerTextField()
            }
        }
    )
}

@Composable
fun RoleRadioButton(selected: Boolean, text: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = GreenIPCA))
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}