package ipca.app.lojasas.ui.funcionario.stock

import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.funcionario.stock.components.StockFab
import ipca.app.lojasas.ui.funcionario.stock.components.StockProductGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import ipca.app.lojasas.ui.theme.GreenSas

@Composable
fun ProductDetailsView(
    navController: NavController,
    nomeProduto: String,
    viewModel: ProductDetailsViewModel = viewModel()
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
        sortOption = state.sortOption,
        onSortSelected = viewModel::onSortSelected,
        groupRow = { group ->
            StockProductGroupCard(
                product = group.product,
                onViewClick = { navController.navigate("stockProduct/${group.product.id}") },
                modifier = Modifier.clickable { navController.navigate("stockProduct/${group.product.id}") }
            )
        },
        onFabClick = {
            navController.navigate("stockProductCreate?productName=${Uri.encode(nomeProduto)}")
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
    sortOption: ProductSortOption,
    onSortSelected: (ProductSortOption) -> Unit,
    groupRow: @Composable (T) -> Unit,
    onFabClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StockSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                showFilter = availableBrands.isNotEmpty() || availableCampaigns.isNotEmpty(),
                filterMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        if (availableBrands.isNotEmpty()) {
                            Text(
                                text = "Marca",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                            availableBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
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
                            Text(
                                text = "Campanha",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                            )
                            availableCampaigns.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
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
                },
                showSort = true,
                sortMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        val options = listOf(
                            ProductSortOption.EXPIRY_ASC to "Validade (mais próxima)",
                            ProductSortOption.EXPIRY_DESC to "Validade (mais distante)",
                            ProductSortOption.SIZE_ASC to "Quantidade (menor \u2192 maior)",
                            ProductSortOption.SIZE_DESC to "Quantidade (maior \u2192 menor)"
                        )
                        options.forEach { (option, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
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
                        color = Color(0xFF333333)
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
                        items(groups.size) { index ->
                            groupRow(groups[index])
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
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
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
                        color = Color(0xFF333333)
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
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
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
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
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
        sortOption = ProductSortOption.EXPIRY_ASC,
        onSortSelected = {},
        groupRow = {},
        onFabClick = {}
    )
}
