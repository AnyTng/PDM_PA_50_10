package ipca.app.lojasas.ui.funcionario.menu.history

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ipca.app.lojasas.data.history.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale

private data class FilterOption(
    val id: String,
    val label: String
)

@Composable
fun HistoryView(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val context = LocalContext.current

    Scaffold(
        containerColor = GreyBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(GreyBg)
                .padding(16.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GreenSas
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Erro ao carregar histórico.",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.entries.isEmpty() -> {
                    Text(
                        text = "Sem registos de histórico.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HistoryFiltersCard(
                            state = state,
                            onToggleAction = viewModel::toggleAction,
                            onToggleYear = viewModel::toggleYear,
                            onToggleFuncionario = viewModel::toggleFuncionario,
                            onClear = viewModel::clearFilters,
                            onExportPdf = { viewModel.exportToPDF(context) }
                        )

                        if (state.filteredEntries.isEmpty()) {
                            Text(
                                text = "Sem resultados para os filtros selecionados.",
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(state.filteredEntries, key = { it.id }) { entry ->
                                    val whenText = entry.timestamp?.let { dateFormatter.format(it) } ?: "-"
                                    HistoryCard(entry = entry, whenText = whenText)
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
private fun HistoryFiltersCard(
    state: HistoryState,
    onToggleAction: (String) -> Unit,
    onToggleYear: (String) -> Unit,
    onToggleFuncionario: (String) -> Unit,
    onClear: () -> Unit,
    onExportPdf: () -> Unit
) {
    val hasFilters = state.selectedActions.isNotEmpty() ||
        state.selectedYears.isNotEmpty() ||
        state.selectedFuncionarios.isNotEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filtros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSas
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear, enabled = hasFilters) {
                    Text("Limpar", color = if (hasFilters) GreenSas else Color.Gray)
                }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar PDF", color = Color.White, fontSize = 12.sp)
                }
            }

            Text(
                text = "Resultados: ${state.filteredEntries.size} / ${state.entries.size}",
                color = Color.DarkGray,
                fontSize = 12.sp
            )

            HistoryFilterSection(
                title = "Funcionários",
                options = state.availableFuncionarios.map { FilterOption(it.id, it.label) },
                selected = state.selectedFuncionarios,
                onToggle = onToggleFuncionario
            )

            HistoryFilterSection(
                title = "Ano",
                options = state.availableYears.map { FilterOption(it, it) },
                selected = state.selectedYears,
                onToggle = onToggleYear
            )

            HistoryFilterSection(
                title = "Tipo de ação",
                options = state.availableActions.map { FilterOption(it, it) },
                selected = state.selectedActions,
                onToggle = onToggleAction
            )
        }
    }
}

@Composable
private fun HistoryFilterSection(
    title: String,
    options: List<FilterOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.Black)
        if (options.isEmpty()) {
            Text(text = "Sem opções disponíveis.", color = Color.Gray, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.id }) { option ->
                    HistoryFilterChip(
                        label = option.label,
                        selected = selected.contains(option.id),
                        onClick = { onToggle(option.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) GreenSas else SurfaceMuted
    val content = if (selected) Color.White else Color.Black
    val border = if (selected) null else BorderStroke(1.dp, Color.LightGray)

    Surface(
        color = background,
        contentColor = content,
        shape = RoundedCornerShape(16.dp),
        border = border,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, whenText: String) {
    val actorName = entry.funcionarioNome.ifBlank { entry.funcionarioId }.ifBlank { "Funcionário" }
    val entityLabel = when {
        entry.entity.isNotBlank() && entry.entityId.isNotBlank() -> "${entry.entity} (${entry.entityId})"
        entry.entity.isNotBlank() -> entry.entity
        entry.entityId.isNotBlank() -> entry.entityId
        else -> ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, GreenSas.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.action.ifBlank { "Ação" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GreenSas
                    )
                    if (entityLabel.isNotBlank()) {
                        Text(
                            text = entityLabel,
                            color = Color.DarkGray,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = whenText,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = GreenSas.copy(alpha = 0.2f))

            Text(
                text = "Por: $actorName",
                color = Color.Black,
                fontSize = 13.sp
            )
            if (!entry.details.isNullOrBlank()) {
                Text(
                    text = entry.details ?: "",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
