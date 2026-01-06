package ipca.app.lojasas.data.products

enum class ProductStatus(
    val firestoreValue: String,
    val displayLabel: String
) {
    AVAILABLE("Disponivel", "Disponivel"),
    RESERVED("Reservado", "Reservado"),
    DELIVERED("Entregue", "Entregue"),
    DONATED_EXPIRED("DoadoForaValidade", "Doado Fora de Validade"),
    UNKNOWN("", "Disponivel");

    companion object {
        private const val AVAILABLE_PREFIX = "dispon"
        private const val RESERVED_PREFIX = "reserv"
        private const val DELIVERED_PREFIX = "entreg"
        private const val DONATED_EXPIRED_KEY = "doadoforavalidade"

        fun fromFirestore(raw: String?): ProductStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            val compact = normalized.replace(" ", "")
            if (normalized.isBlank() || normalized.startsWith(AVAILABLE_PREFIX)) return AVAILABLE
            if (normalized.startsWith(RESERVED_PREFIX)) return RESERVED
            if (normalized.startsWith(DELIVERED_PREFIX)) return DELIVERED
            if (compact == DONATED_EXPIRED_KEY) return DONATED_EXPIRED
            return UNKNOWN
        }

        fun normalizeFirestoreValue(raw: String?): String? {
            val trimmed = raw?.trim()
            val status = fromFirestore(trimmed)
            return when (status) {
                UNKNOWN -> trimmed?.takeIf { it.isNotBlank() }
                else -> status.firestoreValue
            }
        }

        fun displayLabel(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            val status = fromFirestore(trimmed)
            return if (status == UNKNOWN) {
                trimmed.ifBlank { AVAILABLE.displayLabel }
            } else {
                status.displayLabel
            }
        }
    }
}
