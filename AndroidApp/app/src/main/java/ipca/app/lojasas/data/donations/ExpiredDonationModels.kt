package ipca.app.lojasas.data.donations

import java.util.Date

data class ExpiredDonationEntry(
    val id: String,
    val associationName: String,
    val associationContact: String,
    val employeeId: String,
    val donationDate: Date?,
    val productIds: List<String>
)
