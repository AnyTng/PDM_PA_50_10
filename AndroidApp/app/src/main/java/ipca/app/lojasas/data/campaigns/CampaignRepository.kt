package ipca.app.lojasas.data.campaigns

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.app.lojasas.data.AuditLogger
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.toProductOrNull
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    constructor() : this(FirebaseFirestore.getInstance())

    // Busca todas as campanhas
    fun listenCampaigns(onSuccess: (List<Campaign>) -> Unit, onError: (String) -> Unit) {
        db.collection("campanha")
            .orderBy("dataFim", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError(error.message ?: "Erro ao carregar campanhas")
                    return@addSnapshotListener
                }
                val campaigns = value?.documents?.mapNotNull { doc ->
                    try {
                        Campaign(
                            id = doc.id,
                            nomeCampanha = doc.getString("nomeCampanha") ?: "",
                            descCampanha = doc.getString("descCampanha") ?: "",
                            dataInicio = doc.getTimestamp("dataInicio")?.toDate() ?: Date(),
                            dataFim = doc.getTimestamp("dataFim")?.toDate() ?: Date(),
                            tipo = doc.getString("tipo") ?: ""
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                onSuccess(campaigns)
            }
    }

    // Adicionar Campanha
    fun addCampaign(campaign: Campaign, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val data = hashMapOf(
            "nomeCampanha" to campaign.nomeCampanha,
            "descCampanha" to campaign.descCampanha,
            "dataInicio" to campaign.dataInicio,
            "dataFim" to campaign.dataFim,
            "tipo" to campaign.tipo
        )
        db.collection("campanha").add(data)
            .addOnSuccessListener { doc ->
                val nome = campaign.nomeCampanha.trim()
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Criou campanha", "campanha", doc.id, details)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao criar") }
    }

    // Atualizar Campanha
    fun updateCampaign(campaign: Campaign, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val data = hashMapOf(
            "nomeCampanha" to campaign.nomeCampanha,
            "descCampanha" to campaign.descCampanha,
            "dataInicio" to campaign.dataInicio,
            "dataFim" to campaign.dataFim
        )
        db.collection("campanha").document(campaign.id).update(data as Map<String, Any>)
            .addOnSuccessListener {
                val nome = campaign.nomeCampanha.trim()
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Editou campanha", "campanha", campaign.id, details)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar") }
    }

    fun deleteCampaign(campaign: Campaign, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = campaign.id.trim()
        if (id.isBlank()) {
            onError("Erro: O ID da campanha está vazio. Verifique o Repository.")
            return
        }

        db.collection("campanha")
            .document(id)
            .delete()
            .addOnSuccessListener {
                val nome = campaign.nomeCampanha.trim()
                val details = if (nome.isNotBlank()) "Nome: $nome" else null
                AuditLogger.logAction("Apagou campanha", "campanha", id, details)
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e.message ?: "Erro ao apagar") }
    }

    fun getCampaignById(
        campaignId: String,
        onSuccess: (Campaign?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val normalized = campaignId.trim()
        if (normalized.isBlank()) {
            onSuccess(null)
            return
        }

        db.collection("campanha")
            .document(normalized)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                val campaign = try {
                    Campaign(
                        id = doc.id,
                        nomeCampanha = doc.getString("nomeCampanha") ?: "",
                        descCampanha = doc.getString("descCampanha") ?: "",
                        dataInicio = doc.getTimestamp("dataInicio")?.toDate() ?: Date(),
                        dataFim = doc.getTimestamp("dataFim")?.toDate() ?: Date(),
                        tipo = doc.getString("tipo") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
                onSuccess(campaign)
            }
            .addOnFailureListener { onError(it) }
    }

    // Calcular Estatísticas (Produtos associados à campanha)
    fun getCampaignStats(campaignId: String, onSuccess: (CampaignStats) -> Unit) {
        db.collection("produtos")
            .whereEqualTo("campanha", campaignId)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.mapNotNull { it.toProductOrNull() }
                val total = products.size

                // CORREÇÃO: Agrupar por Categoria em vez de SubCategoria
                // Se a categoria for nula, usamos string vazia "" (que a View mostra como "Outros")
                val counts = products.groupingBy { it.categoria ?: "" }.eachCount()

                // Calcular Percentagens
                val percentages = counts.mapValues { (_, count) ->
                    if (total > 0) (count.toFloat() / total) * 100f else 0f
                }

                onSuccess(CampaignStats(total, counts, percentages))
            }
            .addOnFailureListener { onSuccess(CampaignStats(0, emptyMap(), emptyMap())) }
    }

    fun getCampaignProducts(
        campaignId: String,
        onSuccess: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("produtos")
            .whereEqualTo("campanha", campaignId)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.mapNotNull { it.toProductOrNull() }
                onSuccess(products)
            }
            .addOnFailureListener { onError(it) }
    }
}
