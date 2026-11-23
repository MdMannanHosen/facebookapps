package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.MemberOverviewDto
import com.mannanhosen.gymflow.data.remote.dto.MembershipDto
import com.mannanhosen.gymflow.data.remote.dto.MembershipPlanDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.data.remote.parseJson
import com.mannanhosen.gymflow.data.remote.parseFirst
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.MembershipPlan
import com.mannanhosen.gymflow.domain.repository.CreateMembershipInput
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : MembershipRepository {

    override suspend fun getPlans(activeOnly: Boolean): NetworkResult<List<MembershipPlan>> {
        val query = buildMap {
            put("select", "*")
            put("order", "duration_months.asc")
            if (activeOnly) put("is_active", "eq.true")
        }
        return client.get("/membership_plans", query).parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(MembershipPlanDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
        }
    }

    override suspend fun getMembershipsForMember(memberId: String): NetworkResult<List<Membership>> {
        val result = client.get(
            "/memberships",
            mapOf(
                "member_id" to "eq.$memberId",
                "select" to "*",
                "order" to "start_date.desc"
            )
        )
        return result.parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(MembershipDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
        }
    }

    override suspend fun createMembership(input: CreateMembershipInput): NetworkResult<Membership> {
        // Fee, end date and status are computed by the database RPC (authoritative).
        val result = client.post(
            path = "/rpc/create_membership",
            prefer = "return=representation",
            body = buildJsonObject {
                put("p_member_id", input.memberId)
                put("p_plan_id", input.planId)
                put("p_start_date", input.startDate.toString())
                put("p_start_after_current", input.startAfterCurrent)
            }
        )
        return result.parseFirst { element ->
            SupabaseConfig.json
                .decodeFromJsonElement(MembershipDto.serializer(), element)
                .toDomain()
        }
    }

    override suspend fun getExpiringMembers(daysAhead: Int): NetworkResult<List<MemberSummary>> {
        val today = AppClock.todayInDhaka()
        val limitDate = today.plusDays(daysAhead.toLong())
        return client.get("/member_overview", mapOf("select" to "*")).parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(MemberOverviewDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }.filter { summary ->
                val end = summary.currentMembership?.endDate ?: return@filter false
                !end.isBefore(today) && !end.isAfter(limitDate)
            }.sortedBy { it.currentMembership?.endDate }
        }
    }
}