package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.MembershipPlan
import java.time.LocalDate

data class CreateMembershipInput(
    val memberId: String,
    val planId: String,
    /** When true, renewal starts the day after the current membership ends. */
    val startAfterCurrent: Boolean,
    val startDate: LocalDate
)

interface MembershipRepository {

    suspend fun getPlans(activeOnly: Boolean = true): NetworkResult<List<MembershipPlan>>

    suspend fun getMembershipsForMember(memberId: String): NetworkResult<List<Membership>>

    suspend fun createMembership(input: CreateMembershipInput): NetworkResult<Membership>

    suspend fun getExpiringMembers(daysAhead: Int = 7): NetworkResult<List<MemberSummary>>
}