package com.mannanhosen.gymflow.domain.model

import java.time.LocalDate

/** Business-day report shown to the owner/staff. */
data class DailyReport(
    val date: LocalDate,
    val totalCollection: Double = 0.0,
    val cashCollection: Double = 0.0,
    val onlineCollection: Double = 0.0,
    val newMembers: Int = 0,
    val renewals: Int = 0,
    val paymentsCount: Int = 0,
    val outstandingDues: Double = 0.0
)