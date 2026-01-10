package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ipca.app.lojasas.data.auth.AuthRepository
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.data.products.ProductUpsert
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class ProductFormState(
    val nomeProduto: String = "",
    val categoria: String = "",
    val subCategoria: String = "",
    val marca: String = "",
    val campanha: String = "", // Guarda o ID da campanha
    val doado: String = "",
    val codBarras: String = "",
    val descProduto: String = "",
    val estadoProduto: String = ProductStatus.AVAILABLE.firestoreValue,
    val validade: Date? = null,
    val tamanhoValor: String = "",
    val tamanhoUnidade: String = "gr"
)

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val productId: String? = null,
    val isBarcodeMatchLocked: Boolean = false,

    val availableCategories: List<String> = emptyList(),
    val availableCampaigns: List<Campaign> = emptyList(),
    val form: ProductFormState = ProductFormState()
)

private val DEFAULT_CATEGORIES = listOf(
    "Alimentar",
    "Higiene Pessoal",
    "Limpeza"
)

sealed interface ProductFormEvent {
    data class NomeProdutoChanged(val value: String) : ProductFormEvent
    data class CategoriaChanged(val value: String) : ProductFormEvent
    data class SubCategoriaChanged(val value: String) : ProductFormEvent
    data class MarcaChanged(val value: String) : ProductFormEvent
    data class CampanhaChanged(val value: String) : ProductFormEvent
    data class DoadoChanged(val value: String) : ProductFormEvent
    data class CodBarrasChanged(val value: String) : ProductFormEvent
    data class DescProdutoChanged(val value: String) : ProductFormEvent
    data class EstadoProdutoChanged(val value: String) : ProductFormEvent
    data class ValidadeChanged(val value: Date?) : ProductFormEvent
    data class TamanhoValorChanged(val value: String) : ProductFormEvent
    data class TamanhoUnidadeChanged(val value: String) : ProductFormEvent
    data class BarcodeScanned(val value: String) : ProductFormEvent
    object SaveClicked : ProductFormEvent
    object DeleteConfirmed : ProductFormEvent
}

sealed interface ProductFormEffect {
    object NavigateBack : ProductFormEffect
    data class NavigateAfterDelete(val nomeProduto: String, val hasMore: Boolean) : ProductFormEffect
}

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val repository: ProductsRepository,
    private val campaignRepository: CampaignRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductFormUiState())
    val uiState: State<ProductFormUiState> = _uiState

    private val _effects = Channel<ProductFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var initializedKey: String? = null
    private var lastBarcodeLookup: String? = null
    private var allCampaigns: List<Campaign> = emptyList()

    init {
        loadCategories()
        fetchCampaigns()
    }

    private fun fetchCampaigns() {
        campaignRepository.listenCampaigns(
            onSuccess = { campaigns ->
                allCampaigns = campaigns
                updateAvailableCampaigns()
            },
            onError = {
                println("Erro ao carregar campanhas: $it")
            }
        )
    }

    private fun filterValidCampaigns(campaigns: List<Campaign>): List<Campaign> {
        val now = Date()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -3)
        val threeMonthsAgo = cal.time

        return campaigns.filter { c ->
            val inicio = c.dataInicio
            val fim = c.dataFim

            val hasStarted = !inicio.after(now)
            // Se dataFim for null, assume-se que não expirou, caso contrário verifica a data
            val isNotExpiredLongAgo = !fim.before(threeMonthsAgo)

            hasStarted && isNotExpiredLongAgo
        }
    }

    private fun updateAvailableCampaigns() {
        val state = _uiState.value
        val filtered = filterValidCampaigns(allCampaigns)
        val currentValue = state.form.campanha.trim()
        val resolvedCampaign = resolveCampaignByIdOrName(currentValue)
        val normalizedId = resolvedCampaign?.id ?: currentValue

        val merged = if (resolvedCampaign != null && filtered.none { it.id == resolvedCampaign.id }) {
            filtered + resolvedCampaign
        } else {
            filtered
        }

        val updatedForm = if (normalizedId != currentValue) {
            state.form.copy(campanha = normalizedId)
        } else {
            state.form
        }

        _uiState.value = state.copy(
            form = updatedForm,
            availableCampaigns = merged
        )
    }

    fun start(productId: String?, prefillNomeProduto: String?) {
        val key = (productId ?: "NEW") + "|" + (prefillNomeProduto ?: "")
        if (initializedKey == key) return
        initializedKey = key
        lastBarcodeLookup = null
        _uiState.value = _uiState.value.copy(isBarcodeMatchLocked = false)

        if (productId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                form = _uiState.value.form.copy(
                    nomeProduto = prefillNomeProduto?.trim().orEmpty()
                )
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, productId = productId)
        repository.fetchProduct(
            productId = productId,
            onSuccess = { product ->
                if (product == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Produto não encontrado."
                    )
                    return@fetchProduct
                }

                val sizeValue = product.tamanhoValor?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                }.orEmpty()
                val normalizedStatus = ProductStatus.normalizeFirestoreValue(product.estadoProduto)
                    ?: ProductStatus.AVAILABLE.firestoreValue

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    productId = product.id,
                    form = _uiState.value.form.copy(
                        nomeProduto = product.nomeProduto,
                        categoria = product.categoria.orEmpty(),
                        subCategoria = product.subCategoria,
                        marca = product.marca.orEmpty(),
                        campanha = product.campanha.orEmpty(),
                        doado = product.doado.orEmpty(),
                        codBarras = product.codBarras.orEmpty(),
                        descProduto = product.descProduto.orEmpty(),
                        estadoProduto = normalizedStatus,
                        validade = product.validade,
                        tamanhoValor = sizeValue,
                        tamanhoUnidade = product.tamanhoUnidade?.takeIf { it.isNotBlank() } ?: "gr"
                    )
                )
                updateAvailableCampaigns()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar produto."
                )
            }
        )
    }

    private fun loadCategories() {
        repository.getUniqueCategories(
            onSuccess = { categories ->
                val merged = mergeCategories(categories)
                _uiState.value = _uiState.value.copy(availableCategories = merged)
            },
            onError = { println("Erro ao carregar categorias: ${it.message}") }
        )
    }

    fun onEvent(event: ProductFormEvent) {
        val isLocked = _uiState.value.isBarcodeMatchLocked
        when (event) {
            is ProductFormEvent.NomeProdutoChanged -> {
                if (!isLocked) updateForm { copy(nomeProduto = event.value) }
            }
            is ProductFormEvent.CategoriaChanged -> {
                if (!isLocked) updateForm { copy(categoria = event.value) }
            }
            is ProductFormEvent.SubCategoriaChanged -> {
                if (!isLocked) updateForm { copy(subCategoria = event.value) }
            }
            is ProductFormEvent.MarcaChanged -> {
                if (!isLocked) updateForm { copy(marca = event.value) }
            }
            is ProductFormEvent.CampanhaChanged -> updateForm { copy(campanha = event.value) }
            is ProductFormEvent.DoadoChanged -> updateForm { copy(doado = event.value) }
            is ProductFormEvent.CodBarrasChanged -> onBarcodeInput(event.value)
            is ProductFormEvent.DescProdutoChanged -> updateForm { copy(descProduto = event.value) }
            is ProductFormEvent.EstadoProdutoChanged -> updateForm { copy(estadoProduto = event.value) }
            is ProductFormEvent.ValidadeChanged -> updateForm { copy(validade = event.value) }
            is ProductFormEvent.TamanhoValorChanged -> {
                if (!isLocked) updateForm { copy(tamanhoValor = event.value) }
            }
            is ProductFormEvent.TamanhoUnidadeChanged -> {
                if (!isLocked) updateForm { copy(tamanhoUnidade = event.value) }
            }
            is ProductFormEvent.BarcodeScanned -> onBarcodeInput(event.value)
            ProductFormEvent.SaveClicked -> save()
            ProductFormEvent.DeleteConfirmed -> deleteProduct()
        }
    }

    private fun onBarcodeInput(value: String) {
        val normalized = value.trim()
        val current = _uiState.value

        _uiState.value = current.copy(
            form = current.form.copy(codBarras = normalized),
            error = null,
            isBarcodeMatchLocked = if (normalized.isBlank()) false else current.isBarcodeMatchLocked
        )

        if (normalized.isBlank()) {
            lastBarcodeLookup = null
            return
        }

        if (lastBarcodeLookup == normalized) return
        lastBarcodeLookup = normalized

        repository.fetchProductByBarcode(
            codBarras = normalized,
            onSuccess = { product ->
                val latest = _uiState.value
                if (latest.form.codBarras.trim() != normalized) return@fetchProductByBarcode

                if (product == null) {
                    _uiState.value = latest.copy(isBarcodeMatchLocked = false)
                    return@fetchProductByBarcode
                }

                if (!latest.productId.isNullOrBlank() && latest.productId == product.id) {
                    _uiState.value = latest.copy(isBarcodeMatchLocked = false)
                    return@fetchProductByBarcode
                }

                _uiState.value = latest.copy(
                    isBarcodeMatchLocked = true,
                    form = latest.form.copy(
                        nomeProduto = product.nomeProduto,
                        categoria = product.categoria.orEmpty(),
                        subCategoria = product.subCategoria,
                        marca = product.marca.orEmpty(),
                        codBarras = normalized,
                        tamanhoValor = sizeValueFrom(product),
                        tamanhoUnidade = product.tamanhoUnidade?.trim().orEmpty()
                    )
                )
            },
            onError = { e ->
                lastBarcodeLookup = null
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao procurar código de barras."
                )
            }
        )
    }

    private fun save() {
        val current = _uiState.value
        if (current.isSaving || current.isDeleting) return

        val form = current.form
        val nomeProduto = form.nomeProduto.trim()
        val categoria = form.categoria.trim()
        val subCategoria = form.subCategoria.trim()

        if (nomeProduto.isBlank() || categoria.isBlank() || subCategoria.isBlank()) {
            _uiState.value = current.copy(error = "Preenche Nome, Categoria e Subcategoria.")
            return
        }

        val alertaValidade7dEm = alertDateFrom(form.validade)


        val upsert = ProductUpsert(
            nomeProduto = nomeProduto,
            categoria = categoria,
            subCategoria = subCategoria,
            marca = form.marca.trim(),
            campanha = form.campanha.trim(), // Agora envia "" se estiver vazio
            doado = form.doado.trim(),
            codBarras = form.codBarras.trim(),
            validade = form.validade,
            alertaValidade7d = false,
            alertaValidade7dEm = alertaValidade7dEm,
            tamanhoValor = form.tamanhoValor.trim().toDoubleOrNull(),
            tamanhoUnidade = form.tamanhoUnidade.trim().takeIf { it.isNotBlank() },
            descProduto = form.descProduto.trim(),
            estadoProduto = ProductStatus.normalizeFirestoreValue(form.estadoProduto)
                ?: ProductStatus.AVAILABLE.firestoreValue,
            idFunc = authRepository.currentUserId()
        )

        _uiState.value = current.copy(isSaving = true, error = null)

        val handlerSuccess = { _: String ->
            _uiState.value = _uiState.value.copy(isSaving = false)
            _effects.trySend(ProductFormEffect.NavigateBack)
        }
        val handlerError = { e: Exception ->
            _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
        }

        if (current.productId.isNullOrBlank()) {
            repository.createProduct(upsert, { id -> handlerSuccess(id) }, handlerError)
        } else {
            repository.updateProduct(current.productId!!, upsert, { handlerSuccess(current.productId) }, handlerError)
        }
    }

    private fun deleteProduct() {
        val current = _uiState.value
        val id = current.productId?.trim().orEmpty()
        if (id.isBlank() || current.isDeleting || current.isSaving) return

        val nomeProduto = current.form.nomeProduto.trim()
        _uiState.value = current.copy(isDeleting = true, error = null)
        repository.deleteProduct(
            productId = id,
            onSuccess = {
                repository.hasProductsByNomeProduto(
                    nomeProduto = nomeProduto,
                    onSuccess = { hasMore ->
                        _uiState.value = _uiState.value.copy(isDeleting = false)
                        _effects.trySend(ProductFormEffect.NavigateAfterDelete(nomeProduto, hasMore))
                    },
                    onError = { e ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            error = e.message ?: "Erro ao confirmar produto."
                        )
                        _effects.trySend(ProductFormEffect.NavigateAfterDelete(nomeProduto, false))
                    }
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = e.message ?: "Erro ao apagar produto."
                )
            }
        )
    }

    private fun updateForm(block: ProductFormState.() -> ProductFormState) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.block()
        )
    }

    private fun sizeValueFrom(product: Product): String {
        return product.tamanhoValor?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        }.orEmpty()
    }

    private fun resolveCampaignByIdOrName(value: String): Campaign? {
        val normalized = value.trim()
        if (normalized.isBlank()) return null

        val byId = allCampaigns.firstOrNull { it.id == normalized }
        if (byId != null) return byId

        val lower = normalized.lowercase(Locale.getDefault())
        return allCampaigns.firstOrNull { campaign ->
            campaign.nomeCampanha.trim().lowercase(Locale.getDefault()) == lower
        }
    }

    private fun alertDateFrom(validade: Date?): Date? {
        return validade?.let { Date(it.time - TimeUnit.DAYS.toMillis(7)) }
    }
}

private fun mergeCategories(categories: List<String>): List<String> {
    if (categories.isEmpty()) return DEFAULT_CATEGORIES

    val merged = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    fun addCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val key = trimmed.lowercase()
        if (seen.add(key)) {
            merged.add(trimmed)
        }
    }

    DEFAULT_CATEGORIES.forEach { addCategory(it) }
    categories.forEach { addCategory(it) }

    return merged
}

// Mantido apenas para lógica interna se necessário, mas removido do uso no save()
private fun String.normalizedOrNull(): String? = trim().takeIf { it.isNotBlank() }
