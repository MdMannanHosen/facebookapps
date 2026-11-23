package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.PaymentMethod
import com.mannanhosen.gymflow.domain.repository.RecordPaymentInput
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class PaymentError {
    AMOUNT_REQUIRED,
    AMOUNT_NOT_POSITIVE,
    EXCEEDS_DUE
}

sealed interface PaymentValidation {
    data class Valid(val input: RecordPaymentInput) : PaymentValidation
    data class Invalid(val error: PaymentError) : PaymentValidation
}

/**
 * Validates and normalizes a payment before it reaches the repository.
 *
 * Rules:
 * - Amount is required and strictly positive.
 * - Amount may not exceed the outstanding due when a due amount is supplied
 *   (partial payments are allowed, overpayment is not).
 * - Every submission carries an idempotency key to prevent duplicate writes.
 */
class RecordPayment @Inject constructor() {

    operator fun invoke(
        memberId: String,
        membershipId: String?,
        amountInput: String?,
        method: PaymentMethod,
        reference: String?,
        note: String?,
        dueAmount: Double?,
        idempotencyKey: String
    ): PaymentValidation {
        val amount = amountInput?.trim()?.toBigDecimalOrNull()
            ?: return PaymentValidation.Invalid(PaymentError.AMOUNT_REQUIRED)

        if (amount <= BigDecimal.ZERO) {
            return PaymentValidation.Invalid(PaymentError.AMOUNT_NOT_POSITIVE)
        }

        if (dueAmount != null) {
            val due = BigDecimal.valueOf(dueAmount).setScale(2, RoundingMode.HALF_UP)
            if (amount > due) {
                return PaymentValidation.Invalid(PaymentError.EXCEEDS_DUE)
            }
        }

        return PaymentValidation.Valid(
            RecordPaymentInput(
                memberId = memberId,
                membershipId = membershipId,
                amount = amount.setScale(2, RoundingMode.HALF_UP).toDouble(),
                method = method,
                reference = reference?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                idempotencyKey = idempotencyKey
            )
        )
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        runCatching { BigDecimal(this) }.getOrNull()
}