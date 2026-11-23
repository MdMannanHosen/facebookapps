package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.repository.RefundPaymentInput
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class RefundError {
    PAYMENT_REQUIRED,
    AMOUNT_REQUIRED,
    AMOUNT_NOT_POSITIVE,
    EXCEEDS_REFUNDABLE
}

sealed interface RefundValidation {
    data class Valid(val input: RefundPaymentInput) : RefundValidation
    data class Invalid(val error: RefundError) : RefundValidation
}

/**
 * Validates and normalizes a refund before it reaches the repository.
 *
 * Rules:
 * - The payment must be known and its refundable total is paid minus refunded.
 * - The amount is required and strictly positive.
 * - A refund may never exceed what is left of the original payment.
 * - Every submission carries an idempotency key to prevent duplicate writes.
 *
 * The server re-checks all of this inside `refund_payment`; this only gives the
 * user a fast, specific error message.
 */
class RefundPayment @Inject constructor() {

    operator fun invoke(
        paymentId: String?,
        amountInput: String?,
        reason: String?,
        paidAmount: Double,
        alreadyRefunded: Double,
        idempotencyKey: String
    ): RefundValidation {
        if (paymentId.isNullOrBlank()) {
            return RefundValidation.Invalid(RefundError.PAYMENT_REQUIRED)
        }

        val amount = amountInput?.trim()?.toBigDecimalOrNull()
            ?: return RefundValidation.Invalid(RefundError.AMOUNT_REQUIRED)

        if (amount <= BigDecimal.ZERO) {
            return RefundValidation.Invalid(RefundError.AMOUNT_NOT_POSITIVE)
        }

        val refundable = BigDecimal.valueOf(paidAmount)
            .subtract(BigDecimal.valueOf(alreadyRefunded))
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        if (amount > refundable) {
            return RefundValidation.Invalid(RefundError.EXCEEDS_REFUNDABLE)
        }

        return RefundValidation.Valid(
            RefundPaymentInput(
                paymentId = paymentId,
                amount = amount.setScale(2, RoundingMode.HALF_UP).toDouble(),
                reason = reason?.trim()?.ifBlank { null },
                idempotencyKey = idempotencyKey
            )
        )
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        runCatching { BigDecimal(this) }.getOrNull()
}