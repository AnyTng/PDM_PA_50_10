package ipca.app.lojasas.data.campaigns

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.app.lojasas.data.products.Product
import ipca.app.lojasas.data.products.toProductOrNull
import java.util.Calendar
import java.util.Date

class CampaignRepository {
    private val db = FirebaseFirestore.getInstance()

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
            .addOnSuccessListener { onSuccess() }
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
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar") }
    }

    // Calcular Estatísticas (Produtos associados à campanha)
    fun getCampaignStats(campaignName: String, onSuccess: (CampaignStats) -> Unit) {
        db.collection("produtos")
            .whereEqualTo("campanha", campaignName)
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
}