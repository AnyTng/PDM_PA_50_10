package ipca.app.lojasas.ui.funcionario.stock

import android.net.Uri
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ipca.app.lojasas.ui.funcionario.stock.components.StockGroupCard
import ipca.app.lojasas.ui.funcionario.stock.components.StockSearchBar
import ipca.app.lojasas.ui.theme.GreenSas

@Composable
fun ProductsView(
    navController: NavController,
    viewModel: ProductsViewModel = viewModel()
) {
    val state by viewModel.uiState

    ProductsViewContent(
        searchQuery = state.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        isLoading = state.isLoading,
        error = state.error,
        groups = state.groups,
        groupRow = { group ->
            StockGroupCard(
                group = group,
                onClick = {
                    navController.navigate("stockProducts/${Uri.encode(group.name)}")
                }
            )
        },
        onFabClick = { navController.navigate("stockProductCreate") }
    )
}

/**
 * UI "pura" para Preview (não depende de NavController nem de ViewModel).
 */
@Composable
private fun <T> ProductsViewContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    groups: List<T>,
    groupRow: @Composable (T) -> Unit,
    onFabClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StockSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
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
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
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
                .padding(6.dp)
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
        groupRow = {},
        onFabClick = {}
    )
}