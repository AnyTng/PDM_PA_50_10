package ipca.app.lojasas.ui.funcionario.cestas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val GreenSas = Color(0xFF094E33)
private val GreyBg = Color(0xFFF2F2F2)

@Composable
fun CestasListView(
    navController: NavController,
    viewModel: CestasListViewModel = viewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val dateFmt = rememberDateFormatter()

    // Diálogos
    var cestaParaReagendarEntrega by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaFaltaReagendar by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaTerceiraFalta by remember { mutableStateOf<CestaItem?>(null) }
    var cestaParaCancelar by remember { mutableStateOf<CestaItem?>(null) }

    fun openDateTimePicker(initial: Date?, onSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.time = initial

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        onSelected(cal.time)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreyBg)
            .padding(16.dp)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))

            else -> {
                val (agendadas, historico) = remember(state.cestas) {
                    val ag = state.cestas.filter { it.isAgendada() }
                        .sortedBy { it.dataAgendada ?: it.dataRecolha ?: Date(Long.MAX_VALUE) }
                    val hist = state.cestas.filterNot { it.isAgendada() }
                        .sortedByDescending { it.dataAgendada ?: it.dataRecolha ?: Date(0) }
                    ag to hist
                }

                Column(Modifier.fillMaxSize()) {
                    state.error?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (agendadas.isEmpty()) {
                            item {
                                Text("Sem cestas agendadas.", color = Color.Gray)
                            }
                        } else {
                            items(agendadas, key = { it.id }) { cesta ->
                                CestaCard(
                                    cesta = cesta,
                                    dateFmt = dateFmt,
                                    showActions = true,
                                    onCancelar = { cestaParaCancelar = cesta },
                                    onEntregue = { viewModel.marcarEntregue(cesta) },
                                    onReagendar = { cestaParaReagendarEntrega = cesta },
                                    onFaltou = {
                                        // Na 3ª falta não permite escolher dia
                                        if (cesta.faltas >= 2) {
                                            cestaParaTerceiraFalta = cesta
                                        } else {
                                            cestaParaFaltaReagendar = cesta
                                        }
                                    }
                                )
                            }
                        }

                        if (historico.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Entregues / Não levantou",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                            items(historico, key = { it.id }) { cesta ->
                                CestaCard(
                                    cesta = cesta,
                                    dateFmt = dateFmt,
                                    showActions = false,
                                    onCancelar = {},
                                    onEntregue = {},
                                    onReagendar = {},
                                    onFaltou = {}
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { navController.navigate("createCesta") },
                    containerColor = GreenSas,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Criar cesta")
                }
            }
        }
    }

    // Cancelar
    if (cestaParaCancelar != null) {
        val cesta = cestaParaCancelar!!
        AlertDialog(
            onDismissRequest = { cestaParaCancelar = null },
            title = { Text("Cancelar cesta") },
            text = { Text("Pretende cancelar a cesta para o apoiado ${cesta.apoiadoId}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelarCesta(cesta.id)
                        cestaParaCancelar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaCancelar = null }) { Text("Fechar") }
            }
        )
    }

    // Reagendar entrega (SEM contar falta)
    if (cestaParaReagendarEntrega != null) {
        val cesta = cestaParaReagendarEntrega!!
        AlertDialog(
            onDismissRequest = { cestaParaReagendarEntrega = null },
            title = { Text("Reagendar entrega") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(6.dp))
                    Text("Escolha uma nova data/hora.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openDateTimePicker(cesta.dataAgendada ?: cesta.dataRecolha) { novaData ->
                            viewModel.reagendarEntrega(cesta, novaData)
                        }
                        cestaParaReagendarEntrega = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                ) {
                    Text("Selecionar data")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaReagendarEntrega = null }) { Text("Fechar") }
            }
        )
    }

    // Faltou (conta como falta e permite reagendar apenas até à 2ª falta)
    if (cestaParaFaltaReagendar != null) {
        val cesta = cestaParaFaltaReagendar!!
        AlertDialog(
            onDismissRequest = { cestaParaFaltaReagendar = null },
            title = { Text("Faltou - reagendar") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(6.dp))
                    Text("Faltas atuais: ${cesta.faltas}")
                    Spacer(Modifier.height(10.dp))
                    Text("Escolha uma nova data/hora (isto irá contar como falta).")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openDateTimePicker(cesta.dataAgendada ?: cesta.dataRecolha) { novaData ->
                            viewModel.reagendarComFalta(cesta, novaData)
                        }
                        cestaParaFaltaReagendar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                ) {
                    Text("Selecionar data")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaFaltaReagendar = null }) { Text("Fechar") }
            }
        )
    }

    // 3ª falta (sem escolher dia)
    if (cestaParaTerceiraFalta != null) {
        val cesta = cestaParaTerceiraFalta!!
        AlertDialog(
            onDismissRequest = { cestaParaTerceiraFalta = null },
            title = { Text("3ª falta") },
            text = {
                Column {
                    Text("Apoiado: ${cesta.apoiadoId}")
                    Spacer(Modifier.height(8.dp))
                    Text("Esta é a 3ª falta. A cesta passará para o estado 'Nao_Levantou'.")
                    Spacer(Modifier.height(8.dp))
                    Text("Não é possível selecionar novo dia.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.registarTerceiraFaltaSemReagendar(cesta)
                        cestaParaTerceiraFalta = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Marcar como Nao levantou")
                }
            },
            dismissButton = {
                TextButton(onClick = { cestaParaTerceiraFalta = null }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun CestaCard(
    cesta: CestaItem,
    dateFmt: SimpleDateFormat,
    showActions: Boolean,
    onCancelar: () -> Unit,
    onEntregue: () -> Unit,
    onReagendar: () -> Unit,
    onFaltou: () -> Unit
) {
    val estadoLabel = cesta.estadoLabel()
    val data = cesta.dataAgendada ?: cesta.dataRecolha

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apoiado: ${cesta.apoiadoId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GreenSas
                )
                Text(
                    text = estadoLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (estadoLabel == "Nao levantou") Color(0xFFB00020) else Color.Gray
                )
            }

            if (cesta.origem?.equals("Urgente", ignoreCase = true) == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Origem: Pedido Urgente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB26A00)
                )
            }

            data?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Entrega: ${dateFmt.format(it)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (cesta.faltas > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Faltas: ${cesta.faltas}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (showActions) {
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCancelar,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = onEntregue,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                        ) {
                            Text("Entregue")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onReagendar,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                        ) {
                            Text("Reagendar")
                        }
                        Button(
                            onClick = onFaltou,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                        ) {
                            Text("Faltou")
                        }
                    }
                }
            }
        }
    }
}

private fun CestaItem.isAgendada(): Boolean {
    val n = estado.trim().lowercase(Locale.getDefault())
    return n == "agendada" || n == "por preparar" || n == "por_preparar" || n == "em preparar" || n == "em_preparar"
}

private fun CestaItem.estadoLabel(): String {
    val n = estado.trim().lowercase(Locale.getDefault())
    return when {
        n == "entregue" -> "Entregue"
        n == "agendada" -> "Agendada"
        n == "nao_levantou" || n == "não levantou" || n == "nao levantou" -> "Nao levantou"
        n == "cancelada" -> "Cancelada"
        else -> if (estado.isBlank()) "—" else estado
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    MaterialTheme
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
