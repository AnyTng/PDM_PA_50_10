package ipca.app.lojasas.ui.funcionario.menu.campaigns

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event // Ícone para datas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CampaignsView(navController: NavController) {
    val viewModel: CampaignsViewModel = viewModel()
    val state by viewModel.uiState
    var campaignToEdit by remember { mutableStateOf<Campaign?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GreenSas)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. CAMPANHAS ATIVAS (A Decorrer)
                if (state.activeCampaigns.isNotEmpty()) {
                    item { SectionTitle("A Decorrer") }
                    items(state.activeCampaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            isEditable = true, // Ativas podem ser editadas (estender prazo)
                            showResults = false,
                            onAction = { campaignToEdit = campaign }
                        )
                    }
                }

                // 2. CAMPANHAS FUTURAS (Agendadas)
                if (state.futureCampaigns.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionTitle("Agendadas (Futuro)")
                    }
                    items(state.futureCampaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            isEditable = true, // Futuras são totalmente editáveis
                            showResults = false,
                            onAction = { campaignToEdit = campaign }
                        )
                    }
                }

                // 3. HISTÓRICO
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Histórico (Terminadas)")
                }
                if (state.inactiveCampaigns.isEmpty()) item { EmptyText() }
                items(state.inactiveCampaigns) { campaign ->
                    CampaignCard(
                        campaign = campaign,
                        isEditable = false,
                        showResults = true,
                        onAction = { navController.navigate("campaignResults/${campaign.nomeCampanha}") }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { navController.navigate("campaignCreate") },
            containerColor = GreenSas,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova")
        }
    }

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

@Composable
fun CampaignCard(
    campaign: Campaign,
    isEditable: Boolean,
    showResults: Boolean,
    onAction: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(campaign.nomeCampanha, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GreenSas)
                if (isEditable) {
                    Icon(Icons.Default.Edit, "Editar", tint = Color.Gray, modifier = Modifier.clickable { onAction() })
                }
            }
            Text("${dateFormat.format(campaign.dataInicio)} até ${dateFormat.format(campaign.dataFim)}", fontSize = 12.sp, color = Color.Gray)

            if (campaign.descCampanha.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(campaign.descCampanha, fontSize = 14.sp, maxLines = 2, color = Color.Black)
            }

            if (showResults) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
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
                // Nome
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Campanha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenSas, cursorColor = GreenSas, focusedLabelColor = GreenSas)
                )

                // Datas (Lado a Lado)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botão Início
                    OutlinedButton(
                        onClick = { showDatePicker(dataInicio) { dataInicio = it } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            Text("Início", fontSize = 10.sp, color = Color.Gray)
                            Text(dateFormat.format(dataInicio), color = Color.Black, fontSize = 14.sp)
                        }
                    }

                    // Botão Fim
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

                // Validação visual simples
                if (dataInicio.after(dataFim)) {
                    Text("⚠️ A data de início deve ser anterior ao fim.", color = Color.Red, fontSize = 12.sp)
                }

                // Descrição
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenSas, cursorColor = GreenSas, focusedLabelColor = GreenSas)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = campaign.copy(nomeCampanha = nome, descCampanha = desc, dataInicio = dataInicio, dataFim = dataFim)
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                enabled = !dataInicio.after(dataFim) // Desativa botão se datas inválidas
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
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun EmptyText() {
    Text("Nenhuma campanha encontrada.", color = Color.Gray, fontSize = 14.sp)
}