package ipca.app.lojasas.ui.funcionario.stock

import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import java.util.Date

fun Product.status(): ProductStatus = ProductStatus.fromFirestore(estadoProduto)

fun Product.isBaseAvailable(): Boolean {
    return status() == ProductStatus.AVAILABLE
}

fun Product.isReserved(): Boolean {
    return status() == ProductStatus.RESERVED
}

fun Product.isDelivered(): Boolean {
    return status() == ProductStatus.DELIVERED
}

fun Product.isExpired(reference: Date = Date()): Boolean {
    val expiry = validade ?: return false
    return expiry.before(reference)
}

fun Product.isExpiredVisible(reference: Date = Date()): Boolean {
    return isExpired(reference) && !isDelivered() && !isReserved() && !isDonatedExpired()
}

fun Product.isAvailableForCount(reference: Date = Date()): Boolean {
    return isBaseAvailable() && !isExpired(reference)
}

fun Product.displayStatus(reference: Date = Date()): String {
    if (isExpiredVisible(reference)) {
        return "Fora de Validade"
    }
    return ProductStatus.displayLabel(estadoProduto)
}

fun Product.isVisibleInStockList(reference: Date = Date()): Boolean {
    return isReserved() || isBaseAvailable() || isExpiredVisible(reference)
}

fun Product.isDonatedExpired(): Boolean {
    return status() == ProductStatus.DONATED_EXPIRED
}
