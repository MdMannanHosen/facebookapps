package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.NotificationItem

interface NotificationRepository {

    suspend fun getNotifications(): NetworkResult<List<NotificationItem>>

    suspend fun markRead(id: String): NetworkResult<Unit>
}