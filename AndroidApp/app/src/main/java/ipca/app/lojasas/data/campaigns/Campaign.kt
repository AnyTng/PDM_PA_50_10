package ipca.app.lojasas.data.campaigns

import com.google.firebase.Timestamp
import java.util.Date

data class Campaign(
    val id: String = "",
    val nomeCampanha: String = "",
    val descCampanha: String = "",
    val dataInicio: Date = Date(),
    val dataFim: Date = Date(),
    val tipo: String = "" // "Interna", "Externa"
)

// Estatísticas para a página de resultados
data class CampaignStats(
    val totalProducts: Int,
    val categoryCounts: Map<String, Int>, // Ex: "Mercearia" -> 10, "Higiene" -> 5
    val categoryPercentages: Map<String, Float>
)