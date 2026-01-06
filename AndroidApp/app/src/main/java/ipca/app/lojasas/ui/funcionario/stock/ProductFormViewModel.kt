package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.data.products.ProductUpsert
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
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

class ProductFormViewModel(
    private val repository: ProductsRepository = ProductsRepository(),
    private val campaignRepository: CampaignRepository = CampaignRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductFormUiState())
    val uiState: State<ProductFormUiState> = _uiState

    private val _effects = Channel<ProductFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var initializedKey: String? = null

    init {
        loadCategories()
        fetchCampaigns()
    }

    private fun fetchCampaigns() {
        campaignRepository.listenCampaigns(
            onSuccess = { campaigns ->
                val filtered = filterValidCampaigns(campaigns)
                _uiState.value = _uiState.value.copy(availableCampaigns = filtered)
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

    fun start(productId: String?, prefillNomeProduto: String?) {
        val key = (productId ?: "NEW") + "|" + (prefillNomeProduto ?: "")
        if (initializedKey == key) return
        initializedKey = key

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
        when (event) {
            is ProductFormEvent.NomeProdutoChanged -> updateForm { copy(nomeProduto = event.value) }
            is ProductFormEvent.CategoriaChanged -> updateForm { copy(categoria = event.value) }
            is ProductFormEvent.SubCategoriaChanged -> updateForm { copy(subCategoria = event.value) }
            is ProductFormEvent.MarcaChanged -> updateForm { copy(marca = event.value) }
            is ProductFormEvent.CampanhaChanged -> updateForm { copy(campanha = event.value) }
            is ProductFormEvent.DoadoChanged -> updateForm { copy(doado = event.value) }
            is ProductFormEvent.CodBarrasChanged -> updateForm { copy(codBarras = event.value) }
            is ProductFormEvent.DescProdutoChanged -> updateForm { copy(descProduto = event.value) }
            is ProductFormEvent.EstadoProdutoChanged -> updateForm { copy(estadoProduto = event.value) }
            is ProductFormEvent.ValidadeChanged -> updateForm { copy(validade = event.value) }
            is ProductFormEvent.TamanhoValorChanged -> updateForm { copy(tamanhoValor = event.value) }
            is ProductFormEvent.TamanhoUnidadeChanged -> updateForm { copy(tamanhoUnidade = event.value) }
            is ProductFormEvent.BarcodeScanned -> onBarcodeScanned(event.value)
            ProductFormEvent.SaveClicked -> save()
            ProductFormEvent.DeleteConfirmed -> deleteProduct()
        }
    }

    private fun onBarcodeScanned(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return

        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(codBarras = normalized),
            error = null
        )

        repository.fetchProductByBarcode(
            codBarras = normalized,
            onSuccess = { product ->
                if (product == null) return@fetchProductByBarcode

                val current = _uiState.value
                if (!current.productId.isNullOrBlank() && current.productId == product.id) {
                    return@fetchProductByBarcode
                }

                _uiState.value = current.copy(
                    form = current.form.copy(
                        nomeProduto = product.nomeProduto,
                        categoria = product.categoria.orEmpty(),
                        subCategoria = product.subCategoria,
                        marca = product.marca.orEmpty(),
                        codBarras = normalized,
                        tamanhoValor = sizeValueFrom(product),
                        tamanhoUnidade = product.tamanhoUnidade?.trim().orEmpty(),
                        estadoProduto = ProductStatus.normalizeFirestoreValue(product.estadoProduto)
                            ?: current.form.estadoProduto
                    )
                )
            },
            onError = { e ->
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
        val subCategoria = form.subCategoria.trim()

        if (nomeProduto.isBlank() || subCategoria.isBlank()) {
            _uiState.value = current.copy(error = "Preenche pelo menos Nome e Subcategoria.")
            return
        }

        val alertaValidade7dEm = alertDateFrom(form.validade)


        val upsert = ProductUpsert(
            nomeProduto = nomeProduto,
            categoria = form.categoria.trim(),
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
            idFunc = FirebaseAuth.getInstance().currentUser?.uid
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
