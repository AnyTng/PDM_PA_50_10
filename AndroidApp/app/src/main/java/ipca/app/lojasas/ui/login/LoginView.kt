package ipca.app.lojasas.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog // Adicionado
import androidx.compose.material3.Button // Adicionado
import androidx.compose.material3.ButtonDefaults // Adicionado
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField // Adicionado
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Adicionado
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.app.lojasas.R
import ipca.app.lojasas.data.UserRole
import ipca.app.lojasas.ui.theme.LojaSocialIPCATheme

// Definição das Cores do Figma
val GreenIPCA = Color(0xFF094E33)
val WhiteColor = Color(0xFFFFFFFF)
val InputBackground = Color(0x0DC6DBD3)
val InputBorder = Color(0x808B8B8B)

@Composable
fun LoginView(
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.uiState
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    // Estado para controlar a visibilidade do Dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val logoResId = remember {
        context.resources.getIdentifier("sas_white", "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.loginlogo
    }

    val performLogin: () -> Unit = {
        if (!uiState.isLoading) {
            viewModel.login { role ->
                val destination = when (role) {
                    UserRole.APOIADO -> "apoiadoHome"
                    UserRole.FUNCIONARIO -> "funcionarioHome"
                }
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = GreenIPCA)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Secção do Logótipo + Ilustração ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lswhitecircle),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 38.dp, y = 138.dp)
                        .width(463.dp)
                        .height(463.dp)
                        .alpha(0.4f)
                )

                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "Logo do SAS IPCA",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 20.dp)
                        .width(275.dp)
                        .height(100.dp)
                )
            }

            // --- Secção do Cartão Branco (Bottom Sheet) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WhiteColor,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .padding(start = 20.dp, top = 31.dp, end = 20.dp, bottom = 31.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Bem-Vindo",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.introboldalt)),
                        fontWeight = FontWeight(700),
                        color = Color(0xFF000000),
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                // --- Campo de Email ---
                CustomFigmaInput(
                    value = uiState.email ?: "",
                    onValueChange = { viewModel.updateEmail(it) },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    )
                )

                // --- Campo de Password ---
                CustomFigmaInput(
                    value = uiState.password ?: "",
                    onValueChange = { viewModel.updatePassword(it) },
                    placeholder = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    focusRequester = passwordFocusRequester,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            performLogin()
                        }
                    )
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Botões e Link de Recuperação ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão "Não tens conta?"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(width = 0.7.dp, color = GreenIPCA, shape = RoundedCornerShape(5.dp))
                                .background(color = WhiteColor, shape = RoundedCornerShape(5.dp))
                                .clickable {
                                    navController.navigate("createProfileApoiado")
                                }
                        ) {
                            Text(
                                text = "Não tens conta?",
                                color = GreenIPCA,
                                fontFamily = FontFamily(Font(R.font.introboldalt)),
                            )
                        }

                        // Botão "Login"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(color = GreenIPCA, shape = RoundedCornerShape(5.dp))
                                .clickable {
                                    performLogin()
                                }
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = WhiteColor, modifier = Modifier.height(20.dp).width(20.dp))
                            } else {
                                Text(
                                    text = "Login",
                                    color = WhiteColor,
                                    fontFamily = FontFamily(Font(R.font.introboldalt)),
                                )
                            }
                        }
                    }

                    // Link de Recuperação
                    Text(
                        text = "Recuperar Palavra-passe",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily(Font(R.font.introboldalt)),
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .padding(top=10.dp)
                            .clickable {
                                // Abre o Dialog ao clicar
                                showForgotPasswordDialog = true
                            }
                    )
                }
            }
        }

        // --- Pop-up de Recuperação de Senha ---
        if (showForgotPasswordDialog) {
            // Variável local para editar o email no dialog (inicia com o que já estava escrito)
            var dialogEmail by remember { mutableStateOf(uiState.email ?: "") }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = {
                    Text(
                        text = "Recuperar Palavra-passe",
                        color = GreenIPCA,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Insira o seu email para receber as instruções de recuperação.")
                        OutlinedTextField(
                            value = dialogEmail,
                            onValueChange = { dialogEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (dialogEmail.isNotEmpty()) {
                                // Atualiza o email no ViewModel e chama a função de recuperar
                                viewModel.updateEmail(dialogEmail)
                                viewModel.recoverPassword { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                }
                            } else {
                                Toast.makeText(context, "Por favor, insira um email.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenIPCA)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                color = WhiteColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirmar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = WhiteColor,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun CustomFigmaInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val focusModifier = (if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }).onFocusChanged { focusState ->
        isFocused = focusState.isFocused
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        modifier = focusModifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(width = 0.67.dp, color = InputBorder, shape = RoundedCornerShape(5.dp))
                    .background(color = InputBackground, shape = RoundedCornerShape(5.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty() && !isFocused) {
                    Text(
                        text = placeholder,
                        style = TextStyle(color = Color.Gray, fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.introboldalt)),
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    LojaSocialIPCATheme {
        LoginView()
    }
}