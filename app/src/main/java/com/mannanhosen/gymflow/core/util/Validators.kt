package com.mannanhosen.gymflow.core.util

/**
 * Input validation helpers. Keep business-facing validation in one place so the
 * UI and use cases agree on rules.
 */
object Validators {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^(?:\\+?88)?01[0-9]{9}$")

    fun isValidEmail(value: String): Boolean = emailRegex.matches(value.trim())

    /** Accepts Bangladeshi mobile formats: 01XXXXXXXXX or +8801XXXXXXXXX. */
    fun isValidPhone(value: String): Boolean = phoneRegex.matches(value.trim())

    /** Normalize a Bangladeshi phone number to local 11-digit format. */
    fun normalizePhone(value: String): String {
        val digits = value.filter { it.isDigit() }
        return when {
            digits.startsWith("880") && digits.length == 13 -> "0" + digits.substring(3)
            digits.length == 11 -> digits
            else -> digits
        }
    }

    /** Convert local 11-digit to international +880 format for display/WhatsApp. */
    fun toInternationalPhone(local: String): String {
        val digits = normalizePhone(local)
        return if (digits.startsWith("0") && digits.length == 11) "+880" + digits.substring(1) else digits
    }
}