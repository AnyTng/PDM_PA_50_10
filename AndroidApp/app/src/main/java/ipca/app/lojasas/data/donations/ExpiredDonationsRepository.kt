package ipca.app.lojasas.data.donations

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.common.ListenerHandle
import ipca.app.lojasas.data.common.asListenerHandle
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.ProductStatus
import ipca.app.lojasas.data.products.toProductOrNull
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiredDonationsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    constructor() : this(FirebaseFirestore.getInstance())
    private val donationsCollection = firestore.collection("DoadosForaValidade")
    private val productsCollection = firestore.collection("produtos")

    fun donateExpiredProducts(
        productIds: List<String>,
        associationName: String,
        associationContact: String,
        employeeId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ids = productIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val nome = associationName.trim()
        val contacto = associationContact.trim()
        val funcionarioId = employeeId.trim()

        if (ids.isEmpty()) {
            onError(IllegalArgumentException("Sem produtos para doar."))
            return
        }
        if (nome.isBlank() || contacto.isBlank()) {
            onError(IllegalArgumentException("Dados da associacao incompletos."))
            return
        }
        if (funcionarioId.isBlank()) {
            onError(IllegalArgumentException("Sem id do funcionario."))
            return
        }

        val donationData = mapOf(
            "produtosIds" to ids,
            "associacao" to listOf(nome, contacto),
            "idFunc" to funcionarioId,
            "dataDoacao" to FieldValue.serverTimestamp()
        )

        val batch = firestore.batch()
        val donationRef = donationsCollection.document()
        batch.set(donationRef, donationData)
        ids.forEach { productId ->
            batch.update(
                productsCollection.document(productId),
                "estadoProduto",
                ProductStatus.DONATED_EXPIRED.firestoreValue
            )
        }

        batch.commit()
            .addOnSuccessListener {
                val details = "Associacao: $nome | Produtos: ${ids.size}"
                AuditLogger.logAction(
                    action = "Registou doacao de produtos expirados",
                    entity = "doacao_fora_validade",
                    entityId = donationRef.id,
                    details = details
                )
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }

    fun listenExpiredDonations(
        onSuccess: (List<ExpiredDonationEntry>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerHandle {
        val registration = donationsCollection
            .orderBy("dataDoacao", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents.orEmpty().map { doc ->
                    buildExpiredDonationEntry(doc)
                }
                onSuccess(entries)
            }

        return registration.asListenerHandle()
    }

    fun fetchDonationProducts(
        productIds: List<String>,
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ids = productIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val tasks = ids.map { productsCollection.document(it).get() }
        Tasks.whenAllSuccess<Any>(tasks)
            .addOnSuccessListener { results ->
                val products = results.mapNotNull { result ->
                    (result as? DocumentSnapshot)?.toProductOrNull()
                }
                onSuccess(products)
            }
            .addOnFailureListener { onError(it) }
    }

    private fun buildExpiredDonationEntry(doc: DocumentSnapshot): ExpiredDonationEntry {
        val (name, contact) = parseAssociation(doc.get("associacao"))
        return ExpiredDonationEntry(
            id = doc.id,
            associationName = name,
            associationContact = contact,
            employeeId = doc.getString("idFunc")?.trim().orEmpty(),
            donationDate = parseDonationDate(doc),
            productIds = parseProductIds(doc.get("produtosIds"))
        )
    }

    private fun parseAssociation(value: Any?): Pair<String, String> {
        return when (value) {
            is List<*> -> {
                val name = (value.getOrNull(0) as? String)?.trim().orEmpty()
                val contact = (value.getOrNull(1) as? String)?.trim().orEmpty()
                name to contact
            }
            is Map<*, *> -> {
                val name = (value["nome"] as? String)
                    ?: (value["name"] as? String)
                val contact = (value["contacto"] as? String)
                    ?: (value["contato"] as? String)
                    ?: (value["telefone"] as? String)
                    ?: (value["contact"] as? String)
                name?.trim().orEmpty() to contact?.trim().orEmpty()
            }
            else -> "" to ""
        }
    }

    private fun parseProductIds(value: Any?): List<String> {
        return (value as? List<*>)
            .orEmpty()
            .mapNotNull { it as? String }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun parseDonationDate(doc: DocumentSnapshot): Date? {
        return doc.getTimestamp("dataDoacao")?.toDate()
            ?: (doc.get("dataDoacao") as? Date)
    }
}
