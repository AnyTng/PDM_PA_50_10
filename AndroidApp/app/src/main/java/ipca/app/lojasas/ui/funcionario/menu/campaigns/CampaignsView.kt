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
import java.util.Locale

@Composable
fun CampaignsView(navController: NavController) {
    val viewModel: CampaignsViewModel = viewModel()
    val state by viewModel.uiState
    var campaignToEdit by remember { mutableStateOf<Campaign?>(null) }

    // Layout principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
            .padding(16.dp)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // SECÇÃO ATIVAS
            item { SectionTitle("Campanhas Ativas") }
            if (state.activeCampaigns.isEmpty()) item { EmptyText() }
            items(state.activeCampaigns) { campaign ->
                CampaignCard(
                    campaign = campaign,
                    isActive = true,
                    onAction = { campaignToEdit = campaign } // Abre Pop-up
                )
            }

            // SECÇÃO DESATIVADAS
            item {
                Spacer(Modifier.height(16.dp))
                SectionTitle("Histórico (Desativadas)")
            }
            if (state.inactiveCampaigns.isEmpty()) item { EmptyText() }
            items(state.inactiveCampaigns) { campaign ->
                CampaignCard(
                    campaign = campaign,
                    isActive = false,
                    onAction = { navController.navigate("campaignResults/${campaign.nomeCampanha}") } // Abre Resultados
                )
            }
        }

        // FAB (Botão Flutuante) para criar nova campanha
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

    // POP-UP DE EDIÇÃO (Chama a função corrigida abaixo)
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
fun CampaignCard(campaign: Campaign, isActive: Boolean, onAction: () -> Unit) {
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
                if (isActive) {
                    Icon(Icons.Default.Edit, "Editar", tint = Color.Gray, modifier = Modifier.clickable { onAction() })
                }
            }
            Text("${dateFormat.format(campaign.dataInicio)} até ${dateFormat.format(campaign.dataFim)}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            if (!isActive) {
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
fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
}

@Composable
fun EmptyText() {
    Text("Nenhuma campanha encontrada.", color = Color.Gray, fontSize = 14.sp)
}

// --- COMPONENTE DE EDIÇÃO (CORRIGIDO E ÚNICO) ---
@Composable
fun EditCampaignDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onSave: (Campaign) -> Unit
) {
    // Estados locais para edição
    // Usamos 'remember(campaign)' para resetar os valores se a campanha mudar
    var nome by remember(campaign) { mutableStateOf(campaign.nomeCampanha) }
    var desc by remember(campaign) { mutableStateOf(campaign.descCampanha) }
    var dataFim by remember(campaign) { mutableStateOf(campaign.dataFim) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Função auxiliar para mostrar o DatePicker
    fun showDatePicker() {
        calendar.time = dataFim // Inicia o calendário na data atual da campanha
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                // Atualiza a dataFim
                dataFim = calendar.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Campanha",
                fontWeight = FontWeight.Bold,
                color = GreenSas
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo Nome
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

                // Campo Data de Fim (Estender Campanha)
                OutlinedButton(
                    onClick = { showDatePicker() },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Data de Fim (Estender)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataFim),
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    }
                }

                // Campo Descrição
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
                    val updatedCampaign = campaign.copy(
                        nomeCampanha = nome,
                        descCampanha = desc,
                        dataFim = dataFim // Guarda a nova data
                    )
                    onSave(updatedCampaign)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    )
}