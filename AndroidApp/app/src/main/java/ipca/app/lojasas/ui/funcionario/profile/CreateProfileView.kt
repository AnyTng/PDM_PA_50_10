package ipca.app.lojasas.ui.funcionario.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.components.Footer
import ipca.app.lojasas.ui.components.FooterType
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

    Scaffold(
        topBar = {
            AppHeader(
                title = "Criar Perfil",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Footer(navController = navController, type = FooterType.FUNCIONARIO)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createProfile {
                        // Ação ao concluir com sucesso (ex: voltar atrás)
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
                .background(Color(0xFFF8F8F8)) // Fundo ligeiramente cinza
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Mensagem de Erro
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // --- IDENTIFICAÇÃO (Nº Mecanográfico) ---
            FormSection(title = "Identificação") {
                FormInput(
                    value = state.numMecanografico,
                    onValueChange = { viewModel.onNumMecanograficoChange(it) },
                    placeholder = "Nº Mecanográfico (ex: f12345)",
                    // IMPORTANTE: KeyboardType.Text para permitir letras ('f', 'a') + números
                    keyboardType = KeyboardType.Text
                )
            }

            // --- NOME ---
            FormSection(title = "Nome") {
                FormInput(
                    value = state.nome,
                    onValueChange = { viewModel.onNomeChange(it) },
                    placeholder = "Nome"
                )
            }

            // --- CONTACTO ---
            FormSection(title = "Contacto") {
                FormInput(
                    value = state.contacto,
                    onValueChange = { viewModel.onContactoChange(it) },
                    placeholder = "Contacto",
                    keyboardType = KeyboardType.Phone
                )
            }

            // --- EMAIL E PASSE ---
            FormSection(title = "Email e Passe") {
                FormInput(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    placeholder = "Mail",
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    placeholder = "Passe",
                    isPassword = true
                )
            }

            // --- NIF E MORADA ---
            FormSection(title = "NIF e Morada") {
                FormInput(
                    value = state.nif,
                    onValueChange = { viewModel.onNifChange(it) },
                    placeholder = "NIF",
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.morada,
                    onValueChange = { viewModel.onMoradaChange(it) },
                    placeholder = "Morada"
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormInput(
                    value = state.codPostal,
                    onValueChange = { viewModel.onCodPostalChange(it) },
                    placeholder = "CodPostal"
                )
            }

            // --- TIPO ---
            FormSection(title = "Tipo?") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    RoleRadioButton(
                        selected = state.role == UserRole.FUNCIONARIO,
                        text = "Gestor",
                        onClick = { viewModel.onRoleChange(UserRole.FUNCIONARIO) }
                    )
                    RoleRadioButton(
                        selected = state.role == UserRole.APOIADO,
                        text = "Apoiado",
                        onClick = { viewModel.onRoleChange(UserRole.APOIADO) }
                    )
                }
            }

            // Espaço extra para o FAB não tapar o conteúdo
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- COMPONENTES AUXILIARES PARA O ESTILO DO FIGMA ---

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
            color = Color.Black,
            fontFamily = IntroFontFamily
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            thickness = 1.dp,
            color = Color.Gray
        )
        content()
    }
}

@Composable
fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
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
            colors = RadioButtonDefaults.colors(selectedColor = GreenIPCA)
        )
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

@Preview
@Composable
fun CreateProfilePreview() {
    CreateProfileView(navController = rememberNavController())
}