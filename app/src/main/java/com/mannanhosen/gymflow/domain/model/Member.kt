package com.mannanhosen.gymflow.domain.model

import java.time.Instant
import java.time.LocalDate

enum class MemberStatus { ACTIVE, INACTIVE }

/** Core member profile. Membership and payment states are tracked separately. */
data class Member(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val joinedAt: LocalDate,
    val photoUrl: String? = null,
    val createdAt: Instant? = null
)

/** Member row enriched with their current membership and outstanding due. */
data class MemberSummary(
    val member: Member,
    val currentMembership: Membership? = null,
    val dueAmount: Double = 0.0
)