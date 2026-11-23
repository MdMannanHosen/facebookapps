package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.NotificationDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.data.remote.parseJson
import com.mannanhosen.gymflow.domain.model.NotificationItem
import com.mannanhosen.gymflow.domain.repository.NotificationRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : NotificationRepository {

    override suspend fun getNotifications(): NetworkResult<List<NotificationItem>> {
        val result = client.get(
            "/notifications",
            mapOf("select" to "*", "order" to "created_at.desc", "limit" to "100")
        )
        return result.parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(NotificationDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
        }
    }

    override suspend fun markRead(id: String): NetworkResult<Unit> {
        return when (val result = client.patch(
            path = "/notifications",
            query = mapOf("id" to "eq.$id"),
            body = buildJsonObject { put("is_read", true) }
        )) {
            is NetworkResult.Success -> NetworkResult.success(Unit)
            is NetworkResult.Failure -> result
        }
    }
}