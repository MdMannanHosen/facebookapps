package com.mannanhosen.gymflow.domain.model

import java.time.Instant

enum class NotificationType { PAYMENT_DUE, MEMBERSHIP_EXPIRING, PAYMENT_RECEIVED, REPORT, GENERAL }

/** A user-facing notification delivered in-app (push handled separately by FCM). */
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val createdAt: Instant
)