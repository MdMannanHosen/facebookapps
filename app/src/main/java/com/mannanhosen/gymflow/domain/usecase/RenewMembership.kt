package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.repository.CreateMembershipInput
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Renews a member's membership.
 *
 * - If the current membership is still valid, the renewal is queued to start the
 *   day after it ends (early renewal, no lost days).
 * - If the current membership has expired, the renewal starts on the requested
 *   date (today by default).
 */
class RenewMembership @Inject constructor(
    private val repository: MembershipRepository,
    private val calculateRange: CalculateRenewalRange
) {
    suspend operator fun invoke(
        memberId: String,
        planId: String,
        requestedStart: LocalDate,
        today: LocalDate
    ): NetworkResult<Membership> {
        if (memberId.isBlank()) {
            return NetworkResult.Failure(AppError.Validation("Member is required"))
        }
        if (planId.isBlank()) {
            return NetworkResult.Failure(AppError.Validation("Plan is required"))
        }

        val plans = when (val r = repository.getPlans()) {
            is NetworkResult.Success -> r.data
            is NetworkResult.Failure -> return r
        }
        val plan = plans.firstOrNull { it.id == planId }
            ?: return NetworkResult.Failure(AppError.NotFound("Plan not found"))

        val memberships = when (val r = repository.getMembershipsForMember(memberId)) {
            is NetworkResult.Success -> r.data
            is NetworkResult.Failure -> return r
        }
        val current = memberships.maxByOrNull { it.endDate }

        val range = calculateRange(
            currentMembership = current,
            durationMonths = plan.durationMonths,
            requestedStart = requestedStart,
            today = today,
            startAfterCurrent = true
        )

        return repository.createMembership(
            CreateMembershipInput(
                memberId = memberId,
                planId = planId,
                startAfterCurrent = current != null && !today.isAfter(current.endDate),
                startDate = range.startDate
            )
        )
    }
}