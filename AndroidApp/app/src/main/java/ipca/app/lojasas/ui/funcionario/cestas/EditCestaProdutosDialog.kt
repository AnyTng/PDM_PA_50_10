package ipca.app.lojasas.ui.funcionario.cestas

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditCestaProdutosDialog(
    state: EditCestaProdutosState,
    onDismiss: () -> Unit,
    onAddProduto: (Product) -> Unit,
    onRemoveProduto: (String) -> Unit,
    onSave: () -> Unit
) {
    var showProdutosPicker by remember { mutableStateOf(false) }
    val selectedIds = remember(state.selectedProdutoIds) { state.selectedProdutoIds.toSet() }
    val hasChanges = state.selectedProdutoIds != state.originalProdutoIds
    val canSave = hasChanges && !state.isSaving && !state.isLoadingCesta

    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column {
                DialogHeader(
                    title = "Editar Produtos da Cesta",
                    subtitle = "Selecionados: ${state.selectedProdutoIds.size}",
                    onDismiss = { if (!state.isSaving) onDismiss() }
                )
                HorizontalDivider(color = DividerGreenLight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.error?.let {
                        Text(it, color = RedColor, fontSize = 12.sp)
                    }

                    PickerMetaPill(text = "Selecionados: ${state.selectedProdutoIds.size}")

                    if (state.isLoadingCesta) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GreenSas)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.produtosSelecionados.isEmpty() && state.missingProdutoIds.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sem produtos na cesta.", color = GreyColor)
                                }
                            } else {
                                state.produtosSelecionados.forEach { produto ->
                                    ProdutoEditRow(produto = produto, onRemove = onRemoveProduto)
                                }

                                if (state.missingProdutoIds.isNotEmpty()) {
                                    Text(
                                        text = "Produtos nao encontrados",
                                        fontSize = 12.sp,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    state.missingProdutoIds.forEach { id ->
                                        MissingProdutoRow(id = id, onRemove = onRemoveProduto)
                                    }
                                }
                            }

                            AddProdutoCard(onAdd = { showProdutosPicker = true })
                        }
                    }
                }
                HorizontalDivider(color = DividerGreenLight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { if (!state.isSaving) onDismiss() }) {
                        Text("Fechar", color = GreenSas, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onSave,
                        enabled = canSave,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenSas,
                            contentColor = WhiteColor,
                            disabledContainerColor = SurfaceMuted,
                            disabledContentColor = GreyColor
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = WhiteColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    if (showProdutosPicker) {
        ProdutosPickerDialog(
            produtos = state.produtosDisponiveis,
            produtosCesta = state.produtosOriginais,
            selecionadosIds = selectedIds,
            isLoading = state.isLoadingDisponiveis,
            onDismiss = { showProdutosPicker = false },
            onAdd = { produto ->
                onAddProduto(produto)
            }
        )
    }
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(GreenSas)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WhiteColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = WhiteColor.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun ProdutoEditRow(
    produto: Product,
    onRemove: (String) -> Unit
) {
    val status = ProductStatus.fromFirestore(produto.estadoProduto)
    val statusLabel = ProductStatus.displayLabel(produto.estadoProduto).ifBlank { "-" }
    val canRemove = status != ProductStatus.DELIVERED && status != ProductStatus.DONATED_EXPIRED
    val statusColor = resolveStatusColor(status)
    val categoria = produto.categoria?.takeIf { it.isNotBlank() } ?: "Sem categoria"
    val subCategoria = produto.subCategoria.takeIf { it.isNotBlank() } ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        border = BorderStroke(1.dp, DividerGreenLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = produto.nomeProduto.ifBlank { produto.id },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = "Categoria: $categoria / $subCategoria",
                    fontSize = 12.sp,
                    color = GreyColor
                )
                Spacer(Modifier.height(6.dp))
                StatusPill(label = statusLabel, color = statusColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = { onRemove(produto.id) },
                    enabled = canRemove,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = WhiteColor,
                        disabledContainerColor = SurfaceMuted,
                        disabledContentColor = GreyColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover",
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!canRemove) {
                    Spacer(Modifier.height(4.dp))
                    Text("Nao removivel", fontSize = 10.sp, color = GreyColor)
                }
            }
        }
    }
}

@Composable
private fun MissingProdutoRow(
    id: String,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ErrorRed.copy(alpha = 0.12f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(id, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                    Text("Produto nao encontrado.", fontSize = 12.sp, color = GreyColor)
                }
            }
            Button(
                onClick = { onRemove(id) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Remover", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun AddProdutoCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .border(1.dp, GreenSas, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Adicionar produto", color = GreenSas, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = GreenSas)
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun resolveStatusColor(status: ProductStatus): Color = when (status) {
    ProductStatus.AVAILABLE -> GreenSas
    ProductStatus.RESERVED -> ReservedOrange
    ProductStatus.DELIVERED -> MediumGrey
    ProductStatus.DONATED_EXPIRED -> ErrorRed
    ProductStatus.UNKNOWN -> GreyColor
}

@Composable
private fun PickerDialogContainer(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column {
                DialogHeader(title = title, subtitle = subtitle, onDismiss = onDismiss)
                HorizontalDivider(color = DividerGreenLight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
                HorizontalDivider(color = DividerGreenLight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = GreenSas, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerMetaPill(text: String, color: Color = GreenSas) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun PickerSectionLabel(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ProdutosPickerDialog(
    produtos: List<Product>,
    produtosCesta: List<Product>,
    selecionadosIds: Set<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val ipcaFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenSas,
        unfocusedBorderColor = GreenSas,
        focusedLabelColor = GreenSas,
        unfocusedLabelColor = GreenSas,
        focusedPlaceholderColor = GreenSas,
        unfocusedPlaceholderColor = GreenSas,
        cursorColor = GreenSas,
        focusedContainerColor = SurfaceLight,
        unfocusedContainerColor = SurfaceLight,
        disabledContainerColor = SurfaceLight
    )

    var query by remember { mutableStateOf("") }
    val cestaProdutos = remember(produtosCesta) {
        produtosCesta.distinctBy { it.id }
    }
    val cestaIds = remember(cestaProdutos) { cestaProdutos.map { it.id }.toSet() }
    val filteredCestaProdutos = remember(cestaProdutos, query) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) cestaProdutos
        else cestaProdutos.filter { p ->
            p.id.lowercase(Locale.getDefault()).contains(q) ||
                p.nomeProduto.lowercase(Locale.getDefault()).contains(q) ||
                p.subCategoria.lowercase(Locale.getDefault()).contains(q) ||
                (p.categoria?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                (p.codBarras?.lowercase(Locale.getDefault())?.contains(q) == true)
        }
    }
    val filteredProdutos = remember(produtos, query, cestaIds) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) produtos
        else produtos.filter { p ->
            p.id.lowercase(Locale.getDefault()).contains(q) ||
                p.nomeProduto.lowercase(Locale.getDefault()).contains(q) ||
                p.subCategoria.lowercase(Locale.getDefault()).contains(q) ||
                (p.categoria?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                (p.codBarras?.lowercase(Locale.getDefault())?.contains(q) == true)
        }
    }
    val filteredDisponiveis = remember(filteredProdutos, cestaIds) {
        filteredProdutos.filterNot { cestaIds.contains(it.id) }
    }

    val proximos = remember(filteredDisponiveis) {
        filteredDisponiveis
            .filter { it.validade != null }
            .sortedBy { it.validade }
            .take(8)
    }

    val porCategoria = remember(filteredDisponiveis) {
        filteredDisponiveis
            .groupBy { it.categoria?.ifBlank { "Sem categoria" } ?: "Sem categoria" }
            .toSortedMap()
            .mapValues { (_, list) ->
                list.sortedWith(
                    compareBy<Product> { it.validade ?: Date(Long.MAX_VALUE) }
                        .thenBy { it.nomeProduto.lowercase(Locale.getDefault()) }
                )
            }
    }
    val headerSubtitle = if (query.isBlank()) {
        "Disponiveis: ${produtos.size} | Cesta: ${cestaProdutos.size}"
    } else {
        "${filteredDisponiveis.size + filteredCestaProdutos.size} resultados"
    }

    PickerDialogContainer(
        title = "Selecionar produtos",
        subtitle = headerSubtitle,
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenSas) },
            placeholder = { Text("Pesquisar produto (nome, codigo, categoria)", color = GreenSas) },
            shape = RoundedCornerShape(12.dp),
            colors = ipcaFieldColors
        )
        PickerMetaPill(text = "Selecionados: ${selecionadosIds.size}", color = GreenSas)
        HorizontalDivider(color = DividerGreenLight)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenSas)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredDisponiveis.isEmpty() && filteredCestaProdutos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sem produtos disponiveis.", color = GreyColor)
                    }
                } else {
                    if (filteredCestaProdutos.isNotEmpty()) {
                        PickerSectionLabel(text = "Produtos desta cesta", color = StatusBlue)
                        filteredCestaProdutos.forEach { p ->
                            ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                        }
                    }

                    if (proximos.isNotEmpty()) {
                        PickerSectionLabel(text = "Mais proximos do fim da validade", color = ReservedOrange)
                        proximos.forEach { p ->
                            ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                        }
                    }

                    porCategoria.forEach { (cat, list) ->
                        PickerSectionLabel(text = cat, color = GreenSas)
                        list.forEach { p ->
                            ProdutoPickRow(p, selecionadosIds, dateFmt, onAdd)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProdutoPickRow(
    p: Product,
    selecionadosIds: Set<String>,
    dateFmt: SimpleDateFormat,
    onAdd: (Product) -> Unit
) {
    val already = selecionadosIds.contains(p.id)
    val backgroundColor = if (already) SurfaceMuted else WhiteColor
    val textColor = if (already) GreyColor else TextDark
    val borderColor = if (already) DividerLight else DividerGreenLight
    val validadeTxt = p.validade?.let { dateFmt.format(it) } ?: "-"
    val validadeColor = when {
        p.validade == null -> GreyColor
        p.alertaValidade7d -> WarningOrange
        else -> GreenSas
    }
    val categoriaLabel = listOfNotNull(
        p.categoria?.takeIf { it.isNotBlank() },
        p.subCategoria.takeIf { it.isNotBlank() }
    ).joinToString(" - ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(p.nomeProduto.ifBlank { p.id }, fontWeight = FontWeight.SemiBold, color = textColor)
                if (categoriaLabel.isNotBlank()) {
                    Text(categoriaLabel, fontSize = 12.sp, color = GreyColor)
                    Spacer(Modifier.height(4.dp))
                }
                PickerMetaPill(text = "Validade: $validadeTxt", color = validadeColor)
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onAdd(p) },
                enabled = !already,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenSas,
                    contentColor = WhiteColor,
                    disabledContainerColor = SurfaceMuted,
                    disabledContentColor = GreyColor
                )
            ) {
                if (already) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionado")
                } else {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar")
                }
            }
        }
    }
}
