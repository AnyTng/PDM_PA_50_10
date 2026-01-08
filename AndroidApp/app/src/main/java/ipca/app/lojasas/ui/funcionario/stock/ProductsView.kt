package ipca.app.lojasas.ui.funcionario.stock

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.funcionario.stock.components.StockFab
import ipca.app.lojasas.ui.funcionario.stock.components.StockExpiredSummaryCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar

@Composable
fun ProductsView(
    navController: NavController,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current

    ProductsViewContent(
        searchQuery = state.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        isLoading = state.isLoading,
        error = state.error,
        groups = state.groups,
        expiredCount = state.expiredCount,
        availableCategories = state.availableCategories,
        selectedCategory = state.selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        sortOption = state.sortOption,
        onSortSelected = viewModel::onSortSelected,
        onExportClick = { viewModel.exportToCSV(context) },
        onExpiredClick = { navController.navigate(Screen.StockExpiredProducts.route) },
        itemKey = { it.name },
        groupRow = { group ->
            StockGroupCard(
                group = group,
                onClick = {
                    navController.navigate(Screen.StockProductsByName.createRoute(group.name))
                }
            )
        },
        onFabClick = { navController.navigate(Screen.StockProductCreate.createRoute()) }
    )
}

/**
 * UI para Preview
 */
@Composable
private fun <T> ProductsViewContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    groups: List<T>,
    expiredCount: Int,
    availableCategories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    sortOption: StockSortOption,
    onSortSelected: (StockSortOption) -> Unit,
    onExportClick: () -> Unit,
    onExpiredClick: () -> Unit,
    itemKey: ((T) -> Any)? = null,
    groupRow: @Composable (T) -> Unit,
    onFabClick: () -> Unit
) {
    val categoryOptions = listOf(CATEGORY_ALL) + availableCategories

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StockSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                showFilter = availableCategories.isNotEmpty(),
                filterMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onCategorySelected(option)
                                    onDismiss()
                                },
                                trailingIcon = {
                                    if (option == selectedCategory) {
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
                },
                showSort = true,
                sortMenuContent = { expanded, onDismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                        val options = listOf(
                            StockSortOption.NAME_ASC to "Nome (A-Z)",
                            StockSortOption.NAME_DESC to "Nome (Z-A)",
                            StockSortOption.QTY_ASC to "Quantidade (menor \u2192 maior)",
                            StockSortOption.QTY_DESC to "Quantidade (maior \u2192 menor)"
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
                },
                showExport = selectedCategory == CATEGORY_ALL,
                onExportClick = onExportClick
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
                        item {
                            StockExpiredSummaryCard(
                                expiredCount = expiredCount,
                                onClick = onExpiredClick
                            )
                        }
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

@Preview(showBackground = true, name = "ProductsView - Normal")
@Composable
private fun ProductsViewPreview_Normal() {
    val fakeGroups = listOf("Alimentar", "Higiene", "Bebidas", "Laticínios")

    ProductsViewContent(
        searchQuery = "leite",
        onSearchQueryChange = {},
        isLoading = false,
        error = null,
        groups = fakeGroups,
        expiredCount = 12,
        availableCategories = listOf("Alimentar", "Higiene"),
        selectedCategory = CATEGORY_ALL,
        onCategorySelected = {},
        sortOption = StockSortOption.NAME_ASC,
        onSortSelected = {},
        onExportClick = {},
        onExpiredClick = {},
        groupRow = { name ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        onFabClick = {}
    )
}

@Preview(showBackground = true, name = "ProductsView - Loading")
@Composable
private fun ProductsViewPreview_Loading() {
    ProductsViewContent(
        searchQuery = "",
        onSearchQueryChange = {},
        isLoading = true,
        error = null,
        groups = emptyList<String>(),
        expiredCount = 0,
        availableCategories = emptyList(),
        selectedCategory = CATEGORY_ALL,
        onCategorySelected = {},
        sortOption = StockSortOption.NAME_ASC,
        onSortSelected = {},
        onExportClick = {},
        onExpiredClick = {},
        groupRow = {},
        onFabClick = {}
    )
}

@Preview(showBackground = true, name = "ProductsView - Error")
@Composable
private fun ProductsViewPreview_Error() {
    ProductsViewContent(
        searchQuery = "",
        onSearchQueryChange = {},
        isLoading = false,
        error = "Erro ao carregar grupos",
        groups = emptyList<String>(),
        expiredCount = 0,
        availableCategories = emptyList(),
        selectedCategory = CATEGORY_ALL,
        onCategorySelected = {},
        sortOption = StockSortOption.NAME_ASC,
        onSortSelected = {},
        onExportClick = {},
        onExpiredClick = {},
        groupRow = {},
        onFabClick = {}
    )
}
