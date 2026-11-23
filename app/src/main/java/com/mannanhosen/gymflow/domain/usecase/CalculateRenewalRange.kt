package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.Membership
import java.time.LocalDate
import javax.inject.Inject

data class MembershipRange(val startDate: LocalDate, val endDate: LocalDate)

/**
 * Computes the start/end dates for a new or renewed membership.
 *
 * Handles:
 * - Brand new membership starting today.
 * - Early renewal: starts the day after the current membership ends so no days
 *   are lost.
 * - Expired membership: starts today (the previous term is already over).
 */
class CalculateRenewalRange @Inject constructor() {

    operator fun invoke(
        currentMembership: Membership?,
        durationMonths: Int,
        requestedStart: LocalDate,
        today: LocalDate = LocalDate.now(),
        startAfterCurrent: Boolean = true
    ): MembershipRange {
        val start = when {
            // Early renewal against a still-valid membership.
            startAfterCurrent &&
                currentMembership != null &&
                !today.isAfter(currentMembership.endDate) ->
                currentMembership.endDate.plusDays(1)

            // Brand new or expired: start on the requested date (not in the past).
            else -> if (requestedStart.isBefore(today)) today else requestedStart
        }
        val end = start.plusMonths(durationMonths.toLong()).minusDays(1)
        return MembershipRange(startDate = start, endDate = end)
    }
}