package com.mannanhosen.gymflow.domain.model

import java.time.Instant
import java.time.LocalDate

/** Membership lifecycle status. Kept separate from [PaymentStatus]. */
enum class MembershipStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    FROZEN,
    PENDING
}

/**
 * A membership record. A member may have multiple records (renewals create new
 * rows). Renewal start/end logic lives in use cases, never in the UI.
 */
data class Membership(
    val id: String,
    val memberId: String,
    val planId: String,
    val planName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val fee: Double,
    val status: MembershipStatus,
    val createdAt: Instant? = null
) {
    /** A membership covers [today] inclusive when today is between start and end. */
    fun covers(today: LocalDate): Boolean =
        !today.isBefore(startDate) && !today.isAfter(endDate)
}