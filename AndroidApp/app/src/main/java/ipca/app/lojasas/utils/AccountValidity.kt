package ipca.app.lojasas.utils

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Regras de validade de conta.
 *
 * Neste projeto, a validade termina a **31 de Agosto** (fim do ano letivo).
 * - Se a data atual ainda não passou 31 de Agosto, devolve 31/08 do ano corrente.
 * - Se já passou 31 de Agosto, devolve 31/08 do ano seguinte.
 */
object AccountValidity {

    /**
     * Calcula a próxima validade (31 de Agosto 23:59:59.999).
     */
    fun nextAugust31(now: Date = Date(), timeZone: TimeZone = TimeZone.getDefault()): Date {
        val calNow = Calendar.getInstance(timeZone).apply { time = now }
        val year = calNow.get(Calendar.YEAR)

        val aug31 = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Se já passou 31/08 deste ano, então validade é 31/08 do ano seguinte.
        if (calNow.after(aug31)) {
            aug31.add(Calendar.YEAR, 1)
        }

        return aug31.time
    }

    /**
     * True se a conta já expirou (now > validUntil).
     */
    fun isExpired(validUntil: Date, now: Date = Date()): Boolean {
        return now.after(validUntil)
    }
}
