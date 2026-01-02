package ipca.app.lojasas.ui.funcionario.pedidosurgentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

private val GreenSas = Color(0xFF094E33)
private val GreyBg = Color(0xFFF2F2F2)

@Composable
fun UrgentRequestsView(
    navController: NavController,
    viewModel: UrgentRequestsViewModel = viewModel()
) {
    val state by viewModel.uiState
    val dateFmt = rememberDateFormatter()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
            .padding(16.dp)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
            }

            state.pedidos.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sem pedidos urgentes.", color = Color.Gray)
                    state.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    state.error?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.pedidos, key = { it.id }) { pedido ->
                            PedidoUrgenteCard(
                                pedido = pedido,
                                dateFmt = dateFmt,
                                onNegar = {
                                    viewModel.negarPedido(pedido.id)
                                },
                                onAprovar = {
                                    // 1) Marcar como aprovado (Preparar_Apoio)
                                    // 2) Encaminhar para criar cesta (estilo imagem 1)
                                    viewModel.aprovarPedido(pedido.id) { ok ->
                                        if (ok) {
                                            navController.navigate(
                                                "createCestaUrgente/${pedido.id}/${pedido.numeroMecanografico}"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PedidoUrgenteCard(
    pedido: PedidoUrgenteItem,
    dateFmt: SimpleDateFormat,
    onNegar: () -> Unit,
    onAprovar: () -> Unit
) {
    val estadoNorm = pedido.estado.trim().lowercase(Locale.getDefault())
    val isAnalise = estadoNorm.isBlank() || estadoNorm == "analise" || estadoNorm == "em analise" || estadoNorm == "em_analise"

    val estadoLabel = when {
        estadoNorm == "preparar_apoio" || estadoNorm == "preparar apoio" -> "Preparar Apoio"
        estadoNorm == "negado" -> "Negado"
        estadoNorm == "analise" || estadoNorm == "em analise" || estadoNorm == "em_analise" || estadoNorm.isBlank() -> "Em Análise"
        else -> pedido.estado.ifBlank { "—" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apoiado: ${pedido.numeroMecanografico}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSas
                )
                Text(
                    text = estadoLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (estadoNorm == "negado") Color(0xFFB00020) else Color.Gray
                )
            }

            pedido.dataSubmissao?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Submetido: ${dateFmt.format(it)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = pedido.descricao.ifBlank { "(Sem descrição)" },
                fontSize = 14.sp,
                color = Color.Black
            )

            if (isAnalise) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNegar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                    ) {
                        Text("Negar")
                    }
                    Button(
                        onClick = onAprovar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Aprovar")
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    // MaterialTheme não é obrigatório aqui; mas mantém consistência
    MaterialTheme
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
