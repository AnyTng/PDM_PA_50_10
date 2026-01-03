package ipca.app.lojasas.ui.funcionario.stock

import ipca.app.lojasas.data.products.Product
import java.util.Date

private const val STATUS_AVAILABLE_PREFIX = "dispon"
private const val STATUS_RESERVED_PREFIX = "reserv"
private const val STATUS_DELIVERED_PREFIX = "entreg"

fun Product.isBaseAvailable(): Boolean {
    val status = estadoProduto?.trim()?.lowercase().orEmpty()
    return status.isBlank() || status.startsWith(STATUS_AVAILABLE_PREFIX)
}

fun Product.isReserved(): Boolean {
    val status = estadoProduto?.trim()?.lowercase().orEmpty()
    return status.startsWith(STATUS_RESERVED_PREFIX)
}

fun Product.isDelivered(): Boolean {
    val status = estadoProduto?.trim()?.lowercase().orEmpty()
    return status.startsWith(STATUS_DELIVERED_PREFIX)
}

fun Product.isExpired(reference: Date = Date()): Boolean {
    val expiry = validade ?: return false
    return expiry.before(reference)
}

fun Product.isExpiredVisible(reference: Date = Date()): Boolean {
    return isExpired(reference) && !isDelivered() && !isReserved()
}

fun Product.isAvailableForCount(reference: Date = Date()): Boolean {
    return isBaseAvailable() && !isExpired(reference)
}

fun Product.displayStatus(reference: Date = Date()): String {
    if (isExpiredVisible(reference)) {
        return "Fora do Prazo"
    }
    return estadoProduto?.trim()?.takeIf { it.isNotBlank() } ?: "Disponivel"
}

fun Product.isVisibleInStockList(reference: Date = Date()): Boolean {
    return isReserved() || isBaseAvailable() || isExpiredVisible(reference)
}
