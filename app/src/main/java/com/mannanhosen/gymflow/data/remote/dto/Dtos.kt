package com.mannanhosen.gymflow.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * REST DTOs mapped 1:1 to PostgreSQL columns (snake_case).
 * Domain models are kept separate; mapping lives in the mapper package.
 */

@Serializable
data class ProfileDto(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val role: String = "MEMBER",
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MemberDto(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    // Nullable: members created from an auth signup have no phone yet.
    val phone: String? = null,
    val email: String? = null,
    val status: String = "ACTIVE",
    @SerialName("joined_at") val joinedAt: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MembershipPlanDto(
    val id: String = "",
    val name: String = "",
    @SerialName("duration_months") val durationMonths: Int = 1,
    val fee: Double = 0.0,
    val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class MembershipDto(
    val id: String = "",
    @SerialName("member_id") val memberId: String = "",
    @SerialName("plan_id") val planId: String = "",
    @SerialName("plan_name") val planName: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    val fee: Double = 0.0,
    val status: String = "ACTIVE",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PaymentDto(
    val id: String = "",
    @SerialName("member_id") val memberId: String = "",
    @SerialName("membership_id") val membershipId: String? = null,
    val amount: Double = 0.0,
    val method: String = "CASH",
    val status: String = "PENDING",
    @SerialName("paid_at") val paidAt: String? = null,
    val reference: String? = null,
    val note: String? = null,
    @SerialName("collected_by") val collectedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationDto(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "GENERAL",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class StaffPermissionsDto(
    @SerialName("staff_id") val staffId: String = "",
    @SerialName("can_manage_members") val canManageMembers: Boolean = false,
    @SerialName("can_receive_payments") val canReceivePayments: Boolean = false,
    @SerialName("can_renew_memberships") val canRenewMemberships: Boolean = false,
    @SerialName("can_view_reports") val canViewReports: Boolean = false,
    @SerialName("can_manage_plans") val canManagePlans: Boolean = false,
    @SerialName("can_manage_staff") val canManageStaff: Boolean = false
)

/** Local DTO used to represent the joined member + membership + due row. */
@Serializable
data class MemberOverviewDto(
    val member: MemberDto,
    @SerialName("current_membership") val currentMembership: MembershipDto? = null,
    @SerialName("due_amount") val dueAmount: Double = 0.0
)