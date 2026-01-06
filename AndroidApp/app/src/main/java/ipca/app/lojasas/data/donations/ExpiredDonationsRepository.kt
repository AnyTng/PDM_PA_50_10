package ipca.app.lojasas.data.donations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.products.ProductStatus

class ExpiredDonationsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
