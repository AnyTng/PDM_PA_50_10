package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductView(
    navController: NavController,
    productId: String,
    viewModel: ProductViewModel = viewModel()
) {
    val state by viewModel.uiState

    LaunchedEffect(productId) {
        viewModel.observeProduct(productId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = GreenSas,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.error != null) {
            Text(
                text = state.error ?: "Erro desconhecido",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            state.product?.let { prod ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cartão principal com os detalhes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. Cabeçalho do Produto
                            Column {
                                Text(
                                    text = prod.nomeProduto,
                                    fontFamily = IntroFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = if (prod.estadoProduto == "Disponivel") GreenSas.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = prod.estadoProduto ?: "Disponivel",
                                        color = if (prod.estadoProduto == "Disponivel") GreenSas else Color.Red,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.5f))

                            // 2. Linha 1: Categoria e Tipo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailItem(
                                    icon = Icons.Default.Category,
                                    label = "Categoria",
                                    value = prod.categoria ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                DetailItem(
                                    icon = Icons.Default.Style, // Mudei para Style para diferenciar da Categoria
                                    label = "Tipo",
                                    value = prod.subCategoria,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Linha 2: Marca e Tamanho
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailItem(
                                    icon = Icons.Default.Label,
                                    label = "Marca",
                                    value = prod.marca ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                DetailItem(
                                    icon = Icons.Default.Scale,
                                    label = "Tamanho",
                                    value = "${prod.tamanhoValor ?: ""} ${prod.tamanhoUnidade ?: ""}",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 4. Linha 3: Validade e Campanha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailItem(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Validade",
                                    value = formatDate(prod.validade),
                                    modifier = Modifier.weight(1f)
                                )
                                DetailItem(
                                    icon = Icons.Default.Star,
                                    label = "Campanha",
                                    value = prod.campanha ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 5. Linha 4: Origem
                            // Lógica para mostrar Parceiro ou Doador
                            val origemTexto = when {
                                !prod.parceiroExternoNome.isNullOrBlank() -> prod.parceiroExternoNome
                                !prod.doado.isNullOrBlank() -> prod.doado
                                else -> "-"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailItem(
                                    icon = Icons.Default.Person,
                                    label = "Origem",
                                    value = origemTexto,
                                    modifier = Modifier.weight(1f)
                                )
                                // Espaço vazio à direita para alinhar, se quiseres podes pôr outro campo aqui
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.5f))

                            // 6. Código de Barras
                            DetailItemHorizontal(
                                icon = Icons.Default.PhotoCamera,
                                label = "Código de Barras",
                                value = prod.codBarras ?: "Sem código"
                            )

                            // 7. Descrição
                            if (!prod.descProduto.isNullOrBlank()) {
                                Column {
                                    Text(
                                        text = "Descrição",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = prod.descProduto,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Botão de Editar
                    Button(
                        onClick = {
                            navController.navigate("stockProductEdit/${prod.id}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenSas,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editar Produto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Componentes Auxiliares
@Composable
fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GreenSas,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "-" },
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailItemHorizontal(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GreenSas.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GreenSas
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontFamily = IntroFontFamily
            )
        }
    }
}

private fun formatDate(date: Date?): String {
    return if (date != null) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    } else {
        "-"
    }
}