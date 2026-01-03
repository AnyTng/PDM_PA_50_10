package ipca.app.lojasas.ui.funcionario.cestas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GreyBg = Color(0xFFF2F2F2)

@Composable
fun CestaDetailsView(
    cestaId: String,
    viewModel: CestaDetailsViewModel = viewModel()
) {
    val state by viewModel.uiState
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(cestaId) {
        viewModel.observeCesta(cestaId)
    }

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
            state.error != null -> {
                Text(
                    text = state.error ?: "Erro desconhecido",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.cesta?.let { cesta ->
                        DetailsCard(title = "Detalhes da Cesta") {
                            DetailRow("ID", cesta.id)
                            DetailRow("Estado", cesta.estadoLabel())
                            DetailRow("Origem", cesta.origem ?: "-")
                            DetailRow("Tipo de Apoio", cesta.tipoApoio ?: "-")
                            DetailRow("Faltas", cesta.faltas.toString())
                            DetailRow("Produtos", cesta.produtosCount.toString())
                            DetailRow("Agendada", formatDateTime(cesta.dataAgendada, dateFmt))
                            DetailRow("Recolha", formatDateTime(cesta.dataRecolha, dateFmt))
                            DetailRow("Reagendada", formatDateTime(cesta.dataReagendada, dateFmt))
                            DetailRow("Entregue", formatDateTime(cesta.dataEntregue, dateFmt))
                            DetailRow("Cancelada", formatDateTime(cesta.dataCancelada, dateFmt))
                            DetailRow("Ultima falta", formatDateTime(cesta.dataUltimaFalta, dateFmt))
                            DetailRow("Funcionario", cesta.funcionarioId.ifBlank { "-" })
                            cesta.observacoes?.takeIf { it.isNotBlank() }?.let {
                                DetailRow("Observacoes", it)
                            }
                        }
                    }

                    DetailsCard(title = "Produtos da Cesta") {
                        when {
                            state.isLoadingProdutos -> {
                                Text(
                                    text = "A carregar produtos...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            state.produtos.isNotEmpty() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.produtos.forEachIndexed { index, produto ->
                                        ProdutoRow(produto = produto)
                                        if (index != state.produtos.lastIndex) {
                                            HorizontalDivider(color = Color(0xFFE6E6E6))
                                        }
                                    }
                                }
                            }
                            state.produtosError != null -> {
                                Text(
                                    text = state.produtosError ?: "Erro ao carregar produtos.",
                                    fontSize = 12.sp,
                                    color = Color.Red
                                )
                            }
                            else -> {
                                Text(
                                    text = "Sem produtos.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (!state.isLoadingProdutos && state.produtosError != null && state.produtos.isNotEmpty()) {
                            Text(
                                text = state.produtosError ?: "Erro ao carregar produtos.",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }

                        if (!state.isLoadingProdutos && state.produtosMissingIds.isNotEmpty()) {
                            Text(
                                text = "Produtos nao encontrados: ${state.produtosMissingIds.size}",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                    }

                    DetailsCard(title = "Apoiado") {
                        when {
                            state.apoiado != null -> {
                                val apoiado = state.apoiado!!
                                DetailRow("Nome", apoiado.nome)
                                DetailRow("Email", apoiado.email)
                                DetailRow("Contacto", apoiado.contacto)
                                DetailRow("Documento", apoiado.documento)
                                DetailRow("Morada", apoiado.morada)
                                DetailRow("Nacionalidade", apoiado.nacionalidade)
                            }
                            state.apoiadoError != null -> {
                                Text(
                                    text = state.apoiadoError ?: "Erro ao carregar apoiado.",
                                    fontSize = 12.sp,
                                    color = Color.Red
                                )
                            }
                            else -> {
                                Text(
                                    text = "A carregar dados do apoiado...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GreenSas
            )
            HorizontalDivider(color = GreenSas.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value.ifBlank { "-" }, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProdutoRow(produto: Product) {
    val categoria = produto.categoria?.takeIf { it.isNotBlank() } ?: "-"
    val subCategoria = produto.subCategoria.takeIf { it.isNotBlank() } ?: "-"
    val marca = produto.marca?.takeIf { it.isNotBlank() } ?: "-"
    val tamanho = formatTamanho(produto.tamanhoValor, produto.tamanhoUnidade)
    val estado = produto.estadoProduto?.takeIf { it.isNotBlank() } ?: "-"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = produto.nomeProduto.ifBlank { produto.id },
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Categoria: $categoria / $subCategoria",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = "Marca: $marca | Tamanho: $tamanho | Estado: $estado",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

private fun formatDateTime(date: Date?, formatter: SimpleDateFormat): String {
    return date?.let { formatter.format(it) } ?: "-"
}

private fun formatTamanho(valor: Double?, unidade: String?): String {
    if (valor == null) return "-"
    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    val base = nf.format(valor)
    return if (unidade.isNullOrBlank()) base else "$base $unidade"
}

private fun CestaDetails.estadoLabel(): String {
    val n = estado.trim().lowercase(Locale.getDefault())
    return when {
        n == "entregue" -> "Entregue"
        n == "agendada" -> "Agendada"
        n == "nao_levantou" || n == "nao levantou" -> "Nao levantou"
        n == "cancelada" -> "Cancelada"
        n.isBlank() -> "-"
        else -> estado
    }
}
