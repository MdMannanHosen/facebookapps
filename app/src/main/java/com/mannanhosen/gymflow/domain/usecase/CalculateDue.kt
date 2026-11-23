package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.core.util.Money
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Calculates outstanding dues.
 *
 * Due = Membership Fee - Successful Payments + Refunds.
 * Only SUCCESS payments reduce dues. Result is never negative.
 *
 * This mirrors the authoritative backend calculation; the client only displays it.
 */
class CalculateDue @Inject constructor() {

    operator fun invoke(
        membershipFee: Double,
        successfulPayments: Double,
        refunds: Double
    ): BigDecimal = Money.calculateDue(membershipFee, successfulPayments, refunds)

    /** Convenience overload for a list of payment amounts (only successful). */
    operator fun invoke(
        membershipFee: Double,
        successfulPaymentAmounts: List<Double>,
        refunds: Double = 0.0
    ): BigDecimal = invoke(
        membershipFee = membershipFee,
        successfulPayments = successfulPaymentAmounts.sum(),
        refunds = refunds
    )
}