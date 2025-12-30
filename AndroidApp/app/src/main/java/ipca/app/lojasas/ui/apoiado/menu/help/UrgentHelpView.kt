package ipca.app.lojasas.ui.apoiado.menu.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentHelpView(
    navController: NavController,
    numeroMecanografico: String // Recebe o nº mecanográfico do utilizador logado
) {
    val viewModel: UrgentHelpViewModel = viewModel()
    var descricao by remember { mutableStateOf("") }

    // Estados do ViewModel
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Pedido de Ajuda Urgente",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF094E33)
        )
        Text(
            text = "Descreva abaixo o que necessita com urgência.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição da necessidade") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 10
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.submitUrgentRequest(numeroMecanografico, descricao) {
                    navController.popBackStack() // Volta para trás ao terminar
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF094E33)),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White)
            else Text("Submeter Pedido")
        }
    }
}