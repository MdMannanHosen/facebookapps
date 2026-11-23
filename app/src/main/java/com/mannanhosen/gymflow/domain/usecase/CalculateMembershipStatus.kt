package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.MembershipStatus
import java.time.LocalDate
import javax.inject.Inject

/**
 * Computes a membership's status from its dates.
 *
 * Rules:
 * - today before start      -> PENDING
 * - today after end         -> EXPIRED
 * - today within [window] of end -> EXPIRING_SOON
 * - otherwise               -> ACTIVE
 *
 * FROZEN is an administrative state set explicitly and is not inferred here.
 */
class CalculateMembershipStatus @Inject constructor() {

    operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate,
        expiringWindowDays: Long = 7
    ): MembershipStatus = when {
        today.isBefore(startDate) -> MembershipStatus.PENDING
        today.isAfter(endDate) -> MembershipStatus.EXPIRED
        !today.isBefore(endDate.minusDays(expiringWindowDays)) -> MembershipStatus.EXPIRING_SOON
        else -> MembershipStatus.ACTIVE
    }
}