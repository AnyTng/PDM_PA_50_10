package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import ipca.app.lojasas.data.products.ProductUpsert
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Date

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val productId: String? = null,
    val nomeProduto: String = "",
    val subCategoria: String = "",
    val marca: String = "",
    val campanha: String = "",
    val doado: String = "",
    val codBarras: String = "",
    val descProduto: String = "",
    val estadoProduto: String = "Disponivel",
    val validade: Date? = null,
    val tamanhoValor: String = "",
    val tamanhoUnidade: String = "gr"
)

class ProductFormViewModel(
    private val repository: ProductsRepository = ProductsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ProductFormUiState())
    val uiState: State<ProductFormUiState> = _uiState

    private var initializedKey: String? = null

    fun start(productId: String?, prefillSubCategoria: String?) {
        val key = (productId ?: "NEW") + "|" + (prefillSubCategoria ?: "")
        if (initializedKey == key) return
        initializedKey = key

        if (productId.isNullOrBlank()) {
            _uiState.value = ProductFormUiState(
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

    fun setNomeProduto(value: String) = update { copy(nomeProduto = value) }
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

    fun save(onSuccess: (String) -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return

        val nomeProduto = current.nomeProduto.trim()
        val subCategoria = current.subCategoria.trim()
        if (nomeProduto.isBlank() || subCategoria.isBlank()) {
            _uiState.value = current.copy(error = "Preenche pelo menos Nome e Subcategoria.")
            return
        }

        val tamanhoValor = current.tamanhoValor.trim().toDoubleOrNull()
        val tamanhoUnidade = current.tamanhoUnidade.trim().takeIf { it.isNotBlank() }

        val upsert = ProductUpsert(
            nomeProduto = nomeProduto,
            subCategoria = subCategoria,
            marca = current.marca.normalizedOrNull(),
            campanha = current.campanha.normalizedOrNull(),
            doado = current.doado.normalizedOrNull(),
            codBarras = current.codBarras.normalizedOrNull(),
            validade = current.validade,
            tamanhoValor = tamanhoValor,
            tamanhoUnidade = tamanhoUnidade,
            descProduto = current.descProduto.normalizedOrNull(),
            estadoProduto = current.estadoProduto.normalizedOrNull() ?: "Disponivel",
            idFunc = FirebaseAuth.getInstance().currentUser?.uid
        )

        _uiState.value = current.copy(isSaving = true, error = null)
        val id = current.productId
        if (id.isNullOrBlank()) {
            repository.createProduct(
                product = upsert,
                onSuccess = { newId ->
                    _uiState.value = _uiState.value.copy(isSaving = false, productId = newId)
                    onSuccess(newId)
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Erro ao criar produto."
                    )
                }
            )
        } else {
            repository.updateProduct(
                productId = id,
                product = upsert,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onSuccess(id)
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Erro ao guardar produto."
                    )
                }
            )
        }
    }

    private fun update(block: ProductFormUiState.() -> ProductFormUiState) {
        _uiState.value = _uiState.value.block()
    }
}

private fun String.normalizedOrNull(): String? = trim().takeIf { it.isNotBlank() }

