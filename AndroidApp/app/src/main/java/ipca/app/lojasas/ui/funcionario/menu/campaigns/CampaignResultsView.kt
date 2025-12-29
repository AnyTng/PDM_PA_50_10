package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.campaigns.CampaignStats
import ipca.app.lojasas.ui.theme.GreenSas

@Composable
fun CampaignResultsView(navController: NavController, campaignName: String) {
    var stats by remember { mutableStateOf<CampaignStats?>(null) }
    val repo = remember { CampaignRepository() }

    LaunchedEffect(campaignName) {
        repo.getCampaignStats(campaignName) { stats = it }
    }

    val s = stats
    CampaignResultsContent(
        campaignName = campaignName,
        isLoading = (s == null),
        totalProducts = s?.totalProducts ?: 0,
        categoryPercentages = s?.categoryPercentages ?: emptyMap(),
        categoryCounts = s?.categoryCounts ?: emptyMap()
    )
}

/**
 * UI "pura" -> dá para Preview sem repo.
 */
@Composable
private fun CampaignResultsContent(
    campaignName: String,
    isLoading: Boolean,
    totalProducts: Int,
    categoryPercentages: Map<String, Float>,
    categoryCounts: Map<String, Int>
) {
    Scaffold(
        // topBar = { AppHeader("Resultados", true, { navController.popBackStack() }) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenSas)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        campaignName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSas
                    )
                    Text(
                        "Total angariado: $totalProducts produtos",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(32.dp))
                }

                item {
                    if (totalProducts > 0 && categoryPercentages.isNotEmpty()) {
                        SimplePieChart(categoryPercentages)
                        Spacer(Modifier.height(32.dp))
                    } else {
                        Text("Sem dados para apresentar.")
                    }
                }

                item {
                    categoryCounts.forEach { (cat, count) ->
                        val pct = categoryPercentages[cat] ?: 0f
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.ifEmpty { "Outros" }, fontWeight = FontWeight.Bold)
                            Text("$count un. (${String.format("%.1f", pct)}%)")
                        }
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = GreenSas,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(data: Map<String, Float>) {
    val colors = listOf(GreenSas, Color(0xFFD88C28), Color(0xFF0F4C5C), Color.Gray, Color.Magenta)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = -90f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / 100f) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
        Spacer(Modifier.width(24.dp))
        Column {
            data.keys.forEachIndexed { index, name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = colors[index % colors.size],
                        shape = MaterialTheme.shapes.small
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(name.ifEmpty { "Outros" }, fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------- PREVIEWS ----------------

@Preview(showBackground = true, name = "CampaignResults - Loading")
@Composable
private fun CampaignResultsPreview_Loading() {
    CampaignResultsContent(
        campaignName = "Campanha de Inverno",
        isLoading = true,
        totalProducts = 0,
        categoryPercentages = emptyMap(),
        categoryCounts = emptyMap()
    )
}

@Preview(showBackground = true, name = "CampaignResults - Com dados")
@Composable
private fun CampaignResultsPreview_WithData() {
    val pct = mapOf(
        "Alimentar" to 55f,
        "Higiene" to 25f,
        "Bebidas" to 10f,
        "Infantil" to 7f,
        "" to 3f // Outros
    )
    val counts = mapOf(
        "Alimentar" to 110,
        "Higiene" to 50,
        "Bebidas" to 20,
        "Infantil" to 14,
        "" to 6
    )

    CampaignResultsContent(
        campaignName = "Campanha de Inverno",
        isLoading = false,
        totalProducts = 200,
        categoryPercentages = pct,
        categoryCounts = counts
    )
}

@Preview(showBackground = true, name = "CampaignResults - Sem dados")
@Composable
private fun CampaignResultsPreview_NoData() {
    CampaignResultsContent(
        campaignName = "Campanha X",
        isLoading = false,
        totalProducts = 0,
        categoryPercentages = emptyMap(),
        categoryCounts = emptyMap()
    )
}
