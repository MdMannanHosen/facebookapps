package com.mannanhosen.gymflow.domain.model

import java.time.Instant

enum class PaymentMethod {
    CASH, ONLINE, BKASH, NAGAD, CARD, BANK;

    companion object {
        fun from(value: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASH
    }
}

enum class PaymentStatus {
    PENDING, SUCCESS, FAILED, REFUNDED;

    companion object {
        fun from(value: String?): PaymentStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

enum class PaymentEventType { CREATED, VERIFIED, FAILED, REFUNDED }

/**
 * A payment record. Only SUCCESS payments reduce outstanding dues.
 * Payment history is append-only; records are never silently deleted.
 */
data class Payment(
    val id: String,
    val memberId: String,
    val membershipId: String? = null,
    val amount: Double,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val paidAt: Instant? = null,
    val reference: String? = null,
    val note: String? = null,
    val collectedBy: String? = null
) {
    val isSuccessful: Boolean get() = status == PaymentStatus.SUCCESS
}

/** Immutable ledger entry describing a change to a payment's lifecycle. */
data class PaymentEvent(
    val id: String,
    val paymentId: String,
    val type: PaymentEventType,
    val amount: Double,
    val createdAt: Instant
)