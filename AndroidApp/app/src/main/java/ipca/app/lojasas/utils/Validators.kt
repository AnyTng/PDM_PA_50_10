package ipca.app.lojasas.utils

import android.util.Patterns
import java.util.Calendar

/**
 * Validações reutilizáveis para formulários.
 *
 * As regras aqui implementadas são intencionalmente focadas em Portugal (ex.:
 * Código Postal NNNN-NNN) e em requisitos de negócio do projeto.
 */
object Validators {

    /** Regra de negócio: não permitir submissões de pessoas com menos de X anos. */
    const val MIN_AGE_YEARS: Int = 10

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun isValidMecanografico(value: String): Boolean {
        // Ex.: f12345
        return Regex("^[a-zA-Z]\\d+$").matches(value.trim())
    }

    /**
     * Normaliza Código Postal PT para o formato NNNN-NNN.
     * Aceita entrada com/sem hífen, com espaços, etc.
     */
    fun normalizePostalCodePT(value: String): String? {
        val digits = value.filter { it.isDigit() }
        if (digits.length != 7) return null
        return digits.substring(0, 4) + "-" + digits.substring(4, 7)
    }

    fun isValidPostalCodePT(value: String): Boolean = normalizePostalCodePT(value) != null

    /**
     * Normaliza contacto PT (9 dígitos). Aceita "#########" ou "+351#########".
     * Devolve apenas os 9 dígitos, ou null se inválido.
     */
    fun normalizePhonePT(value: String): String? {
        val compact = value.replace("\\s".toRegex(), "")
        val digits = compact.filter { it.isDigit() }

        return when {
            digits.length == 9 -> digits
            digits.length == 12 && digits.startsWith("351") -> digits.substring(3)
            else -> null
        }
    }

    fun isValidPhonePT(value: String): Boolean = normalizePhonePT(value) != null

    /**
     * Normaliza NIF para 9 dígitos (sem espaços).
     */
    fun normalizeNif(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return if (digits.length == 9) digits else null
    }

    /**
     * Valida NIF (checksum). Não impõe regras de prefixo.
     */
    fun isValidNif(value: String): Boolean {
        val nif = normalizeNif(value) ?: return false
        val nums = nif.map { it.digitToInt() }

        var sum = 0
        // Pesos 9..2 para os primeiros 8 dígitos
        for (i in 0 until 8) {
            sum += nums[i] * (9 - i)
        }

        val mod = sum % 11
        val checkDigit = if (mod < 2) 0 else 11 - mod
        return checkDigit == nums[8]
    }

    /**
     * Calcula idade em anos completos.
     */
    fun ageYearsFromBirthMillis(birthMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
        val birth = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }

        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

        val nowMonth = now.get(Calendar.MONTH)
        val birthMonth = birth.get(Calendar.MONTH)
        if (nowMonth < birthMonth || (nowMonth == birthMonth && now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        return age
    }

    fun isAgeAtLeast(birthMillis: Long, minYears: Int = MIN_AGE_YEARS, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (birthMillis > nowMillis) return false
        return ageYearsFromBirthMillis(birthMillis, nowMillis) >= minYears
    }

    /**
     * Devolve o maxDate (DatePicker) para garantir idade mínima.
     */
    fun maxBirthDateForMinAgeMillis(minYears: Int = MIN_AGE_YEARS, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.add(Calendar.YEAR, -minYears)
        return cal.timeInMillis
    }
}
