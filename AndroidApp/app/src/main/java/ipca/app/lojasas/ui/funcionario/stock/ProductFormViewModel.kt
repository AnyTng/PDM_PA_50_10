package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.data.campaigns.Campaign
import ipca.app.lojasas.data.campaigns.CampaignRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductUpsert
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Calendar
import java.util.Date

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val productId: String? = null,

    val availableCategories: List<String> = emptyList(),
    val availableCampaigns: List<Campaign> = emptyList(),

    val nomeProduto: String = "",
    val categoria: String = "",
    val subCategoria: String = "",
    val marca: String = "",
    val campanha: String = "", // Guarda o ID da campanha
    val doado: String = "",
    val codBarras: String = "",
    val descProduto: String = "",
    val estadoProduto: String = "Disponivel",
    val validade: Date? = null,
    val tamanhoValor: String = "",
    val tamanhoUnidade: String = "gr"
)

class ProductFormViewModel(
    private val repository: ProductsRepository = ProductsRepository(),
    private val campaignRepository: CampaignRepository = CampaignRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductFormUiState())
    val uiState: State<ProductFormUiState> = _uiState

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

    fun start(productId: String?, prefillSubCategoria: String?) {
        val key = (productId ?: "NEW") + "|" + (prefillSubCategoria ?: "")
        if (initializedKey == key) return
        initializedKey = key

        if (productId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                subCategoria = prefillSubCategoria?.trim().orEmpty()
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    productId = product.id,
                    nomeProduto = product.nomeProduto,
                    categoria = product.categoria.orEmpty(),
                    subCategoria = product.subCategoria,
                    marca = product.marca.orEmpty(),
                    campanha = product.campanha.orEmpty(),
                    doado = product.doado.orEmpty(),
                    codBarras = product.codBarras.orEmpty(),
                    descProduto = product.descProduto.orEmpty(),
                    estadoProduto = product.estadoProduto?.takeIf { it.isNotBlank() } ?: "Disponivel",
                    validade = product.validade,
                    tamanhoValor = sizeValue,
                    tamanhoUnidade = product.tamanhoUnidade?.takeIf { it.isNotBlank() } ?: "gr"
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
                _uiState.value = _uiState.value.copy(availableCategories = categories)
            },
            onError = { println("Erro ao carregar categorias: ${it.message}") }
        )
    }

    fun setNomeProduto(value: String) = update { copy(nomeProduto = value) }
    fun setCategoria(value: String) = update { copy(categoria = value) }
    fun setSubCategoria(value: String) = update { copy(subCategoria = value) }
    fun setMarca(value: String) = update { copy(marca = value) }
    fun setCampanha(value: String) = update { copy(campanha = value) }
    fun setDoado(value: String) = update { copy(doado = value) }
    fun setCodBarras(value: String) = update { copy(codBarras = value) }
    fun setDescProduto(value: String) = update { copy(descProduto = value) }
    fun setEstadoProduto(value: String) = update { copy(estadoProduto = value) }
    fun setValidade(value: Date?) = update { copy(validade = value) }
    fun setTamanhoValor(value: String) = update { copy(tamanhoValor = value) }
    fun setTamanhoUnidade(value: String) = update { copy(tamanhoUnidade = value) }

    fun onBarcodeScanned(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return

        _uiState.value = _uiState.value.copy(codBarras = normalized, error = null)

        repository.fetchProductByBarcode(
            codBarras = normalized,
            onSuccess = { product ->
                if (product == null) return@fetchProductByBarcode

                val current = _uiState.value
                if (!current.productId.isNullOrBlank() && current.productId == product.id) {
                    return@fetchProductByBarcode
                }

                _uiState.value = current.copy(
                    nomeProduto = product.nomeProduto,
                    categoria = product.categoria.orEmpty(),
                    subCategoria = product.subCategoria,
                    marca = product.marca.orEmpty(),
                    codBarras = normalized,
                    tamanhoValor = sizeValueFrom(product),
                    tamanhoUnidade = product.tamanhoUnidade?.trim().orEmpty(),
                    estadoProduto = product.estadoProduto?.takeIf { it.isNotBlank() } ?: current.estadoProduto
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao procurar código de barras."
                )
            }
        )
    }

    fun save(onSuccess: (String) -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return

        val nomeProduto = current.nomeProduto.trim()
        val subCategoria = current.subCategoria.trim()

        if (nomeProduto.isBlank() || subCategoria.isBlank()) {
            _uiState.value = current.copy(error = "Preenche pelo menos Nome e Subcategoria.")
            return
        }

        // Correção Importante: Usar .trim() em vez de .normalizedOrNull()
        // para permitir enviar strings vazias ("") e limpar o campo na BD.
        val upsert = ProductUpsert(
            nomeProduto = nomeProduto,
            categoria = current.categoria.trim(),
            subCategoria = subCategoria,
            marca = current.marca.trim(),
            campanha = current.campanha.trim(), // Agora envia "" se estiver vazio
            doado = current.doado.trim(),
            codBarras = current.codBarras.trim(),
            validade = current.validade,
            tamanhoValor = current.tamanhoValor.trim().toDoubleOrNull(),
            tamanhoUnidade = current.tamanhoUnidade.trim().takeIf { it.isNotBlank() },
            descProduto = current.descProduto.trim(),
            estadoProduto = current.estadoProduto.trim().ifBlank { "Disponivel" },
            idFunc = FirebaseAuth.getInstance().currentUser?.uid
        )

        _uiState.value = current.copy(isSaving = true, error = null)

        val handlerSuccess = { _: String ->
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess(_uiState.value.productId ?: "")
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

    private fun update(block: ProductFormUiState.() -> ProductFormUiState) {
        _uiState.value = _uiState.value.block()
    }

    private fun sizeValueFrom(product: Product): String {
        return product.tamanhoValor?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        }.orEmpty()
    }
}

// Mantido apenas para lógica interna se necessário, mas removido do uso no save()
private fun String.normalizedOrNull(): String? = trim().takeIf { it.isNotBlank() }
