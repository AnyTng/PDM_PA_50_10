package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.ui.funcionario.stock.components.ConfirmDeleteDialog
import ipca.app.lojasas.ui.funcionario.stock.components.StockBackground
import ipca.app.lojasas.ui.funcionario.stock.components.rememberBarcodeScanner
import ipca.app.lojasas.ui.theme.GreenSas
import ipca.app.lojasas.ui.theme.IntroFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormView(
    navController: NavController,
    productId: String? = null,
    prefillNomeProduto: String? = null,
    viewModel: ProductFormViewModel = viewModel()
) {
    val state by viewModel.uiState
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val canDelete = !state.productId.isNullOrBlank() &&
        ProductStatus.fromFirestore(state.form.estadoProduto) == ProductStatus.AVAILABLE

    val startScan = rememberBarcodeScanner(
        onScanned = { code -> viewModel.onEvent(ProductFormEvent.BarcodeScanned(code)) },
        onError = { /* Log error */ }
    )

    LaunchedEffect(productId, prefillNomeProduto) {
        viewModel.start(productId, prefillNomeProduto)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductFormEffect.NavigateBack -> navController.popBackStack()
                is ProductFormEffect.NavigateAfterDelete -> {
                    val trimmedName = effect.nomeProduto.trim()
                    val targetRoute = if (effect.hasMore && trimmedName.isNotBlank()) {
                        Screen.StockProductsByName.createRoute(trimmedName)
                    } else {
                        Screen.StockProducts.route
                    }

                    val poppedDetails = navController.popBackStack(Screen.StockProduct.route, true)
                    if (!poppedDetails) {
                        navController.popBackStack(Screen.StockProductEdit.route, true)
                    }

                    navController.navigate(targetRoute) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // Date Picker (fica aqui para não aparecer nos previews)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.form.validade?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onEvent(ProductFormEvent.ValidadeChanged(Date(millis)))
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

    if (showDeleteDialog && canDelete) {
        ConfirmDeleteDialog(
            title = "Remover Produto",
            text = "Tem a certeza que deseja remover este produto? Esta ação é irreversível.",
            isLoading = state.isDeleting,
            onConfirm = {
                viewModel.onEvent(ProductFormEvent.DeleteConfirmed)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
        } else {
            ProductFormViewContent(
                state = state,
                showDeleteButton = canDelete,
                onEvent = viewModel::onEvent,
                onValidadeClick = { showDatePicker = true },
                onScanClick = startScan,
                onDeleteClick = { showDeleteDialog = true }
            )
        }
    }
}

/**
 * UI "pura" -> dá para Preview sem NavController/ViewModel.
 */
@Composable
private fun ProductFormViewContent(
    state: ProductFormUiState,
    showDeleteButton: Boolean,
    onEvent: (ProductFormEvent) -> Unit,
    onValidadeClick: () -> Unit,
    onScanClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val title = if (state.productId.isNullOrBlank()) "Novo Produto" else "Editar Produto"
    val form = state.form

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Título
        Text(
            text = title,
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
            value = form.categoria,
            suggestions = state.availableCategories,
            onValueChange = { onEvent(ProductFormEvent.CategoriaChanged(it)) }
        )

        StockInput(
            label = "Produto (Ex: Arroz)",
            value = form.nomeProduto,
            onValueChange = { onEvent(ProductFormEvent.NomeProdutoChanged(it)) }
        )

        StockInput(
            label = "Tipo / Variedade (Ex: Carolino)",
            value = form.subCategoria,
            onValueChange = { onEvent(ProductFormEvent.SubCategoriaChanged(it)) }
        )

        StockInput(
            label = "Marca",
            value = form.marca,
            onValueChange = { onEvent(ProductFormEvent.MarcaChanged(it)) }
        )

        // 2. Detalhes de Origem (Campanha e Doador)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StockCampaignSelector(
                label = "Campanha",
                selectedId = form.campanha,
                campaigns = state.availableCampaigns,
                onCampaignSelected = { onEvent(ProductFormEvent.CampanhaChanged(it)) },
                modifier = Modifier.weight(1f)
            )

            StockInput(
                label = "Origem / Doador",
                value = form.doado,
                onValueChange = { onEvent(ProductFormEvent.DoadoChanged(it)) },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Características Físicas
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StockInput(
                label = "Tamanho (Valor)",
                value = form.tamanhoValor,
                onValueChange = { onEvent(ProductFormEvent.TamanhoValorChanged(it)) },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            StockInput(
                label = "Unidade (Kg...)",
                value = form.tamanhoUnidade,
                onValueChange = { onEvent(ProductFormEvent.TamanhoUnidadeChanged(it)) },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Validade
        StockDateInput(
            label = "Validade",
            date = form.validade,
            onClick = onValidadeClick
        )

        // 5. Descrição
        StockInput(
            label = "Descrição / Notas",
            value = form.descProduto,
            onValueChange = { onEvent(ProductFormEvent.DescProdutoChanged(it)) },
            singleLine = false
        )

        // 6. Código de Barras
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StockInput(
                label = "Código de Barras",
                value = form.codBarras,
                onValueChange = { onEvent(ProductFormEvent.CodBarrasChanged(it)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(GreenSas, shape = RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Scan", tint = Color.White)
            }
        }

        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showDeleteButton) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !state.isSaving && !state.isDeleting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D2E),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE11D2E).copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "Remover Produto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { onEvent(ProductFormEvent.SaveClicked) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !state.isSaving && !state.isDeleting,
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

            }
        } else {
            Button(
                onClick = { onEvent(ProductFormEvent.SaveClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isSaving && !state.isDeleting,
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
        }

        Spacer(modifier = Modifier.height(120.dp))

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
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
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

// ---------------- PREVIEWS ----------------

@Preview(showBackground = true, name = "ProductForm - Novo Produto")
@Composable
private fun ProductFormViewPreview_New() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .padding(16.dp)
    ) {
        val previewState = ProductFormUiState(
            availableCategories = listOf("Alimentar", "Higiene", "Bebidas", "Infantil"),
            form = ProductFormState(
                categoria = "Alimentar",
                nomeProduto = "Arroz",
                subCategoria = "Carolino",
                marca = "Nacional",
                doado = "Doação Particular",
                tamanhoValor = "1",
                tamanhoUnidade = "kg",
                validade = Date(),
                descProduto = "Em bom estado. Armazenar em local seco.",
                codBarras = "5601234567890"
            )
        )
        ProductFormViewContent(
            state = previewState,
            showDeleteButton = false,
            onEvent = {},
            onValidadeClick = {},
            onScanClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ProductForm - Editar Produto")
@Composable
private fun ProductFormViewPreview_Edit() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .padding(16.dp)
    ) {
        val previewState = ProductFormUiState(
            productId = "preview",
            availableCategories = listOf("Alimentar", "Higiene", "Bebidas", "Infantil"),
            form = ProductFormState(
                categoria = "Higiene",
                nomeProduto = "Gel de Banho",
                subCategoria = "Neutro",
                marca = "Marca X",
                doado = "Parceiro Externo",
                tamanhoValor = "0.75",
                tamanhoUnidade = "L",
                descProduto = "Sem observações."
            )
        )
        ProductFormViewContent(
            state = previewState,
            showDeleteButton = true,
            onEvent = {},
            onValidadeClick = {},
            onScanClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "ProductForm - Loading")
@Composable
private fun ProductFormViewPreview_Loading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockBackground)
            .padding(16.dp)
    ) {
        CircularProgressIndicator(color = GreenSas, modifier = Modifier.align(Alignment.Center))
    }
}
