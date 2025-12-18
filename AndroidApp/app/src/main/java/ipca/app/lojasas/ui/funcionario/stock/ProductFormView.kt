package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.funcionario.stock.components.rememberBarcodeScanner
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormView(
    navController: NavController,
    productId: String? = null,
    prefillSubCategoria: String? = null, // Parâmetro restaurado!
    viewModel: ProductFormViewModel = viewModel()
) {
    val state by viewModel.uiState
    var showDatePicker by remember { mutableStateOf(false) }

    val startScan = rememberBarcodeScanner(
        onScanned = { code -> viewModel.setCodBarras(code) },
        onError = { /* Log error */ }
    )

    LaunchedEffect(productId, prefillSubCategoria) {
        viewModel.start(productId, prefillSubCategoria)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.validade?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setValidade(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = GreenSas)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    text = if (productId == null) "Novo Produto" else "Editar Produto",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Campos Principais
                StockAutocomplete(
                    label = "Categoria (Ex: Alimentar)",
                    value = state.categoria,
                    suggestions = state.availableCategories,
                    onValueChange = { viewModel.setCategoria(it) }
                )

                StockInput(
                    label = "Produto (Ex: Arroz)",
                    value = state.nomeProduto,
                    onValueChange = { viewModel.setNomeProduto(it) }
                )

                StockInput(
                    label = "Tipo / Variedade (Ex: Carolino)",
                    value = state.subCategoria,
                    onValueChange = { viewModel.setSubCategoria(it) }
                )

                StockInput(
                    label = "Marca",
                    value = state.marca,
                    onValueChange = { viewModel.setMarca(it) }
                )

                // 2. Detalhes de Origem (Campanha e Doador)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // SELETOR DE CAMPANHA (Novo)
                    StockCampaignSelector(
                        label = "Campanha",
                        selectedId = state.campanha,
                        campaigns = state.availableCampaigns,
                        onCampaignSelected = { viewModel.setCampanha(it) },
                        modifier = Modifier.weight(1f)
                    )

                    StockInput(
                        label = "Origem / Doador",
                        value = state.doado,
                        onValueChange = { viewModel.setDoado(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Características Físicas
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StockInput(
                        label = "Tamanho (Valor)",
                        value = state.tamanhoValor,
                        onValueChange = { viewModel.setTamanhoValor(it) },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    StockInput(
                        label = "Unidade (Kg...)",
                        value = state.tamanhoUnidade,
                        onValueChange = { viewModel.setTamanhoUnidade(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Validade
                StockDateInput(
                    label = "Validade",
                    date = state.validade,
                    onClick = { showDatePicker = true }
                )

                // 5. Descrição
                StockInput(
                    label = "Descrição / Notas",
                    value = state.descProduto,
                    onValueChange = { viewModel.setDescProduto(it) },
                    singleLine = false
                )

                // 6. Código de Barras
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockInput(
                        label = "Código de Barras",
                        value = state.codBarras,
                        onValueChange = { viewModel.setCodBarras(it) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = startScan,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(GreenSas, shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Scan", tint = Color.White)
                    }
                }

                if (state.error != null) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.save { navController.popBackStack() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSas,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (state.isSaving) "A guardar..." else "Guardar Produto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCampaignSelector(
    label: String,
    selectedId: String,
    campaigns: List<Campaign>,
    onCampaignSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Mostra o nome da campanha se encontrada, senão mostra o ID ou "Nenhuma"
    val displayText = campaigns.find { it.id == selectedId }?.nomeCampanha
        ?: if (selectedId.isNotBlank()) "ID: $selectedId" else ""

    Box(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSas,
                    focusedLabelColor = GreenSas,
                    focusedTextColor = Color.Black,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedLabelColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                // Opção para limpar
                DropdownMenuItem(
                    text = { Text("Nenhuma", color = Color.Gray) },
                    onClick = {
                        onCampaignSelected("")
                        expanded = false
                    }
                )

                campaigns.forEach { campaign ->
                    DropdownMenuItem(
                        text = { Text(campaign.nomeCampanha, color = Color.Black) },
                        onClick = {
                            onCampaignSelected(campaign.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAutocomplete(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = suggestions.filter {
        it.contains(value, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    expanded = true
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSas,
                    focusedLabelColor = GreenSas,
                    focusedTextColor = Color.Black,
                    cursorColor = GreenSas,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedLabelColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                trailingIcon = {
                    if (filteredSuggestions.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }
            )

            if (filteredSuggestions.isNotEmpty() && expanded) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    filteredSuggestions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, color = Color.Black) },
                            onClick = {
                                onValueChange(selectionOption)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenSas,
            focusedLabelColor = GreenSas,
            focusedTextColor = Color.Black,
            cursorColor = GreenSas,
            unfocusedBorderColor = Color.Gray,
            unfocusedLabelColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

@Composable
fun StockDateInput(
    label: String,
    date: Date?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(date) {
        if (date != null) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date) else ""
    }

    Box(modifier = modifier.clickable { onClick() }) {
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenSas,
                focusedLabelColor = GreenSas,
                focusedTextColor = Color.Black,
                cursorColor = GreenSas,
                unfocusedBorderColor = Color.Gray,
                unfocusedLabelColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}