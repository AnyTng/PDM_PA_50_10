package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.funcionario.stock.components.StockProductGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import ipca.app.lojasas.ui.theme.GreenSas
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun ExpiredProductsView(
    navController: NavController,
    viewModel: ExpiredProductsViewModel = viewModel()
) {
    val state by viewModel.uiState

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
        groupRow = { group ->
            StockProductGroupCard(
                product = group.product,
                onViewClick = { navController.navigate("stockProduct/${group.product.id}") },
                modifier = Modifier.clickable { navController.navigate("stockProduct/${group.product.id}") }
            )
        }
    )
}

@Composable
private fun <T> ExpiredProductsViewContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    emptyText: String,
    groups: List<T>,
    sortOption: ProductSortOption,
    onSortSelected: (ProductSortOption) -> Unit,
    groupRow: @Composable (T) -> Unit
) {
    var showExpirySortSection by remember { mutableStateOf(false) }
    var showSizeSortSection by remember { mutableStateOf(false) }
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
            .background(StockBackground)
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
    }
}

@Preview(showBackground = true, name = "ExpiredProducts - Normal")
@Composable
private fun ExpiredProductsViewPreview_Normal() {
    val fake = listOf(
        ProductGroupUi(
            product = Product(id = "p1", nomeProduto = "Leite UHT Meio-Gordo", subCategoria = "Laticinios"),
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
        groupRow = { g ->
            StockProductGroupCard(
                product = g.product,
                onViewClick = {},
                modifier = Modifier
            )
        }
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
