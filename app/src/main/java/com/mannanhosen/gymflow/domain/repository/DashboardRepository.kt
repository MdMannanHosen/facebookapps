package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.DashboardSummary
import com.mannanhosen.gymflow.domain.model.DailyReport
import com.mannanhosen.gymflow.domain.model.UserRole
import java.time.LocalDate

interface DashboardRepository {

    suspend fun getDashboard(role: UserRole): NetworkResult<DashboardSummary>

    suspend fun getDailyReport(date: LocalDate): NetworkResult<DailyReport>
}