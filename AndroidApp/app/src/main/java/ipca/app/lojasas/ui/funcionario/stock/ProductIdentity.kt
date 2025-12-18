package ipca.app.lojasas.ui.funcionario.stock

import ipca.app.lojasas.data.products.Product
import java.util.concurrent.TimeUnit

data class ProductIdentity(
    val nomeProduto: String,
    val subCategoria: String,
    val marca: String,
    val campanha: String,
    val doado: String,
    val codBarras: String,
    val validadeDay: Long,
    val tamanhoValor: Double,
    val tamanhoUnidade: String,
    val descProduto: String,
    val estadoProduto: String,
    val parceiroExternoNome: String,
    val categoria: String

)

fun Product.identity(): ProductIdentity {
    return ProductIdentity(
        nomeProduto = nomeProduto.trim().lowercase(),
        subCategoria = subCategoria.trim().lowercase(),
        marca = marca?.trim()?.lowercase().orEmpty(),
        campanha = campanha?.trim()?.lowercase().orEmpty(),
        doado = doado?.trim()?.lowercase().orEmpty(),
        codBarras = codBarras?.trim()?.lowercase().orEmpty(),
        validadeDay = validade?.let { TimeUnit.MILLISECONDS.toDays(it.time) } ?: -1L,
        tamanhoValor = tamanhoValor ?: -1.0,
        tamanhoUnidade = tamanhoUnidade?.trim()?.lowercase().orEmpty(),
        descProduto = descProduto?.trim()?.lowercase().orEmpty(),
        estadoProduto = estadoProduto?.trim()?.lowercase().orEmpty(),
        parceiroExternoNome = parceiroExternoNome?.trim()?.lowercase().orEmpty(),
        categoria = categoria?.trim()?.lowercase().orEmpty()
    )
}

