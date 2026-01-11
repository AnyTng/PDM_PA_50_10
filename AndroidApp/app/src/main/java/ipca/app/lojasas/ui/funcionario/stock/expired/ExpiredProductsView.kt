
package ipca.app.lojasas.ui.funcionario.stock.expired

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.donations.ExpiredDonationEntry
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.ProductGroupUi
import ipca.app.lojasas.ui.funcionario.stock.ProductSortOption
import ipca.app.lojasas.ui.funcionario.stock.components.StockProductGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExpiredProductsView(
    navController: NavController,
    viewModel: ExpiredProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    var showDonationDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var associationName by remember { mutableStateOf("") }
    var associationContact by remember { mutableStateOf("") }
    var wasDonating by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedIds, state.isDonating) {
        if (state.selectedIds.isEmpty() && !state.isDonating) {
            showDonationDialog = false
            associationName = ""
            associationContact = ""
        }
    }

    LaunchedEffect(state.isDonating, state.donationError) {
        if (wasDonating && !state.isDonating && state.donationError.isNullOrBlank()) {
            Toast.makeText(context, "Obrigado! ❤️", Toast.LENGTH_SHORT).show()
        }
        wasDonating = state.isDonating
    }

    ExpiredProductsViewContent(
        searchQuery = state.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        isLoading = state.isLoading,
        error = state.error,
        isEmpty = state.groups.isEmpty(),
        emptyText = "Sem produtos fora de validade.",
        groups = state.groups,
        sortOption = state.sortOption,
        onSortSelected = viewModel::onSortSelected,
        selectedIds = state.selectedIds,
        onToggleSelection = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        isDonating = state.isDonating,
        donationError = state.donationError,
        onOpenDetails = { product -> navController.navigate(Screen.StockProduct.createRoute(product.id)) },
        onDonateClick = { showDonationDialog = true },
        onHistoryClick = { showHistoryDialog = true }
    )

    if (showDonationDialog) {
        ExpiredDonationDialog(
            selectedCount = state.selectedIds.size,
            associationName = associationName,
            associationContact = associationContact,
            donationError = state.donationError,
            isDonating = state.isDonating,
            onAssociationNameChange = { associationName = it },
            onAssociationContactChange = { associationContact = it },
            onConfirm = { viewModel.donateSelected(associationName, associationContact) },
            onDismiss = { showDonationDialog = false }
        )
    }

    if (showHistoryDialog) {
        ExpiredDonationHistoryDialog(
            entries = state.historyEntries,
            isLoading = state.isHistoryLoading,
            error = state.historyError,
            exportingDonationId = state.exportingDonationId,
            onDismiss = {
                showHistoryDialog = false
                viewModel.closeDonationDetails()
            },
            onViewDetails = viewModel::openDonationDetails,
            onExportPdf = { entry -> viewModel.exportDonationPdf(context, entry) }
        )
    }

    val detailsEntry = state.detailsEntry
    if (showHistoryDialog && detailsEntry != null) {
        ExpiredDonationDetailsDialog(
            entry = detailsEntry,
            products = state.detailsProducts,
            isLoading = state.isDetailsLoading,
            error = state.detailsError,
            onDismiss = viewModel::closeDonationDetails
        )
    }
}

@Composable
private fun ExpiredDonationDialog(
    selectedCount: Int,
    associationName: String,
    associationContact: String,
    donationError: String?,
    isDonating: Boolean,
    onAssociationNameChange: (String) -> Unit,
    onAssociationContactChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canConfirm = associationName.isNotBlank() &&
        associationContact.isNotBlank() &&
        selectedCount > 0 &&
        !isDonating
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
    val dismissAction = { if (!isDonating) onDismiss() }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DonationDialogHeader(
                    title = "Doar itens fora de validade",
                    subtitle = "Selecionados: $selectedCount",
                    isDismissEnabled = !isDonating,
                    onDismiss = dismissAction
                )
                HorizontalDivider(color = DividerGreenLight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = associationName,
                        onValueChange = onAssociationNameChange,
                        label = { Text("Nome da associacao") },
                        singleLine = true,
                        enabled = !isDonating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ipcaFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = associationContact,
                        onValueChange = onAssociationContactChange,
                        label = { Text("Contacto") },
                        singleLine = true,
                        enabled = !isDonating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ipcaFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!donationError.isNullOrBlank()) {
                        Text(
                            text = donationError.ifBlank { "Erro" },
                            color = RedColor,
                            fontSize = 12.sp
                        )
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
                    TextButton(
                        onClick = dismissAction,
                        enabled = !isDonating
                    ) {
                        Text("Cancelar", color = GreenSas, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenSas,
                            contentColor = WhiteColor,
                            disabledContainerColor = SurfaceMuted,
                            disabledContentColor = GreyColor
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        if (isDonating) {
                            CircularProgressIndicator(
                                color = WhiteColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(16.dp)
                                    .width(16.dp)
                            )
                        }
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredDonationHistoryDialog(
    entries: List<ExpiredDonationEntry>,
    isLoading: Boolean,
    error: String?,
    exportingDonationId: String?,
    onDismiss: () -> Unit,
    onViewDetails: (ExpiredDonationEntry) -> Unit,
    onExportPdf: (ExpiredDonationEntry) -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DonationDialogHeader(
                    title = "Historico de doacoes fora de validade",
                    subtitle = "Total: ${entries.size}",
                    isDismissEnabled = !isLoading,
                    onDismiss = onDismiss
                )
                HorizontalDivider(color = DividerGreenLight)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = GreenSas
                            )
                        }
                        error != null -> {
                            Text(
                                text = error.ifBlank { "Erro ao carregar historico." },
                                color = RedColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        entries.isEmpty() -> {
                            Text(
                                text = "Sem registos de doacoes.",
                                color = GreyColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(items = entries, key = { it.id }) { entry ->
                                    val whenText = entry.donationDate?.let { dateFormatter.format(it) } ?: "-"
                                    ExpiredDonationHistoryCard(
                                        entry = entry,
                                        whenText = whenText,
                                        isExporting = exportingDonationId == entry.id,
                                        onViewDetails = { onViewDetails(entry) },
                                        onExportPdf = { onExportPdf(entry) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredDonationHistoryCard(
    entry: ExpiredDonationEntry,
    whenText: String,
    isExporting: Boolean,
    onViewDetails: () -> Unit,
    onExportPdf: () -> Unit
) {
    val associationName = entry.associationName.ifBlank { "Associacao" }
    val associationContact = entry.associationContact.ifBlank { "-" }

    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, DividerGreenLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = associationName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GreenSas
                    )
                    Text(
                        text = "Contacto: $associationContact",
                        fontSize = 12.sp,
                        color = GreyColor
                    )
                }
                Text(text = whenText, fontSize = 11.sp, color = GreyColor)
            }

            HorizontalDivider(color = DividerGreenLight)

            Text(
                text = "Produtos: ${entry.productIds.size}",
                fontSize = 12.sp,
                color = DarkGreyColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewDetails) {
                    Text("Ver detalhes", color = GreenSas, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onExportPdf,
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSas,
                        contentColor = WhiteColor,
                        disabledContainerColor = SurfaceMuted,
                        disabledContentColor = GreyColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = WhiteColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WhiteColor)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExpiredDonationDetailsDialog(
    entry: ExpiredDonationEntry,
    products: List<ExpiredDonationProductItem>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val whenText = entry.donationDate?.let { dateFormatter.format(it) } ?: "-"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            border = BorderStroke(1.dp, DividerGreenLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DonationDialogHeader(
                    title = "Detalhes da doacao",
                    subtitle = "${entry.associationName.ifBlank { "Associacao" }} - $whenText",
                    isDismissEnabled = true,
                    onDismiss = onDismiss
                )
                HorizontalDivider(color = DividerGreenLight)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DonationSummaryCard(entry = entry, whenText = whenText)

                    Text(
                        text = "Produtos doados",
                        fontWeight = FontWeight.Bold,
                        color = GreenSas
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = GreenSas
                                )
                            }
                            error != null -> {
                                Text(
                                    text = error.ifBlank { "Erro ao carregar detalhes." },
                                    color = RedColor,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            products.isEmpty() -> {
                                Text(
                                    text = "Sem produtos associados.",
                                    color = GreyColor,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            else -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)
                                ) {
                                    items(items = products, key = { "${it.name}-${it.idLabel}-${it.count}" }) { item ->
                                        ExpiredDonationProductCard(item = item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationSummaryCard(entry: ExpiredDonationEntry, whenText: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, DividerGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DonationDetailRow(label = "Associacao", value = entry.associationName.ifBlank { "-" })
            DonationDetailRow(label = "Contacto", value = entry.associationContact.ifBlank { "-" })
            DonationDetailRow(label = "Data", value = whenText)
            DonationDetailRow(label = "Produtos", value = entry.productIds.size.toString())
        }
    }
}

@Composable
private fun DonationDetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = GreyColor)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = BlackColor)
    }
}

@Composable
private fun ExpiredDonationProductCard(item: ExpiredDonationProductItem) {
    val chipColor = if (item.isMissing) SurfaceMuted else GreenSas.copy(alpha = 0.12f)
    val chipBorder = if (item.isMissing) LightGreyColor else GreenSas.copy(alpha = 0.3f)
    val chipText = if (item.isMissing) GreyColor else GreenSas

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        border = BorderStroke(1.dp, DividerGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
                Text(
                    text = "Categoria: ${item.category} / ${item.subCategory}",
                    fontSize = 12.sp,
                    color = GreyColor
                )
                Text(
                    text = "Marca: ${item.brand} | Tam: ${item.sizeLabel}",
                    fontSize = 12.sp,
                    color = GreyColor
                )
                if (item.idLabel != null) {
                    Text(
                        text = "ID: ${item.idLabel}",
                        fontSize = 11.sp,
                        color = GreyColor
                    )
                }
            }
            Surface(
                color = chipColor,
                contentColor = chipText,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, chipBorder)
            ) {
                Text(
                    text = "x${item.count}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DonationDialogHeader(
    title: String,
    subtitle: String,
    isDismissEnabled: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WhiteColor
                )
                Text(
                    text = subtitle,
                    fontFamily = IntroFontFamily,
                    fontSize = 12.sp,
                    color = WhiteColor.copy(alpha = 0.85f)
                )
            }
            IconButton(onClick = onDismiss, enabled = isDismissEnabled) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WhiteColor)
            }
        }
    }
}

@Composable
private fun ExpiredProductsViewContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    emptyText: String,
    groups: List<ProductGroupUi>,
    sortOption: ProductSortOption,
    onSortSelected: (ProductSortOption) -> Unit,
    selectedIds: Set<String>,
    onToggleSelection: (ProductGroupUi) -> Unit,
    onClearSelection: () -> Unit,
    isDonating: Boolean,
    donationError: String?,
    onOpenDetails: (Product) -> Unit,
    onDonateClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var showExpirySortSection by remember { mutableStateOf(false) }
    var showSizeSortSection by remember { mutableStateOf(false) }
    val selectedCount = selectedIds.size
    val expirySortOptions = remember {
        listOf(
            ProductSortOption.EXPIRY_ASC to "Proxima",
            ProductSortOption.EXPIRY_DESC to "Distante"
        )
    }
    val sizeSortOptions = remember {
        listOf(
            ProductSortOption.SIZE_ASC to "Crescente",
            ProductSortOption.SIZE_DESC to "Decrescente"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StockSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                showSort = true,
                sortMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        DropdownMenuSectionHeader(
                            title = "Validade",
                            expanded = showExpirySortSection,
                            onToggle = { showExpirySortSection = !showExpirySortSection }
                        )
                        if (showExpirySortSection) {
                            expirySortOptions.forEach { (option, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, modifier = Modifier.padding(start = 12.dp)) },
                                    onClick = {
                                        onSortSelected(option)
                                        onDismiss()
                                    },
                                    trailingIcon = {
                                        if (option == sortOption) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenSas
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = DividerGreenLight,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        DropdownMenuSectionHeader(
                            title = "Quantidade",
                            expanded = showSizeSortSection,
                            onToggle = { showSizeSortSection = !showSizeSortSection }
                        )
                        if (showSizeSortSection) {
                            sizeSortOptions.forEach { (option, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, modifier = Modifier.padding(start = 12.dp)) },
                                    onClick = {
                                        onSortSelected(option)
                                        onDismiss()
                                    },
                                    trailingIcon = {
                                        if (option == sortOption) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenSas
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )

            if (selectedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "$selectedCount selecionado(s)")
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onClearSelection,
                        enabled = !isDonating,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = GreenSas,
                            disabledContentColor = GreenSas.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Limpar")
                    }
                    Button(
                        onClick = onDonateClick,
                        enabled = !isDonating,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                    ) {
                        Text("Doar")
                    }
                }
            }

            if (!donationError.isNullOrBlank()) {
                Text(
                    text = donationError ?: "Erro ao doar.",
                    color = RedColor,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        CircularProgressIndicator(color = GreenSas)
                    }
                }

                error != null -> {
                    Text(
                        text = error.ifBlank { "Erro" },
                        color = RedColor,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }

                isEmpty -> {
                    ExpiredEmptyState(message = emptyText)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 140.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = groups, key = { it.product.id }) { group ->
                            val isSelected = group.productIds.all { selectedIds.contains(it) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isDonating) { onToggleSelection(group) },
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelection(group) },
                                    enabled = !isDonating,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ExpiredRed,
                                        uncheckedColor = GreyColor,
                                        checkmarkColor = WhiteColor,
                                        disabledUncheckedColor = LightGreyColor
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StockProductGroupCard(
                                    product = group.product,
                                    onViewClick = { onOpenDetails(group.product) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onHistoryClick,
            containerColor = GreenSas,
            contentColor = WhiteColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
        ) {
            Icon(imageVector = Icons.Default.History, contentDescription = "Historico")
        }
    }
}

@Preview(showBackground = true, name = "ExpiredProducts - Normal")
@Composable
private fun ExpiredProductsViewPreview_Normal() {
    val fake = listOf(
        ProductGroupUi(
            product = Product(
                id = "p1",
                nomeProduto = "Leite UHT Meio-Gordo",
                subCategoria = "Laticinios"
            ),
            quantity = 2,
            productIds = listOf("p1")
        ),
        ProductGroupUi(
            product = Product(id = "p2", nomeProduto = "Arroz Carolino", subCategoria = "Cereais"),
            quantity = 1,
            productIds = listOf("p2")
        )
    )

    ExpiredProductsViewContent(
        searchQuery = "leite",
        onSearchQueryChange = {},
        isLoading = false,
        error = null,
        isEmpty = false,
        emptyText = "Sem produtos fora de validade.",
        groups = fake,
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        selectedIds = setOf("p1"),
        onToggleSelection = {},
        onClearSelection = {},
        isDonating = false,
        donationError = null,
        onOpenDetails = {},
        onDonateClick = {},
        onHistoryClick = {}
    )
}

@Composable
private fun DropdownMenuSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(title, color = GreyColor) },
        onClick = onToggle,
        trailingIcon = {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = GreyColor
            )
        }
    )
}

@Composable
private fun ExpiredEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = GreenSas.copy(alpha = 0.45f),
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = TextDarkGrey,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
