package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.StaffMember
import com.mannanhosen.gymflow.domain.model.StaffPermissions

/**
 * Per-staff grants. Owners always have every permission implicitly; this
 * repository only manages the explicit STAFF rows.
 *
 * The server is authoritative: RLS restricts reads/writes to owners, and the
 * `CheckStaffPermission` use case mirrors the grants for UI gating.
 */
interface StaffRepository {

    suspend fun getStaffMembers(): NetworkResult<List<StaffMember>>

    suspend fun getPermissions(staffId: String): NetworkResult<StaffPermissions>

    suspend fun savePermissions(permissions: StaffPermissions): NetworkResult<Unit>
}