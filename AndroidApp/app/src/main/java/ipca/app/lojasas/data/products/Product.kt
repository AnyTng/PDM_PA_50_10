package ipca.app.lojasas.data.products

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class Product(
    val id: String,
    val nomeProduto: String,
    val subCategoria: String,
    val marca: String? = null,
    val campanha: String? = null,
    val doado: String? = null,
    val codBarras: String? = null,
    val validade: Date? = null,
    val tamanhoValor: Double? = null,
    val tamanhoUnidade: String? = null,
    val descProduto: String? = null,
    val estadoProduto: String? = null,
    val parceiroExternoNome: String? = null,
    val idFunc: String? = null
)

data class ProductUpsert(
    val nomeProduto: String,
    val subCategoria: String,
    val marca: String? = null,
    val campanha: String? = null,
    val doado: String? = null,
    val codBarras: String? = null,
    val validade: Date? = null,
    val tamanhoValor: Double? = null,
    val tamanhoUnidade: String? = null,
    val descProduto: String? = null,
    val estadoProduto: String? = null,
    val parceiroExternoNome: String? = null,
    val idFunc: String? = null
)

fun DocumentSnapshot.toProductOrNull(): Product? {
    val nomeProduto = getStringTrimmed("nomeProduto").orEmpty()
    val subCategoria = getStringTrimmed("subCategoria").orEmpty()
    if (nomeProduto.isBlank() || subCategoria.isBlank()) return null

    val tamanho = get("tamanho") as? List<*>
    val tamanhoValor = (tamanho?.getOrNull(0) as? Number)?.toDouble()
    val tamanhoUnidade = (tamanho?.getOrNull(1) as? String)?.trim()

    val validade = when (val raw = get("validade")) {
        is Timestamp -> raw.toDate()
        is Date -> raw
        else -> getTimestamp("validade")?.toDate()
    }

    return Product(
        id = id,
        nomeProduto = nomeProduto,
        subCategoria = subCategoria,
        marca = getStringTrimmed("marca"),
        campanha = getStringTrimmed("campanha"),
        doado = getStringTrimmed("doado"),
        codBarras = getStringOrNumber("codBarras"),
        validade = validade,
        tamanhoValor = tamanhoValor,
        tamanhoUnidade = tamanhoUnidade,
        descProduto = getStringTrimmed("descProduto"),
        estadoProduto = getStringTrimmed("estadoProduto"),
        parceiroExternoNome = getStringTrimmed("ParceiroExternoNome"),
        idFunc = getStringTrimmed("idFunc")
    )
}

private fun DocumentSnapshot.getStringTrimmed(field: String): String? =
    (get(field) as? String)?.trim()

private fun DocumentSnapshot.getStringOrNumber(field: String): String? {
    return when (val value = get(field)) {
        is String -> value.trim()
        is Number -> value.toLong().toString()
        else -> null
    }
}
