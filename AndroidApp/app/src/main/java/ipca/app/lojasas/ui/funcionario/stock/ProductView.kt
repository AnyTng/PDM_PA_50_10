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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductView(
    navController: NavController,
    productId: String,
    viewModel: ProductViewModel = hiltViewModel()
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
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = GreenSas,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.error != null -> {
                Text(
                    text = state.error ?: "Erro desconhecido",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                state.product?.let { prod ->
                    val reference = Date()
                    val statusLabel = prod.displayStatus(reference)
                    val isDisponivel = prod.isBaseAvailable() && !prod.isExpired(reference)
                    ProductViewContent(
                        id = prod.id,
                        nomeProduto = prod.nomeProduto,
                        estadoLabel = statusLabel,
                        isDisponivel = isDisponivel,
                        categoria = prod.categoria,
                        subCategoria = prod.subCategoria,
                        marca = prod.marca,
                        tamanhoValor = prod.tamanhoValor,   // <-- Double? (ou Double)
                        tamanhoUnidade = prod.tamanhoUnidade,
                        validade = prod.validade,
                        campanha = prod.campanha,
                        parceiroExternoNome = prod.parceiroExternoNome,
                        doado = prod.doado,
                        codBarras = prod.codBarras,
                        descProduto = prod.descProduto,
                        onEditClick = { id ->
                            navController.navigate(Screen.StockProductEdit.createRoute(id))
                        }
                    )
                }
            }
        }
    }
}

/**
 * UI "pura" (stateless) -> dá para Preview sem ViewModel / Firestore.
 */
@Composable
private fun ProductViewContent(
    id: String,
    nomeProduto: String,
    estadoLabel: String,
    isDisponivel: Boolean,
    categoria: String?,
    subCategoria: String,
    marca: String?,
    tamanhoValor: Double?,          // <-- agora Double?
    tamanhoUnidade: String?,
    validade: Date?,
    campanha: String?,
    parceiroExternoNome: String?,
    doado: String?,
    codBarras: String?,
    descProduto: String?,
    onEditClick: (String) -> Unit
) {
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
                        text = nomeProduto,
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = if (isDisponivel) GreenSas.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = estadoLabel,
                            color = if (isDisponivel) GreenSas else Color.Red,
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
                        value = categoria ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        icon = Icons.Default.Style,
                        label = "Tipo",
                        value = subCategoria,
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
                        value = marca ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        icon = Icons.Default.Scale,
                        label = "Tamanho",
                        value = formatTamanho(tamanhoValor, tamanhoUnidade),
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
                        value = formatDate(validade),
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        icon = Icons.Default.Star,
                        label = "Campanha",
                        value = campanha ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 5. Linha 4: Origem (Parceiro ou Doador)
                val origemTexto = when {
                    !parceiroExternoNome.isNullOrBlank() -> parceiroExternoNome
                    !doado.isNullOrBlank() -> doado
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
                    Spacer(modifier = Modifier.weight(1f))
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // 6. Código de Barras
                DetailItemHorizontal(
                    icon = Icons.Default.PhotoCamera,
                    label = "Código de Barras",
                    value = codBarras ?: "Sem código"
                )

                // 7. Descrição
                if (!descProduto.isNullOrBlank()) {
                    Column {
                        Text(
                            text = "Descrição",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descProduto,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Botão de Editar
        Button(
            onClick = { onEditClick(id) },
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

// ---------- Componentes Auxiliares ----------

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

/**
 * Formata Double -> "1" em vez de "1,0" e "1,5" conforme o locale.
 * Se for null, devolve "-".
 */
private fun formatTamanho(valor: Double?, unidade: String?): String {
    if (valor == null) return "-"

    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    val base = nf.format(valor)
    return if (unidade.isNullOrBlank()) base else "$base $unidade"
}

// ---------- PREVIEWS (Android Studio) ----------

@Preview(showBackground = true, name = "Produto - Disponível")
@Composable
private fun ProductViewPreview_Disponivel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
    ) {
        ProductViewContent(
            id = "p1",
            nomeProduto = "Leite UHT Meio-Gordo",
            estadoLabel = ProductStatus.AVAILABLE.displayLabel,
            isDisponivel = true,
            categoria = "Alimentar",
            subCategoria = "Laticínios",
            marca = "Mimosa",
            tamanhoValor = 1.0,
            tamanhoUnidade = "L",
            validade = Date(),
            campanha = "Cesta Solidária",
            parceiroExternoNome = "Mercadona",
            doado = null,
            codBarras = "5601234567890",
            descProduto = "Embalagem intacta. Armazenar em local fresco e seco.",
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Produto - Reservado")
@Composable
private fun ProductViewPreview_Indisponivel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
    ) {
        ProductViewContent(
            id = "p2",
            nomeProduto = "Arroz Carolino",
            estadoLabel = ProductStatus.RESERVED.displayLabel,
            isDisponivel = false,
            categoria = "Alimentar",
            subCategoria = "Cereais",
            marca = "Nacional",
            tamanhoValor = 1.5,
            tamanhoUnidade = "kg",
            validade = null,
            campanha = "-",
            parceiroExternoNome = null,
            doado = "Doação Particular",
            codBarras = null,
            descProduto = "Saco selado. Sem observações.",
            onEditClick = {}
        )
    }
}
