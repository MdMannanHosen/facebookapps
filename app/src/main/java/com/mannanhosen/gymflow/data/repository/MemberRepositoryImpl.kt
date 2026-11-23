package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.MemberDto
import com.mannanhosen.gymflow.data.remote.dto.MemberOverviewDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.data.remote.parseJson
import com.mannanhosen.gymflow.data.remote.parseFirst
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.domain.model.Member
import com.mannanhosen.gymflow.domain.model.MemberStatus
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.repository.MemberFilter
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.NewMemberInput
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : MemberRepository {

    override suspend fun getMembers(
        query: String?,
        filter: MemberFilter
    ): NetworkResult<List<MemberSummary>> {
        val result = client.get("/member_overview", mapOf("select" to "*"))
        return result.parseJson { element ->
            val array = element.asArrayOrEmpty()
            array.mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json.decodeFromJsonElement(
                        MemberOverviewDto.serializer(),
                        item
                    ).toDomain()
                }.getOrNull()
            }.let { all -> applyFilter(all, query, filter) }
        }
    }

    override suspend fun getMember(id: String): NetworkResult<Member> {
        val result = client.get(
            "/members",
            mapOf("id" to "eq.$id", "select" to "*", "limit" to "1")
        )
        return result.parseFirst { element ->
            SupabaseConfig.json.decodeFromJsonElement(MemberDto.serializer(), element).toDomain()
        }
    }

    override suspend fun createMember(input: NewMemberInput): NetworkResult<Member> {
        val result = client.post(
            path = "/members",
            prefer = "return=representation",
            body = buildJsonObject {
                put("full_name", input.fullName)
                put("phone", input.phone)
                input.email?.let { put("email", it) }
                put("status", MemberStatus.ACTIVE.name)
                put("joined_at", input.joinedAt.toString())
            }
        )
        return result.parseFirst { element ->
            SupabaseConfig.json.decodeFromJsonElement(MemberDto.serializer(), element).toDomain()
        }
    }

    override suspend fun updateMember(id: String, input: NewMemberInput): NetworkResult<Member> {
        val result = client.patch(
            path = "/members",
            prefer = "return=representation",
            query = mapOf("id" to "eq.$id"),
            body = buildJsonObject {
                put("full_name", input.fullName)
                put("phone", input.phone)
                input.email?.let { put("email", it) }
                put("joined_at", input.joinedAt.toString())
            }
        )
        return result.parseFirst { element ->
            SupabaseConfig.json.decodeFromJsonElement(MemberDto.serializer(), element).toDomain()
        }
    }

    override suspend fun deleteMember(id: String): NetworkResult<Unit> {
        return when (val result = client.delete("/members", mapOf("id" to "eq.$id"))) {
            is NetworkResult.Success -> NetworkResult.success(Unit)
            is NetworkResult.Failure -> result
        }
    }

    private fun applyFilter(
        all: List<MemberSummary>,
        query: String?,
        filter: MemberFilter
    ): List<MemberSummary> {
        val q = query?.trim().orEmpty().lowercase()
        return all.filter { summary ->
            val matchesQuery = q.isEmpty() ||
                summary.member.fullName.lowercase().contains(q) ||
                summary.member.phone.contains(q)
            val matchesFilter = when (filter) {
                MemberFilter.ALL -> true
                MemberFilter.ACTIVE -> summary.currentMembership?.status?.name == "ACTIVE"
                MemberFilter.EXPIRING -> summary.currentMembership?.status?.name == "EXPIRING_SOON"
                MemberFilter.EXPIRED -> summary.currentMembership?.status?.name == "EXPIRED"
                MemberFilter.WITH_DUES -> summary.dueAmount > 0.0
            }
            matchesQuery && matchesFilter
        }
    }
}