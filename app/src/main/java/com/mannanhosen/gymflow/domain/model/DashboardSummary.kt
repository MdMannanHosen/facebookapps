package com.mannanhosen.gymflow.domain.model

/**
 * Aggregated dashboard data for a role.
 * The dashboard shows only the most important daily information.
 */
data class DashboardSummary(
    val activeMembers: Int = 0,
    val expiringSoon: Int = 0,
    val outstandingDues: Double = 0.0,
    val todaysCollection: Double = 0.0,
    val pendingPayments: Int = 0,
    val totalMembers: Int = 0,
    val recentPayments: List<Payment> = emptyList(),
    val expiringMembers: List<MemberSummary> = emptyList()
)