package ipca.app.lojasas.ui.apoiado.menu.profile

import ipca.app.lojasas.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions // Import necessário
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection // Import necessário
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager // Import necessário
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction // Import necessário
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.UserRole


@Composable
fun CreateProfileApoiadoView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: CreateProfileApoiadoViewModel = hiltViewModel()
    val state by viewModel.uiState
    val scrollState = rememberScrollState()

    // 1. Gestor de Foco
    val focusManager = LocalFocusManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createProfile {
                        navController.navigate(Screen.ApoiadoHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                containerColor = GreenSas,
                contentColor = WhiteColor,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = WhiteColor, modifier = Modifier.size(24.dp))
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
                .background(SurfaceLight)
                .imePadding() // 2. Adiciona espaço quando o teclado abre
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- CABEÇALHO ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = GreenSas,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Criar Conta",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSas,
                    fontFamily = IntroFontFamily
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = RedColor,
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
                    // 3. Configuração de Next
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
            FormSection(title = "Email e Password") {
                FormInput(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    placeholder = "Password",
                    isPassword = true,
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
            }

            // --- NIF / PASSAPORTE E MORADA ---
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

                // ÚLTIMO CAMPO: Ação Done para fechar o teclado
                FormInput(
                    value = state.codPostal,
                    onValueChange = { viewModel.onCodPostalChange(it) },
                    placeholder = "Codigo Postal (Ex: 1234-567)",
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
            viewModel.onRoleChange(UserRole.APOIADO)

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BlackColor,
            fontFamily = IntroFontFamily
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            thickness = 1.dp,
            color = GreyColor
        )
        content()
    }
}

// 4. FormInput Atualizado para suportar navegação
@Composable
fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next, // Default é Next
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 16.sp, color = BlackColor),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction // Define a tecla Enter
        ),
        keyboardActions = keyboardActions, // Define a ação
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true, // Impede criar novas linhas
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(WhiteColor, RoundedCornerShape(8.dp))
                    .border(1.dp, GreyColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = GreyColor, fontSize = 16.sp)
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun RoleRadioButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = GreenSas)
        )
        Text(text = text, fontSize = 16.sp, color = BlackColor)
    }
}

@Preview
@Composable
fun CreateProfilePreview() {
    CreateProfileApoiadoView(navController = rememberNavController())
}
