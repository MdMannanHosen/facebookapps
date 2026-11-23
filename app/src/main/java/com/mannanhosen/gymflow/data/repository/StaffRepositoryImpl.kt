package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.asJsonArray
import com.mannanhosen.gymflow.data.remote.dto.ProfileDto
import com.mannanhosen.gymflow.data.remote.dto.StaffPermissionsDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.data.remote.parseJson
import com.mannanhosen.gymflow.domain.model.StaffMember
import com.mannanhosen.gymflow.domain.model.StaffPermissions
import com.mannanhosen.gymflow.domain.repository.StaffRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : StaffRepository {

    override suspend fun getStaffMembers(): NetworkResult<List<StaffMember>> {
        val result = client.get(
            "/profiles",
            mapOf(
                "role" to "eq.STAFF",
                "select" to "*",
                "order" to "full_name.asc"
            )
        )
        return result.parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(ProfileDto.serializer(), item)
                        .toStaffMember()
                }.getOrNull()
            }
        }
    }

    override suspend fun getPermissions(staffId: String): NetworkResult<StaffPermissions> {
        val result = client.get(
            "/staff_permissions",
            mapOf("staff_id" to "eq.$staffId", "select" to "*", "limit" to "1")
        )
        // PostgREST answers with an empty array when the staff member has no row.
        // That is a normal state, not a failure: treat it as "no grants yet"
        // instead of letting parseFirst surface a parse error.
        return result.parseJson { element ->
            val rows = element.asJsonArray()
            if (rows.isEmpty()) {
                StaffPermissions.none(staffId)
            } else {
                SupabaseConfig.json
                    .decodeFromJsonElement(StaffPermissionsDto.serializer(), rows.first())
                    .toDomain()
            }
        }
    }

    override suspend fun savePermissions(permissions: StaffPermissions): NetworkResult<Unit> {
        // Upsert on staff_id: an owner can grant a brand new staff member without
        // creating the row first.
        val result = client.post(
            path = "/staff_permissions",
            query = mapOf("on_conflict" to "staff_id"),
            prefer = "resolution=merge-duplicates,return=minimal",
            body = buildJsonObject {
                put("staff_id", permissions.staffId)
                put("can_manage_members", permissions.canManageMembers)
                put("can_receive_payments", permissions.canReceivePayments)
                put("can_renew_memberships", permissions.canRenewMemberships)
                put("can_view_reports", permissions.canViewReports)
                put("can_manage_plans", permissions.canManagePlans)
                put("can_manage_staff", permissions.canManageStaff)
            }
        )
        return when (result) {
            is NetworkResult.Success -> NetworkResult.success(Unit)
            is NetworkResult.Failure -> result
        }
    }

    private fun ProfileDto.toStaffMember() = StaffMember(
        id = id,
        fullName = fullName.ifBlank { email.ifBlank { "Staff" } },
        email = email
    )
}