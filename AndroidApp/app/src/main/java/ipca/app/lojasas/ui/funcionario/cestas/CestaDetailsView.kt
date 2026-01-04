package ipca.app.lojasas.ui.funcionario.cestas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as PdfColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.theme.GreenSas
import java.io.File
import java.io.FileOutputStream
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
    val context = LocalContext.current

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

                    Spacer(modifier = Modifier.height(90.dp))
                }

                if (state.cesta != null) {
                    FloatingActionButton(
                        onClick = { exportCestaDetailsPdf(context, state) },
                        containerColor = GreenSas,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(64.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Transferir PDF")
                    }
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

private fun exportCestaDetailsPdf(context: Context, state: CestaDetailsState) {
    val cesta = state.cesta
    if (cesta == null) {
        Toast.makeText(context, "Cesta indisponivel para exportar.", Toast.LENGTH_SHORT).show()
        return
    }

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f
    val lineHeight = 14f
    val sectionSpacing = 8f
    val maxWidth = pageWidth - margin * 2

    val titlePaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val sectionPaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textPaint = Paint().apply {
        color = PdfColor.BLACK
        textSize = 11f
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = margin

    fun drawHeader() {
        y = drawTextLine(canvas, "Detalhes da Cesta", margin, y, titlePaint, maxWidth, lineHeight)
        y = drawTextLine(canvas, "ID: ${cesta.id}", margin, y, textPaint, maxWidth, lineHeight)
        y += sectionSpacing
    }

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
        drawHeader()
    }

    fun ensureSpace(lines: Int) {
        val needed = lines * lineHeight + sectionSpacing
        if (y + needed > pageHeight - margin) {
            newPage()
        }
    }

    fun drawLine(text: String, paint: Paint = textPaint) {
        ensureSpace(1)
        y = drawTextLine(canvas, text, margin, y, paint, maxWidth, lineHeight)
    }

    fun drawSection(title: String) {
        ensureSpace(2)
        y = drawTextLine(canvas, title, margin, y, sectionPaint, maxWidth, lineHeight)
        y += 2f
    }

    drawHeader()

    drawSection("Cesta")
    drawLine("Estado: ${cesta.estadoLabel()}")
    drawLine("Origem: ${cesta.origem ?: "-"}")
    drawLine("Tipo apoio: ${cesta.tipoApoio ?: "-"}")
    drawLine("Faltas: ${cesta.faltas}")
    drawLine("Produtos: ${cesta.produtosCount}")
    drawLine("Agendada: ${formatDateTime(cesta.dataAgendada, dateFormatter)}")
    drawLine("Recolha: ${formatDateTime(cesta.dataRecolha, dateFormatter)}")
    drawLine("Reagendada: ${formatDateTime(cesta.dataReagendada, dateFormatter)}")
    drawLine("Entregue: ${formatDateTime(cesta.dataEntregue, dateFormatter)}")
    drawLine("Cancelada: ${formatDateTime(cesta.dataCancelada, dateFormatter)}")
    drawLine("Ultima falta: ${formatDateTime(cesta.dataUltimaFalta, dateFormatter)}")
    drawLine("Funcionario: ${cesta.funcionarioId.ifBlank { "-" }}")
    cesta.observacoes?.takeIf { it.isNotBlank() }?.let {
        drawLine("Observacoes: $it")
    }

    y += sectionSpacing
    drawSection("Apoiado")
    val apoiado = state.apoiado
    if (apoiado == null) {
        drawLine("Apoiado indisponivel")
    } else {
        drawLine("Nome: ${apoiado.nome}")
        drawLine("Email: ${apoiado.email}")
        drawLine("Contacto: ${apoiado.contacto}")
        drawLine("Documento: ${apoiado.documento}")
        drawLine("Morada: ${apoiado.morada}")
        drawLine("Nacionalidade: ${apoiado.nacionalidade}")
    }

    y += sectionSpacing
    drawSection("Produtos")
    when {
        state.isLoadingProdutos -> drawLine("A carregar produtos...")
        state.produtosError != null -> drawLine("Erro produtos: ${state.produtosError}")
        state.produtos.isEmpty() -> drawLine("Sem produtos.")
        else -> {
            state.produtos.forEach { produto ->
                val categoria = produto.categoria?.takeIf { it.isNotBlank() } ?: "-"
                val subCategoria = produto.subCategoria.takeIf { it.isNotBlank() } ?: "-"
                val marca = produto.marca?.takeIf { it.isNotBlank() } ?: "-"
                val tamanho = formatTamanho(produto.tamanhoValor, produto.tamanhoUnidade)
                val estado = produto.estadoProduto?.takeIf { it.isNotBlank() } ?: "-"
                drawLine("- ${produto.nomeProduto.ifBlank { produto.id }} | $categoria / $subCategoria | $marca | $tamanho | $estado")
            }
        }
    }
    if (!state.isLoadingProdutos && state.produtosMissingIds.isNotEmpty()) {
        drawLine("Produtos nao encontrados: ${state.produtosMissingIds.size}")
    }

    document.finishPage(page)

    try {
        val safeId = cesta.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "cesta_${safeId}_${System.currentTimeMillis()}.pdf"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, fileName)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        Toast.makeText(context, "Guardado em Downloads: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        document.close()
    }
}

private fun drawTextLine(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    maxWidth: Float,
    lineHeight: Float
): Float {
    val clipped = clipText(text, paint, maxWidth)
    canvas.drawText(clipped, x, y, paint)
    return y + lineHeight
}

private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "..."
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
        end -= 1
    }
    return if (end > 0) text.substring(0, end) + ellipsis else ellipsis
}

private fun CestaDetails.estadoLabel(): String {
    val n = estado.trim().lowercase(Locale.getDefault())
    return when {
        n == "entregue" -> "Entregue"
        n == "agendada" -> "Agendada"
        n == "por preparar" || n == "por_preparar" -> "Por preparar"
        n == "em preparar" || n == "em_preparar" -> "Em preparar"
        n == "nao_levantou" || n == "nao levantou" -> "Nao levantou"
        n == "cancelada" -> "Cancelada"
        n.isBlank() -> "-"
        else -> estado
    }
}
