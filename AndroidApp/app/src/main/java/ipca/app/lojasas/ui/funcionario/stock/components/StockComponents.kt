package ipca.app.lojasas.ui.funcionario.stock.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.displayStatus
import ipca.app.lojasas.ui.funcionario.stock.isAvailableForCount
import ipca.app.lojasas.ui.funcionario.stock.isExpiredVisible
import ipca.app.lojasas.ui.funcionario.stock.isReserved
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val StockBackground = Color(0xFFF2F2F2)
val StockAccent = Color(0xFFC9A27B)
val StockReserved = Color(0xFFB26A00)
val StockExpired = Color(0xFFE53935)

@Composable
fun StockSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pesquisar",
    showFilter: Boolean = false,
    filterMenuContent: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    showSort: Boolean = false,
    sortMenuContent: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    showExport: Boolean = false,
    onExportClick: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GreenSas
                    )
                },
                placeholder = { Text(placeholder, color = GreenSas.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSas,
                    unfocusedBorderColor = GreenSas,
                    focusedLabelColor = GreenSas,
                    unfocusedLabelColor = GreenSas,
                    cursorColor = GreenSas,
                    focusedLeadingIconColor = GreenSas,
                    unfocusedLeadingIconColor = GreenSas,
                    focusedTextColor = GreenSas,
                    unfocusedTextColor = GreenSas,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            if (showFilter) {
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = Color(0xFF333333))
                    }
                    filterMenuContent(showFilterMenu) { showFilterMenu = false }
                }
            }
            if (showSort) {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color(0xFF333333))
                    }
                    sortMenuContent(showSortMenu) { showSortMenu = false }
                }
            }
            if (showExport) {
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Exportar", tint = Color(0xFF333333))
                }
            }
        }
    }
}

@Immutable
data class StockGroupUi(
    val name: String,
    val availableCount: Int
)

@Composable
fun StockGroupCard(
    group: StockGroupUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, GreenSas)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = IntroFontFamily
                ),
                color = GreenSas,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = GreenSas,
                contentColor = Color.White,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = availableText(group.availableCount),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Abrir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StockExpiredSummaryCard(
    expiredCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StockExpired),
        border = BorderStroke(2.dp, StockExpired)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fora de validade",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = IntroFontFamily
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = Color.White,
                contentColor = StockExpired,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expiredCountText(expiredCount),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Abrir",
                        tint = StockExpired,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StockProductGroupCard(
    product: Product,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reference = Date()
    val statusLabel = product.displayStatus(reference)
    val statusColor = when {
        product.isExpiredVisible(reference) -> StockExpired
        product.isReserved() -> StockReserved
        product.isAvailableForCount(reference) -> GreenSas
        else -> Color(0xFF9E9E9E)
    }
    val barcodeBitmap by produceState<Bitmap?>(initialValue = null, key1 = product.codBarras) {
        val code = product.codBarras?.takeIf { it.isNotBlank() }
        value = if (code == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                generateBarcodeBitmap(code, width = 400, height = 100)
            }
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, statusColor)
    ) {
        // Cabeçalho por estado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.nomeProduto,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = IntroFontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = Color.White,
                    contentColor = statusColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Corpo do Cartão
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Tipo:", value = product.subCategoria)
                }
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Data de Validade:", value = formatDate(product.validade))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Tamanho:", value = productSizeLabel(product) ?: "—")
                }
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Marca:", value = product.marca ?: "—")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Linha do Código de Barras e Botão Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                if (!product.codBarras.isNullOrBlank()) {
                    // CORREÇÃO AQUI: Adicionado horizontalAlignment para centralizar imagem e texto
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        barcodeBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Código de Barras",
                                modifier = Modifier
                                    .height(50.dp)
                                    .fillMaxWidth(0.8f),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                        Text(
                            text = product.codBarras!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = onViewClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = GreenSas
                    ),
                    border = BorderStroke(1.dp, Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Detalhes",
                        color = GreenSas,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Composable
fun StockLabelValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

fun generateBarcodeBitmap(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (x in 0 until w) {
            for (y in 0 until h) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun StockFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = GreenSas,
        contentColor = Color.White,
        modifier = modifier.size(64.dp)
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String, text: String, isLoading: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D2E))
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(16.dp), color = Color.White, strokeWidth = 2.dp)
                Text("Apagar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GreenSas)) {
                Text("Cancelar")
            }
        }
    )
}

private fun availableText(count: Int): String = when (count) {
    0 -> "0 Disponíveis"
    1 -> "1 Disponível"
    else -> "$count Disponíveis"
}

private fun expiredCountText(count: Int): String = when (count) {
    1 -> "1 item"
    else -> "$count itens"
}

private fun formatDate(date: Date?): String {
    if (date == null) return "—"
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    } catch (_: Exception) { "—" }
}

private fun productSizeLabel(product: Product): String? {
    val value = product.tamanhoValor ?: return null
    val unit = product.tamanhoUnidade?.takeIf { it.isNotBlank() } ?: return null
    val numeric = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    return "$numeric $unit"
}
