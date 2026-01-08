package ipca.app.lojasas.ui.funcionario.menu.campaigns

import ipca.app.lojasas.ui.theme.*
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.campaigns.Campaign
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CampaignsView(navController: NavController) {
    val viewModel: CampaignsViewModel = hiltViewModel()
    val state by viewModel.uiState
    var campaignToEdit by remember { mutableStateOf<Campaign?>(null) }
    val context = LocalContext.current // Necessário para os Toasts

    val activeModels = remember(state.activeCampaigns) {
        state.activeCampaigns.map {
            CampaignCardModel(
                item = it,
                nome = it.nomeCampanha,
                desc = it.descCampanha,
                inicio = it.dataInicio,
                fim = it.dataFim
            )
        }
    }
    val futureModels = remember(state.futureCampaigns) {
        state.futureCampaigns.map {
            CampaignCardModel(
                item = it,
                nome = it.nomeCampanha,
                desc = it.descCampanha,
                inicio = it.dataInicio,
                fim = it.dataFim
            )
        }
    }
    val inactiveModels = remember(state.inactiveCampaigns) {
        state.inactiveCampaigns.map {
            CampaignCardModel(
                item = it,
                nome = it.nomeCampanha,
                desc = it.descCampanha,
                inicio = it.dataInicio,
                fim = it.dataFim
            )
        }
    }

    CampaignsViewContent(
        isLoading = state.isLoading,
        active = activeModels,
        future = futureModels,
        inactive = inactiveModels,
        onCreate = { navController.navigate(Screen.CampaignCreate.route) },
        onEdit = { campaign -> campaignToEdit = campaign },
        // [CORREÇÃO AQUI] Adicionados onSuccess e onError vazios
        onDeleteFuture = { campaign ->
            viewModel.deleteCampaign(
                campaign,
                onSuccess = {
                    android.widget.Toast.makeText(context, "Campanha eliminada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { erro ->
                    android.widget.Toast.makeText(context, erro, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        },
        onResults = { campaign ->
            navController.navigate(Screen.CampaignResults.createRoute(campaign.id))
        }
    )

    // POP-UP DE EDIÇÃO
    if (campaignToEdit != null) {
        EditCampaignDialog(
            campaign = campaignToEdit!!,
            onDismiss = { campaignToEdit = null },
            onSave = { updated ->
                viewModel.updateCampaign(updated) { campaignToEdit = null }
            }
        )
    }
}

/**
 * Modelo simples para poderes fazer Preview sem precisar instanciar Campaign.
 */
private data class CampaignCardModel<T>(
    val item: T,
    val nome: String,
    val desc: String,
    val inicio: Date,
    val fim: Date
)

/**
 * UI "pura" -> dá para Preview sem NavController/ViewModel.
 */
@Composable
private fun <T> CampaignsViewContent(
    isLoading: Boolean,
    active: List<CampaignCardModel<T>>,
    future: List<CampaignCardModel<T>>,
    inactive: List<CampaignCardModel<T>>,
    onCreate: () -> Unit,
    onEdit: (T) -> Unit,
    onDeleteFuture: (T) -> Unit,
    onResults: (T) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
            .padding(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. CAMPANHAS ATIVAS
                if (active.isNotEmpty()) {
                    item { SectionTitle("A Decorrer") }
                    items(active) { model ->
                        CampaignCardContent(
                            nome = model.nome,
                            desc = model.desc,
                            dataInicio = model.inicio,
                            dataFim = model.fim,
                            isEditable = true,
                            showResults = false,
                            onAction = { onEdit(model.item) }
                        )
                    }
                }

                // 2. CAMPANHAS FUTURAS
                if (future.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionTitle("Agendadas (Futuro)")
                    }
                    items(future) { model ->
                        CampaignCardContent(
                            nome = model.nome,
                            desc = model.desc,
                            dataInicio = model.inicio,
                            dataFim = model.fim,
                            isEditable = true,
                            showResults = false,
                            onAction = { onEdit(model.item) },
                            onDelete = { onDeleteFuture(model.item) }
                        )
                    }
                }

                // 3. HISTÓRICO
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Histórico (Terminadas)")
                }
                if (inactive.isEmpty()) item { EmptyText() }
                items(inactive) { model ->
                    CampaignCardContent(
                        nome = model.nome,
                        desc = model.desc,
                        dataInicio = model.inicio,
                        dataFim = model.fim,
                        isEditable = false,
                        showResults = true,
                        onAction = { onResults(model.item) }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onCreate,
            containerColor = GreenSas,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova")
        }
    }
}

/**
 * Wrapper original.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    isEditable: Boolean,
    showResults: Boolean,
    onAction: () -> Unit
) {
    CampaignCardContent(
        nome = campaign.nomeCampanha,
        desc = campaign.descCampanha,
        dataInicio = campaign.dataInicio,
        dataFim = campaign.dataFim,
        isEditable = isEditable,
        showResults = showResults,
        onAction = onAction
    )
}

/**
 * UI do cartão.
 */
@Composable
private fun CampaignCardContent(
    nome: String,
    desc: String,
    dataInicio: Date,
    dataFim: Date,
    isEditable: Boolean,
    showResults: Boolean,
    onAction: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenSas)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Apagar",
                            tint = Color.Red,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable { onDelete() }
                        )
                    }

                    if (isEditable) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.Gray,
                            modifier = Modifier.clickable { onAction() }
                        )
                    }
                }
            }

            Text(
                "${dateFormat.format(dataInicio)} até ${dateFormat.format(dataFim)}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 14.sp, maxLines = 2, color = Color.Black)
            }

            if (showResults) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Resultados")
                }
            }
        }
    }
}

@Composable
fun EditCampaignDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onSave: (Campaign) -> Unit
) {
    var nome by remember(campaign) { mutableStateOf(campaign.nomeCampanha) }
    var desc by remember(campaign) { mutableStateOf(campaign.descCampanha) }
    var dataInicio by remember(campaign) { mutableStateOf(campaign.dataInicio) }
    var dataFim by remember(campaign) { mutableStateOf(campaign.dataFim) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val hasStarted = remember(campaign) { campaign.dataInicio.before(Date()) }

    fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        calendar.time = initialDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Campanha", fontWeight = FontWeight.Bold, color = GreenSas) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Campanha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenSas,
                        cursorColor = GreenSas,
                        focusedLabelColor = GreenSas
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker(dataInicio) { dataInicio = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !hasStarted
                    ) {
                        Column {
                            val color = if (hasStarted) Color.LightGray else Color.Black
                            val labelColor = if (hasStarted) Color.LightGray else Color.Gray

                            Text("Início", fontSize = 10.sp, color = labelColor)
                            Text(dateFormat.format(dataInicio), color = color, fontSize = 14.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showDatePicker(dataFim) { dataFim = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            Text("Fim", fontSize = 10.sp, color = Color.Gray)
                            Text(dateFormat.format(dataFim), color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }

                if (hasStarted) {
                    Text("⚠️ Campanha ativa. Início bloqueado.", color = Color.Gray, fontSize = 11.sp)
                }

                if (dataInicio.after(dataFim)) {
                    Text("⚠️ A data de início deve ser anterior ao fim.", color = Color.Red, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenSas,
                        cursorColor = GreenSas,
                        focusedLabelColor = GreenSas
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = campaign.copy(
                        nomeCampanha = nome,
                        descCampanha = desc,
                        dataInicio = dataInicio,
                        dataFim = dataFim
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                enabled = !dataInicio.after(dataFim)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun EmptyText() {
    Text("Nenhuma campanha encontrada.", color = Color.Gray, fontSize = 14.sp)
}

// ---------------- PREVIEWS ----------------

private fun dateOf(y: Int, m: Int, d: Int): Date =
    Calendar.getInstance().apply {
        set(y, m, d, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

@Preview(showBackground = true, name = "Campaigns - Loading")
@Composable
private fun CampaignsViewPreview_Loading() {
    CampaignsViewContent<Unit>(
        isLoading = true,
        active = emptyList(),
        future = emptyList(),
        inactive = emptyList(),
        onCreate = {},
        onEdit = { _ -> },
        onDeleteFuture = { _ -> },
        onResults = { _ -> }
    )
}

@Preview(showBackground = true, name = "Campaigns - Com listas")
@Composable
private fun CampaignsViewPreview_WithData() {
    val active = listOf(
        CampaignCardModel(Unit, "Campanha de Inverno", "Angariação de bens alimentares e higiene.",
            dateOf(2025, Calendar.DECEMBER, 1), dateOf(2026, Calendar.JANUARY, 10)
        ),
        CampaignCardModel(Unit, "Natal Solidário", "Produtos infantis e brinquedos.",
            dateOf(2025, Calendar.DECEMBER, 10), dateOf(2026, Calendar.JANUARY, 5)
        )
    )

    val future = listOf(
        CampaignCardModel(Unit, "Páscoa", "Cabazes para famílias.",
            dateOf(2026, Calendar.MARCH, 1), dateOf(2026, Calendar.APRIL, 10)
        )
    )

    val inactive = listOf(
        CampaignCardModel(Unit, "Regresso às Aulas", "Material escolar e higiene.",
            dateOf(2025, Calendar.SEPTEMBER, 1), dateOf(2025, Calendar.SEPTEMBER, 30)
        )
    )

    CampaignsViewContent<Unit>(
        isLoading = false,
        active = active,
        future = future,
        inactive = inactive,
        onCreate = {},
        onEdit = { _ -> },
        onDeleteFuture = { _ -> },
        onResults = { _ -> }
    )
}

@Preview(showBackground = true, name = "Campaigns - Histórico vazio")
@Composable
private fun CampaignsViewPreview_EmptyHistory() {
    CampaignsViewContent<Unit>(
        isLoading = false,
        active = emptyList(),
        future = emptyList(),
        inactive = emptyList(),
        onCreate = {},
        onEdit = { _ -> },
        onDeleteFuture = { _ -> },
        onResults = { _ -> }
    )
}
