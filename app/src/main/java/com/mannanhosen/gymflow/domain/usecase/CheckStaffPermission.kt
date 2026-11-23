package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.StaffPermissions
import com.mannanhosen.gymflow.domain.model.UserRole
import javax.inject.Inject

/**
 * Central authorization check used by the UI to gate actions.
 *
 * Server-side RLS and RPC validation remain authoritative; this prevents the UI
 * from offering actions the user cannot perform.
 */
class CheckStaffPermission @Inject constructor() {

    operator fun invoke(role: UserRole, permissions: StaffPermissions?): Boolean = when (role) {
        UserRole.OWNER -> true
        UserRole.MEMBER -> false
        UserRole.STAFF -> permissions != null
    }

    fun canManageMembers(role: UserRole, p: StaffPermissions?): Boolean =
        role == UserRole.OWNER || (role == UserRole.STAFF && (p?.canManageMembers == true))

    fun canReceivePayments(role: UserRole, p: StaffPermissions?): Boolean =
        role == UserRole.OWNER || (role == UserRole.STAFF && (p?.canReceivePayments == true))

    fun canRenewMemberships(role: UserRole, p: StaffPermissions?): Boolean =
        role == UserRole.OWNER || (role == UserRole.STAFF && (p?.canRenewMemberships == true))

    fun canViewReports(role: UserRole, p: StaffPermissions?): Boolean =
        role == UserRole.OWNER || (role == UserRole.STAFF && (p?.canViewReports == true))
}