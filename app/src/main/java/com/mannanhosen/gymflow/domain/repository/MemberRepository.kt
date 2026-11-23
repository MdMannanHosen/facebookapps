package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Member
import com.mannanhosen.gymflow.domain.model.MemberSummary
import java.time.LocalDate

enum class MemberFilter { ALL, ACTIVE, EXPIRING, EXPIRED, WITH_DUES }

data class NewMemberInput(
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val joinedAt: LocalDate
)

interface MemberRepository {

    suspend fun getMembers(
        query: String? = null,
        filter: MemberFilter = MemberFilter.ALL
    ): NetworkResult<List<MemberSummary>>

    suspend fun getMember(id: String): NetworkResult<Member>

    suspend fun createMember(input: NewMemberInput): NetworkResult<Member>

    suspend fun updateMember(id: String, input: NewMemberInput): NetworkResult<Member>

    suspend fun deleteMember(id: String): NetworkResult<Unit>
}