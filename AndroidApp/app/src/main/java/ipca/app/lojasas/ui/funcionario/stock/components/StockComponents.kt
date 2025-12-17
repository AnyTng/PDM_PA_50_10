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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val StockBackground = Color(0xFFF2F2F2)
val StockAccent = Color(0xFFC9A27B)

@Composable
fun StockSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pesquisar"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                    tint = StockAccent
                )
            },
            placeholder = { Text(placeholder, color = StockAccent) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StockAccent,
                unfocusedBorderColor = StockAccent,
                focusedLabelColor = StockAccent,
                unfocusedLabelColor = StockAccent,
                cursorColor = GreenSas,
                focusedLeadingIconColor = StockAccent,
                unfocusedLeadingIconColor = StockAccent
            )
        )
        IconButton(onClick = { /* TODO: filtros */ }) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = Color(0xFF333333))
        }
        IconButton(onClick = { /* TODO: ordenar */ }) {
            Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color(0xFF333333))
        }
        IconButton(onClick = { /* TODO: exportar */ }) {
            Icon(Icons.Default.FileDownload, contentDescription = "Exportar", tint = Color(0xFF333333))
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = IntroFontFamily
                ),
                color = GreenSas,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
fun StockProductGroupCard(
    product: Product,
    quantity: Int,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // Cabeçalho Verde
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenSas)
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
                    contentColor = GreenSas,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Quantity: $quantity",
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
                    StockLabelValue(label = "Product Type:", value = product.subCategoria)
                }
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Expiry Date:", value = formatDate(product.validade))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Size:", value = productSizeLabel(product) ?: "—")
                }
                Column(modifier = Modifier.weight(1f)) {
                    StockLabelValue(label = "Brand:", value = product.marca ?: "—")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Linha do Código de Barras e Botão Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                if (!product.codBarras.isNullOrBlank()) {
                    val barcodeBitmap = remember(product.codBarras) {
                        generateBarcodeBitmap(product.codBarras!!, width = 400, height = 100)
                    }

                    // CORREÇÃO AQUI: Adicionado horizontalAlignment para centralizar imagem e texto
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (barcodeBitmap != null) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "Barcode",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = "Delete", color = Color.White, fontWeight = FontWeight.Bold)
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