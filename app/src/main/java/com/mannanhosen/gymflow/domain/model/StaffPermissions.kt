package com.mannanhosen.gymflow.domain.model

/**
 * Individually configurable staff permissions.
 * Server-side enforcement is authoritative; this mirrors the grant for UI gating.
 */
data class StaffPermissions(
    val staffId: String,
    val canManageMembers: Boolean = false,
    val canReceivePayments: Boolean = false,
    val canRenewMemberships: Boolean = false,
    val canViewReports: Boolean = false,
    val canManagePlans: Boolean = false,
    val canManageStaff: Boolean = false
) {
    companion object {
        /** Owner implicitly has all permissions. */
        fun owner(staffId: String) = StaffPermissions(
            staffId = staffId,
            canManageMembers = true,
            canReceivePayments = true,
            canRenewMemberships = true,
            canViewReports = true,
            canManagePlans = true,
            canManageStaff = true
        )

        /** Staff with no explicit grants: the safest default. */
        fun none(staffId: String) = StaffPermissions(staffId = staffId)
    }
}

/** A staff profile shown in the permissions screen. */
data class StaffMember(
    val id: String,
    val fullName: String,
    val email: String
)