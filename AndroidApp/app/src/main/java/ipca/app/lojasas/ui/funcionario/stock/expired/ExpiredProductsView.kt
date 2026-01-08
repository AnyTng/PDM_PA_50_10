package ipca.app.lojasas.ui.funcionario.stock.expired

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.components.StockProductGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import ipca.app.lojasas.ui.funcionario.stock.ProductGroupUi
import ipca.app.lojasas.ui.funcionario.stock.ProductSortOption

@Composable
fun ExpiredProductsView(
    navController: NavController,
    viewModel: ExpiredProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    var showDonationDialog by remember { mutableStateOf(false) }
    var associationName by remember { mutableStateOf("") }
    var associationContact by remember { mutableStateOf("") }

    LaunchedEffect(state.selectedIds, state.isDonating) {
        if (state.selectedIds.isEmpty() && !state.isDonating) {
            showDonationDialog = false
            associationName = ""
            associationContact = ""
        }
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
        onDonateClick = { showDonationDialog = true }
    )

    if (showDonationDialog) {
        val canConfirm = associationName.isNotBlank() &&
            associationContact.isNotBlank() &&
            state.selectedIds.isNotEmpty() &&
            !state.isDonating

        AlertDialog(
            onDismissRequest = { if (!state.isDonating) showDonationDialog = false },
            title = { Text("Doar itens fora de validade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = associationName,
                        onValueChange = { associationName = it },
                        label = { Text("Nome da associação") },
                        singleLine = true,
                        enabled = !state.isDonating,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = associationContact,
                        onValueChange = { associationContact = it },
                        label = { Text("Contacto") },
                        singleLine = true,
                        enabled = !state.isDonating,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!state.donationError.isNullOrBlank()) {
                        Text(text = state.donationError ?: "Erro", color = RedColor)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.donateSelected(associationName, associationContact) },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                ) {
                    if (state.isDonating) {
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
            },
            dismissButton = {
                TextButton(
                    onClick = { showDonationDialog = false },
                    enabled = !state.isDonating
                ) {
                    Text("Cancelar")
                }
            }
        )
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
    onDonateClick: () -> Unit
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
                            bottom = 120.dp
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
        onDonateClick = {}
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
        Text(
            text = "✓",
            fontSize = 80.sp,
            color = GreenSas.copy(alpha = 0.45f)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = TextDarkGrey,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
