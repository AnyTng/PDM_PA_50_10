package ipca.app.lojasas.ui.funcionario.stock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import ipca.app.lojasas.data.donations.ExpiredDonationsRepository
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductsRepository
import java.util.Date
import java.util.Locale

data class ExpiredProductsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val groups: List<ProductGroupUi> = emptyList(),
    val sortOption: ProductSortOption = ProductSortOption.EXPIRY_ASC,
    val selectedIds: Set<String> = emptySet(),
    val isDonating: Boolean = false,
    val donationError: String? = null
)

class ExpiredProductsViewModel(
    private val repository: ProductsRepository = ProductsRepository(),
    private val donationsRepository: ExpiredDonationsRepository = ExpiredDonationsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(ExpiredProductsUiState())
    val uiState: State<ExpiredProductsUiState> = _uiState

    private var listener: ListenerRegistration? = null
    private var allGroups: List<ProductGroupUi> = emptyList()

    init {
        listener = repository.listenAllProducts(
            onSuccess = { products ->
                val reference = Date()
                val expired = products.filter { product ->
                    product.isExpiredVisible(reference) && product.doado.isNullOrBlank()
                }
                allGroups = groupIdenticalProducts(expired)
                pruneSelection()
                applyFilter()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar produtos.",
                    groups = emptyList()
                )
            }
        )
    }

    fun onSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
        applyFilter()
    }

    fun onSortSelected(option: ProductSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilter()
    }

    fun toggleSelection(group: ProductGroupUi) {
        if (_uiState.value.isDonating) return
        val ids = group.productIds
        if (ids.isEmpty()) return
        val current = _uiState.value.selectedIds
        val allSelected = ids.all { current.contains(it) }
        val updated = if (allSelected) {
            current - ids
        } else {
            current + ids
        }
        _uiState.value = _uiState.value.copy(selectedIds = updated, donationError = null)
    }

    fun clearSelection() {
        if (_uiState.value.isDonating) return
        _uiState.value = _uiState.value.copy(selectedIds = emptySet(), donationError = null)
    }

    fun donateSelected(associationName: String, associationContact: String) {
        val state = _uiState.value
        if (state.isDonating) return
        if (state.selectedIds.isEmpty()) {
            _uiState.value = state.copy(donationError = "Selecione produtos para doar.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            _uiState.value = state.copy(donationError = "Sem utilizador autenticado.")
            return
        }

        val reference = Date()
        val validIds = allGroups
            .filter { it.product.isExpiredVisible(reference) }
            .flatMap { it.productIds }
            .toSet()
        val idsToDonate = state.selectedIds.intersect(validIds).toList()

        if (idsToDonate.isEmpty()) {
            _uiState.value = state.copy(donationError = "Selecao invalida para doar.")
            return
        }

        _uiState.value = state.copy(isDonating = true, donationError = null)
        donationsRepository.donateExpiredProducts(
            productIds = idsToDonate,
            associationName = associationName,
            associationContact = associationContact,
            employeeId = userId,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isDonating = false,
                    selectedIds = emptySet(),
                    donationError = null
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isDonating = false,
                    donationError = e.message ?: "Erro ao doar produtos."
                )
            }
        )
    }

    private fun applyFilter() {
        val state = _uiState.value
        val q = state.searchQuery.trim()

        var filtered = allGroups
        if (q.isNotBlank()) {
            filtered = filtered.filter {
                it.product.nomeProduto.contains(q, ignoreCase = true) ||
                    (it.product.codBarras?.contains(q, ignoreCase = true) == true) ||
                    (it.product.marca?.contains(q, ignoreCase = true) == true)
            }
        }

        val sorted = when (state.sortOption) {
            ProductSortOption.EXPIRY_ASC -> filtered.sortedWith(expiryComparator())
            ProductSortOption.EXPIRY_DESC -> filtered.sortedWith(expiryComparator().reversed())
            ProductSortOption.SIZE_ASC -> filtered.sortedWith(sizeComparator())
            ProductSortOption.SIZE_DESC -> filtered.sortedWith(sizeComparator().reversed())
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            groups = sorted
        )
    }

    private fun pruneSelection() {
        val selected = _uiState.value.selectedIds
        if (selected.isEmpty()) return
        val validIds = allGroups.flatMap { it.productIds }.toSet()
        val pruned = selected.intersect(validIds)
        if (pruned.size != selected.size) {
            _uiState.value = _uiState.value.copy(selectedIds = pruned)
        }
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}

private fun expiryComparator(): Comparator<ProductGroupUi> {
    return Comparator<ProductGroupUi> { a, b ->
        val aTime = a.product.validade?.time
        val bTime = b.product.validade?.time
        when {
            aTime == null && bTime == null -> 0
            aTime == null -> 1
            bTime == null -> -1
            else -> aTime.compareTo(bTime)
        }
    }.thenBy { it.product.nomeProduto.lowercase(Locale.getDefault()) }
}

private fun sizeComparator(): Comparator<ProductGroupUi> {
    return Comparator<ProductGroupUi> { a, b ->
        val aSize = sizeInBaseUnits(a.product)
        val bSize = sizeInBaseUnits(b.product)
        when {
            aSize == null && bSize == null -> 0
            aSize == null -> 1
            bSize == null -> -1
            else -> aSize.compareTo(bSize)
        }
    }.thenBy { it.product.nomeProduto.lowercase(Locale.getDefault()) }
}

private fun sizeInBaseUnits(product: Product): Double? {
    val value = product.tamanhoValor ?: return null
    val unitRaw = product.tamanhoUnidade?.trim()?.lowercase(Locale.getDefault()) ?: return null
    val unit = unitRaw.replace(" ", "")
    val multiplier = when (unit) {
        "g", "gr", "grama", "gramas" -> 1.0
        "kg", "kgs", "quilo", "quilos", "kilogram", "kilograms" -> 1000.0
        "mg", "miligram", "miligrama", "miligramas" -> 0.001
        "ml", "mililitro", "mililitros" -> 1.0
        "cl" -> 10.0
        "l", "lt", "litro", "litros" -> 1000.0
        "un", "uni", "unid", "unidade", "unidades" -> 1.0
        else -> return null
    }
    return value * multiplier
}

private fun groupIdenticalProducts(products: List<Product>): List<ProductGroupUi> {
    return products
        .groupBy { it.identity() }
        .values
        .map { groupProducts ->
            val representative = groupProducts.first()
            ProductGroupUi(
                product = representative,
                quantity = groupProducts.size,
                productIds = groupProducts.map { it.id }
            )
        }
        .sortedWith(
            compareBy<ProductGroupUi> { it.product.nomeProduto.lowercase() }
                .thenBy { it.product.marca.orEmpty().lowercase() }
        )
}
