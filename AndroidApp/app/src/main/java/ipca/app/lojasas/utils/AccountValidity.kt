package ipca.app.lojasas.utils

import java.util.Calendar
import java.util.Date
import java.util.TimeZone


object AccountValidity {

    /**
     * Calcula a próxima validade (30 de Setembro 23:59:59.999).
     */
    fun nextSeptembem30(now: Date = Date(), timeZone: TimeZone = TimeZone.getDefault()): Date {
        val calNow = Calendar.getInstance(timeZone).apply { time = now }
        val year = calNow.get(Calendar.YEAR)

        val set30 = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 30)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Se já passou 31/08 deste ano, então validade é 31/08 do ano seguinte.
        if (calNow.after(set30)) {
            set30.add(Calendar.YEAR, 1)
        }

        return set30.time
    }

    /**
     * True se a conta já expirou (now > validUntil).
     */
    fun isExpired(validUntil: Date, now: Date = Date()): Boolean {
        return now.after(validUntil)
    }
}
