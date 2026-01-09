package ipca.app.lojasas.data.calendar

import java.util.Date

enum class EventType {
    CAMPAIGN_START,
    CAMPAIGN_END,
    PRODUCT_EXPIRY,
    BASKET_DELIVERY
}

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: Date,
    val type: EventType,
    val description: String = ""
)
