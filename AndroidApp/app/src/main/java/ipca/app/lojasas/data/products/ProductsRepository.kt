package ipca.app.lojasas.data.products

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import ipca.app.lojasas.data.AuditLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.get

@Singleton
class ProductsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    constructor() : this(FirebaseFirestore.getInstance())
    private val collection = firestore.collection("produtos")

    fun listenAllProducts(
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents.orEmpty().mapNotNull { it.toProductOrNull() }
            onSuccess(products)
        }
    }

    fun listenProductsBySubCategoria(
        subCategoria: String,
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection
            .whereEqualTo("subCategoria", subCategoria)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents.orEmpty().mapNotNull { it.toProductOrNull() }
                onSuccess(products)
            }
    }

    fun listenProductsByNomeProduto(
        nomeProduto: String,
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection
            .whereEqualTo("nomeProduto", nomeProduto)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents.orEmpty().mapNotNull { it.toProductOrNull() }
                onSuccess(products)
            }
    }

    fun listenProduct(
        productId: String,
        onSuccess: (Product?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection.document(productId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            onSuccess(snapshot?.toProductOrNull())
        }
    }

    fun fetchProduct(
        productId: String,
        onSuccess: (Product?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(productId)
            .get()
            .addOnSuccessListener { onSuccess(it.toProductOrNull()) }
            .addOnFailureListener { onError(it) }
    }

    fun fetchProductByBarcode(
        codBarras: String,
        onSuccess: (Product?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = codBarras.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        collection
            .whereEqualTo("codBarras", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val product = result.documents.firstOrNull()?.toProductOrNull()
                onSuccess(product)
            }
            .addOnFailureListener { onError(it) }
    }

    fun hasProductsByNomeProduto(
        nomeProduto: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = nomeProduto.trim()
        if (normalized.isBlank()) {
            onSuccess(false)
            return
        }

        collection
            .whereEqualTo("nomeProduto", normalized)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot -> onSuccess(!snapshot.isEmpty) }
            .addOnFailureListener { onError(it) }
    }

    fun createProduct(
        product: ProductUpsert,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.add(product.toFirestoreMap())
            .addOnSuccessListener { doc ->
                val nome = product.nomeProduto.trim()
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Criou produto", "produto", doc.id, details)
                onSuccess(doc.id)
            }
            .addOnFailureListener { onError(it) }
    }

    fun updateProduct(
        productId: String,
        product: ProductUpsert,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(productId)
            .set(product.toFirestoreMap(), SetOptions.merge())
            .addOnSuccessListener {
                val nome = product.nomeProduto.trim()
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Editou produto", "produto", productId, details)
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }

    fun updateBarcode(
        productId: String,
        codBarras: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(productId)
            .set(mapOf("codBarras" to codBarras.trim()), SetOptions.merge())
            .addOnSuccessListener {
                val normalized = codBarras.trim()
                val details = if (normalized.isNotBlank()) "Codigo: $normalized" else null
                AuditLogger.logAction("Atualizou codigo de barras", "produto", productId, details)
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }

    fun getUniqueCategories(onSuccess: (List<String>) -> Unit, onError: (Exception) -> Unit) {
        collection.get()
            .addOnSuccessListener { result ->
                val categories = result.documents
                    .mapNotNull { it.getString("categoria")?.trim() } // O nome do campo tem de ser igual ao do toFirestoreMap
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                onSuccess(categories)
            }
            .addOnFailureListener { onError(it) }
    }

    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(productId)
            .delete()
            .addOnSuccessListener {
                AuditLogger.logAction("Apagou produto", "produto", productId)
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }
}

private fun ProductUpsert.toFirestoreMap(): Map<String, Any?> {
    val base = mutableMapOf<String, Any?>(
        "nomeProduto" to nomeProduto.trim(),
        "subCategoria" to subCategoria.trim(),
        "marca" to marca?.trim(),
        "categoria" to categoria?.trim(),
        "campanha" to campanha?.trim(),
        "doado" to doado?.trim(),
        "codBarras" to codBarras?.trim(),
        "descProduto" to descProduto?.trim(),
        "estadoProduto" to (ProductStatus.normalizeFirestoreValue(estadoProduto) ?: estadoProduto?.trim()),
        "ParceiroExternoNome" to parceiroExternoNome?.trim(),
        "idFunc" to idFunc?.trim(),
        "validade" to validade,
        "alertaValidade7d" to alertaValidade7d,
        "alertaValidade7dEm" to alertaValidade7dEm
    )

    val tamanhoList = if (tamanhoValor != null && !tamanhoUnidade.isNullOrBlank()) {
        val value: Any =
            if (tamanhoValor % 1.0 == 0.0) tamanhoValor.toLong() else tamanhoValor
        listOf(value, tamanhoUnidade.trim())
    } else {
        null
    }
    base["tamanho"] = tamanhoList

    val keepNullKeys = setOf("alertaValidade7dEm")
    return base.filter { (key, value) -> value != null || key in keepNullKeys }
}
