package ipca.app.lojasas.ui.funcionario.menu.campaigns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.GreenSas
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CampaignsView(navController: NavController) {
    val viewModel: CampaignsViewModel = viewModel()
    val state by viewModel.uiState
    var campaignToEdit by remember { mutableStateOf<Campaign?>(null) }

    Scaffold(
        //topBar = { AppHeader(title = "Campanhas", showBack = true, onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("campaignCreate") },
                containerColor = GreenSas,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Nova") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF2F2F2))) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

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
                item { Spacer(Modifier.height(16.dp)); SectionTitle("Histórico (Desativadas)") }
                if (state.inactiveCampaigns.isEmpty()) item { EmptyText() }
                items(state.inactiveCampaigns) { campaign ->
                    CampaignCard(
                        campaign = campaign,
                        isActive = false,
                        onAction = { navController.navigate("campaignResults/${campaign.nomeCampanha}") } // Abre Resultados
                    )
                }
            }
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
@Composable
fun EditCampaignDialog(
    campaign: Campaign,
    onDismiss: () -> Unit,
    onSave: (Campaign) -> Unit
) {
    // Estados locais para edição
    var nome by remember { mutableStateOf(campaign.nomeCampanha) }
    var desc by remember { mutableStateOf(campaign.descCampanha) }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Campanha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Cria uma cópia da campanha com os novos dados
                    val updatedCampaign = campaign.copy(
                        nomeCampanha = nome,
                        descCampanha = desc
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