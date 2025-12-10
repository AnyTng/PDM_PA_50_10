package ipca.app.lojasas.ui.login

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import ipca.app.lojasas.R
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = GreenIPCA) // Fundo Verde Principal
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween, // Empurra o cartão branco para o fundo
        ) {

            // --- Secção do Logótipo ---
            Box(
                modifier = Modifier
                    .offset(x = 16.dp, y = 18.dp)
                    .width(244.dp)
                    .height(112.dp) // Arredondei ligeiramente para simplificar
            ) {
                // Certifique-se que a imagem "loginlogo" existe em res/drawable
                Image(
                    painter = painterResource(id = R.drawable.loginlogo),
                    contentDescription = "Logo IPCA",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
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

                // --- Título Bem-Vindo (Com a fonte Intro) ---
                Text(
                    text = "Bem-Vindo",
                    style = TextStyle(
                        fontSize = 20.sp,
                        // Certifique-se que o ficheiro intro.ttf está na pasta res/font/
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
                    keyboardType = KeyboardType.Email
                )

                // --- Campo de Password ---
                CustomFigmaInput(
                    value = uiState.password ?: "",
                    onValueChange = { viewModel.updatePassword(it) },
                    placeholder = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Botões ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão 1: "Não tens conta?" (Estilo: Outline Verde)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .border(width = 0.7.dp, color = GreenIPCA, shape = RoundedCornerShape(5.dp))
                            .background(color = WhiteColor, shape = RoundedCornerShape(5.dp))
                            .clickable {
                                // Navegar para o ecrã de registo
                            }
                    ) {
                        Text(text = "Não tens conta?", color = GreenIPCA, fontFamily = FontFamily(Font(R.font.introboldalt)),
                        )
                    }

                    // Botão 2: "Login" (Estilo: Filled Verde)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(color = GreenIPCA, shape = RoundedCornerShape(5.dp))
                            .clickable {
                                viewModel.login {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = WhiteColor, modifier = Modifier.height(20.dp).width(20.dp))
                        } else {
                            Text(text = "Login", color = WhiteColor, fontFamily = FontFamily(Font(R.font.introboldalt)),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// --- Componente de Input Personalizado (Estilo Figma) ---
@Composable
fun CustomFigmaInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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
                if (value.isEmpty()) {
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