package ipca.app.lojasas.ui.funcionario.stock

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.funcionario.stock.components.StockFab
import ipca.app.lojasas.ui.funcionario.stock.components.StockProductGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun ProductDetailsView(
    navController: NavController,
    nomeProduto: String,
    viewModel: ProductDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState

    LaunchedEffect(nomeProduto) {
        viewModel.observeNomeProduto(nomeProduto)
    }

    ProductDetailsViewContent(
        searchQuery = state.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        isLoading = state.isLoading,
        error = state.error,
        isEmpty = state.groups.isEmpty(),
        emptyText = "Sem produtos com este nome.",
        groups = state.groups,
        availableBrands = state.availableBrands,
        selectedBrand = state.selectedBrand,
        onBrandSelected = viewModel::onBrandSelected,
        availableCampaigns = state.availableCampaigns,
        selectedCampaign = state.selectedCampaign,
        onCampaignSelected = viewModel::onCampaignSelected,
        selectedStatus = state.selectedStatus,
        onStatusSelected = viewModel::onStatusSelected,
        sortOption = state.sortOption,
        onSortSelected = viewModel::onSortSelected,
        itemKey = { it.product.id },
        groupRow = { group ->
            StockProductGroupCard(
                product = group.product,
                onViewClick = { navController.navigate(Screen.StockProduct.createRoute(group.product.id)) },
                modifier = Modifier.clickable { navController.navigate(Screen.StockProduct.createRoute(group.product.id)) }
            )
        },
        onFabClick = {
            navController.navigate(Screen.StockProductCreate.createRoute(nomeProduto))
        }
    )
}

/**
 * UI "pura" (stateless) -> dá para Preview sem NavController/ViewModel.
 */
@Composable
private fun <T> ProductDetailsViewContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    emptyText: String,
    groups: List<T>,
    availableBrands: List<String>,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit,
    availableCampaigns: List<CampaignFilterOption>,
    selectedCampaign: String,
    onCampaignSelected: (String) -> Unit,
    selectedStatus: ProductStatusFilter,
    onStatusSelected: (ProductStatusFilter) -> Unit,
    sortOption: ProductSortOption,
    onSortSelected: (ProductSortOption) -> Unit,
    itemKey: ((T) -> Any)? = null,
    groupRow: @Composable (T) -> Unit,
    onFabClick: () -> Unit
) {
    var showBrandSection by remember { mutableStateOf(false) }
    var showCampaignSection by remember { mutableStateOf(false) }
    var showStatusSection by remember { mutableStateOf(false) }
    var showExpirySortSection by remember { mutableStateOf(false) }
    var showSizeSortSection by remember { mutableStateOf(false) }
    val statusOptions = remember { ProductStatusFilter.values().toList() }
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
                showFilter = true,
                filterMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        DropdownMenuSectionHeader(
                            title = "Marca",
                            expanded = showBrandSection,
                            onToggle = { showBrandSection = !showBrandSection }
                        )
                        if (showBrandSection) {
                            availableBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand, modifier = Modifier.padding(start = 12.dp)) },
                                    onClick = {
                                        onBrandSelected(brand)
                                        onDismiss()
                                    },
                                    trailingIcon = {
                                        if (brand == selectedBrand) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenSas
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        if (availableCampaigns.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuSectionHeader(
                                title = "Campanha",
                                expanded = showCampaignSection,
                                onToggle = { showCampaignSection = !showCampaignSection }
                            )
                            if (showCampaignSection) {
                                availableCampaigns.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, modifier = Modifier.padding(start = 12.dp)) },
                                        onClick = {
                                            onCampaignSelected(option.id)
                                            onDismiss()
                                        },
                                        trailingIcon = {
                                            if (option.id == selectedCampaign) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GreenSas
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuSectionHeader(
                            title = "Estado",
                            expanded = showStatusSection,
                            onToggle = { showStatusSection = !showStatusSection }
                        )
                        if (showStatusSection) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, modifier = Modifier.padding(start = 12.dp)) },
                                    onClick = {
                                        onStatusSelected(option)
                                        onDismiss()
                                    },
                                    trailingIcon = {
                                        if (option == selectedStatus) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenSas
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
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
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GreenSas
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
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
                        color = Color.Red,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }

                isEmpty -> {
                    Text(
                        text = emptyText,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        color = TextDarkGrey
                    )
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
                        items(items = groups, key = itemKey) { group ->
                            groupRow(group)
                        }
                    }
                }
            }
        }

        StockFab(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 22.dp)
        )
    }
}

// ---------------- PREVIEWS ----------------
private data class FakeProduct(val id: String, val nome: String)
private data class FakeGroup(val product: FakeProduct, val quantity: Int)

@Preview(showBackground = true, name = "ProductDetails - Normal")
@Composable
private fun ProductDetailsViewPreview_Normal() {
    val fake = listOf(
        FakeGroup(FakeProduct("p1", "Leite UHT Meio-Gordo"), 12),
        FakeGroup(FakeProduct("p2", "Arroz Carolino"), 7),
        FakeGroup(FakeProduct("p3", "Massa Esparguete"), 20)
    )

    ProductDetailsViewContent(
        searchQuery = "leite",
        onSearchQueryChange = {},
        isLoading = false,
        error = null,
        isEmpty = false,
        emptyText = "Sem produtos com este nome.",
        groups = fake,
        availableBrands = listOf(BRAND_ALL, "Marca X"),
        selectedBrand = BRAND_ALL,
        onBrandSelected = {},
        availableCampaigns = listOf(CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")),
        selectedCampaign = CAMPAIGN_ALL,
        onCampaignSelected = {},
        selectedStatus = ProductStatusFilter.ALL,
        onStatusSelected = {},
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        itemKey = { it.product.id },
        groupRow = { g ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = g.product.nome,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "x${g.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDarkGrey
                    )
                }
            }
        },
        onFabClick = {}
    )
}

@Preview(showBackground = true, name = "ProductDetails - Empty")
@Composable
private fun ProductDetailsViewPreview_Empty() {
    ProductDetailsViewContent(
        searchQuery = "",
        onSearchQueryChange = {},
        isLoading = false,
        error = null,
        isEmpty = true,
        emptyText = "Sem produtos com este nome.",
        groups = emptyList<FakeGroup>(),
        availableBrands = listOf(BRAND_ALL),
        selectedBrand = BRAND_ALL,
        onBrandSelected = {},
        availableCampaigns = listOf(CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")),
        selectedCampaign = CAMPAIGN_ALL,
        onCampaignSelected = {},
        selectedStatus = ProductStatusFilter.ALL,
        onStatusSelected = {},
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        itemKey = { it.product.id },
        groupRow = {},
        onFabClick = {}
    )
}

@Preview(showBackground = true, name = "ProductDetails - Loading")
@Composable
private fun ProductDetailsViewPreview_Loading() {
    ProductDetailsViewContent(
        searchQuery = "",
        onSearchQueryChange = {},
        isLoading = true,
        error = null,
        isEmpty = false,
        emptyText = "Sem produtos com este nome.",
        groups = emptyList<FakeGroup>(),
        availableBrands = listOf(BRAND_ALL),
        selectedBrand = BRAND_ALL,
        onBrandSelected = {},
        availableCampaigns = listOf(CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")),
        selectedCampaign = CAMPAIGN_ALL,
        onCampaignSelected = {},
        selectedStatus = ProductStatusFilter.ALL,
        onStatusSelected = {},
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        itemKey = { it.product.id },
        groupRow = {},
        onFabClick = {}
    )
}

@Preview(showBackground = true, name = "ProductDetails - Error")
@Composable
private fun ProductDetailsViewPreview_Error() {
    ProductDetailsViewContent(
        searchQuery = "",
        onSearchQueryChange = {},
        isLoading = false,
        error = "Erro ao carregar produtos",
        isEmpty = false,
        emptyText = "Sem produtos com este nome.",
        groups = emptyList<FakeGroup>(),
        availableBrands = listOf(BRAND_ALL),
        selectedBrand = BRAND_ALL,
        onBrandSelected = {},
        availableCampaigns = listOf(CampaignFilterOption(CAMPAIGN_ALL, "Todas as campanhas")),
        selectedCampaign = CAMPAIGN_ALL,
        onCampaignSelected = {},
        selectedStatus = ProductStatusFilter.ALL,
        onStatusSelected = {},
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        itemKey = { it.product.id },
        groupRow = {},
        onFabClick = {}
    )
}

@Composable
private fun DropdownMenuSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(title, color = Color.Gray) },
        onClick = onToggle,
        trailingIcon = {
            Icon(
                imageVector = if (expanded) {
                    androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                } else {
                    androidx.compose.material.icons.Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = Color.Gray
            )
        }
    )
}
