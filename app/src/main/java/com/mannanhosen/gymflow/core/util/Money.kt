package com.mannanhosen.gymflow.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money helpers. The app stores monetary values as minor-unit-free BigDecimal-like
 * Doubles coming from the backend, but all arithmetic uses [BigDecimal] to avoid
 * floating point drift. Display uses Bangladeshi Taka (BDT).
 */
object Money {

    /** Currency code for the gym's locale. */
    const val CURRENCY_CODE = "BDT"

    fun of(value: String?): BigDecimal =
        value?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        runCatching { BigDecimal(this) }.getOrNull()

    fun format(amount: Double): String = format(BigDecimal.valueOf(amount))

    fun format(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalFormatSymbols = this
        }
        val df = DecimalFormat("#,##0.00", symbols)
        return df.format(amount.setScale(2, RoundingMode.HALF_UP))
    }

    /** Format with the ৳ symbol prefix, e.g. ৳1,250.00 */
    fun withSymbol(amount: Double): String = "৳${format(amount)}"

    fun withSymbol(amount: BigDecimal): String = "৳${format(amount)}"

    /**
     * due = membershipFee - successfulPayments + refunds.
     * Never negative.
     */
    fun calculateDue(
        membershipFee: Double,
        successfulPayments: Double,
        refunds: Double
    ): BigDecimal {
        val due = BigDecimal.valueOf(membershipFee)
            .subtract(BigDecimal.valueOf(successfulPayments))
            .add(BigDecimal.valueOf(refunds))
        return if (due < BigDecimal.ZERO) BigDecimal.ZERO else due.setScale(2, RoundingMode.HALF_UP)
    }
}