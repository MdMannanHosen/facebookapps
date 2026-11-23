package com.mannanhosen.gymflow.data.remote.mapper

import com.mannanhosen.gymflow.data.remote.dto.MemberDto
import com.mannanhosen.gymflow.data.remote.dto.MemberOverviewDto
import com.mannanhosen.gymflow.data.remote.dto.MembershipDto
import com.mannanhosen.gymflow.data.remote.dto.MembershipPlanDto
import com.mannanhosen.gymflow.data.remote.dto.NotificationDto
import com.mannanhosen.gymflow.data.remote.dto.PaymentDto
import com.mannanhosen.gymflow.data.remote.dto.ProfileDto
import com.mannanhosen.gymflow.data.remote.dto.StaffPermissionsDto
import com.mannanhosen.gymflow.domain.model.Member
import com.mannanhosen.gymflow.domain.model.MemberStatus
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.MembershipPlan
import com.mannanhosen.gymflow.domain.model.MembershipStatus
import com.mannanhosen.gymflow.domain.model.NotificationItem
import com.mannanhosen.gymflow.domain.model.NotificationType
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.model.PaymentMethod
import com.mannanhosen.gymflow.domain.model.PaymentStatus
import com.mannanhosen.gymflow.domain.model.StaffPermissions
import com.mannanhosen.gymflow.domain.model.UserRole
import java.time.Instant
import java.time.LocalDate

/** DTO -> domain mappers. Keeps API models separate from domain models. */

internal fun LocalDate?.orToday(): LocalDate = this ?: LocalDate.now()

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this.take(10)) }.getOrNull()

private fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()

fun MemberDto.toDomain(): Member = Member(
    id = id,
    fullName = fullName,
    phone = phone.orEmpty(),
    email = email?.ifBlank { null },
    status = if (status.equals("INACTIVE", true)) MemberStatus.INACTIVE else MemberStatus.ACTIVE,
    joinedAt = joinedAt.toLocalDateOrNull() ?: LocalDate.now(),
    photoUrl = photoUrl,
    createdAt = createdAt?.toInstantOrNull()
)

fun MembershipPlanDto.toDomain(): MembershipPlan = MembershipPlan(
    id = id,
    name = name,
    durationMonths = durationMonths,
    fee = fee,
    description = description,
    isActive = isActive
)

fun MembershipDto.toDomain(): Membership = Membership(
    id = id,
    memberId = memberId,
    planId = planId,
    planName = planName,
    startDate = startDate.toLocalDateOrNull() ?: LocalDate.now(),
    endDate = endDate.toLocalDateOrNull() ?: LocalDate.now(),
    fee = fee,
    status = MembershipStatus.entries.firstOrNull { it.name.equals(status, true) }
        ?: MembershipStatus.ACTIVE,
    createdAt = createdAt?.toInstantOrNull()
)

fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    memberId = memberId,
    membershipId = membershipId,
    amount = amount,
    method = PaymentMethod.from(method),
    status = PaymentStatus.from(status),
    paidAt = paidAt?.toInstantOrNull(),
    reference = reference,
    note = note,
    collectedBy = collectedBy
)

fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = id,
    title = title,
    body = body,
    type = NotificationType.entries.firstOrNull { it.name.equals(type, true) }
        ?: NotificationType.GENERAL,
    isRead = isRead,
    createdAt = createdAt.toInstantOrNull() ?: Instant.now()
)

fun StaffPermissionsDto.toDomain(): StaffPermissions = StaffPermissions(
    staffId = staffId,
    canManageMembers = canManageMembers,
    canReceivePayments = canReceivePayments,
    canRenewMemberships = canRenewMemberships,
    canViewReports = canViewReports,
    canManagePlans = canManagePlans,
    canManageStaff = canManageStaff
)

fun MemberOverviewDto.toDomain(): MemberSummary = MemberSummary(
    member = member.toDomain(),
    currentMembership = currentMembership?.toDomain(),
    dueAmount = dueAmount
)

fun ProfileDto.toRole(): UserRole = UserRole.from(role)