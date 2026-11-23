package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.repository.CreateMembershipInput
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Creates a brand-new membership for a member.
 * The backend is the source of truth for the fee and end date.
 */
class CreateMembership @Inject constructor(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        memberId: String,
        planId: String,
        startDate: LocalDate
    ): NetworkResult<Membership> {
        if (memberId.isBlank()) {
            return NetworkResult.Failure(AppError.Validation("Member is required"))
        }
        if (planId.isBlank()) {
            return NetworkResult.Failure(AppError.Validation("Plan is required"))
        }
        return repository.createMembership(
            CreateMembershipInput(
                memberId = memberId,
                planId = planId,
                startAfterCurrent = false,
                startDate = startDate
            )
        )
    }
}