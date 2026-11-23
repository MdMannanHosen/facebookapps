package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.MemberOverviewDto
import com.mannanhosen.gymflow.data.remote.dto.PaymentDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.domain.model.DashboardSummary
import com.mannanhosen.gymflow.domain.model.DailyReport
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.model.UserRole
import com.mannanhosen.gymflow.domain.repository.DashboardRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds dashboard and report aggregates from the member_overview view and the
 * payments table. Aggregation is read-only; financial truth stays in the DB.
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : DashboardRepository {

    override suspend fun getDashboard(role: UserRole): NetworkResult<DashboardSummary> {
        val today = AppClock.todayInDhaka()
        val expiringLimit = today.plusDays(7)

        val overviewResult = client.get("/member_overview", mapOf("select" to "*"))
        val overviewBody = overviewResult.getOrNull()
            ?: return NetworkResult.failure(
                (overviewResult as? NetworkResult.Failure)?.error
                    ?: com.mannanhosen.gymflow.core.util.AppError.Unknown("Failed to load overview")
            )
        val overview = parseOverviews(overviewBody)

        val recentPayments = parsePayments(
            client.get(
                "/payments",
                mapOf(
                    "select" to "*",
                    "status" to "eq.SUCCESS",
                    "order" to "created_at.desc",
                    "limit" to "10"
                )
            )
        )

        val todayPayments = parsePayments(
            client.get(
                "/payments",
                mapOf(
                    "select" to "*",
                    "status" to "eq.SUCCESS",
                    "paid_at" to "gte.${AppClock.startOfDayUtc(today)}"
                )
            )
        )

        val activeMembers = overview.count { summary ->
            val status = summary.currentMembership?.status?.name
            status == "ACTIVE" || status == "EXPIRING_SOON"
        }

        val expiringMembers = overview.filter { summary ->
            val end = summary.currentMembership?.endDate
            end != null && !end.isBefore(today) && !end.isAfter(expiringLimit)
        }

        val summary = DashboardSummary(
            activeMembers = activeMembers,
            expiringSoon = expiringMembers.size,
            outstandingDues = overview.sumOf { it.dueAmount },
            todaysCollection = todayPayments.sumOf { it.amount },
            pendingPayments = 0,
            totalMembers = overview.size,
            recentPayments = recentPayments,
            expiringMembers = expiringMembers.sortedBy { it.currentMembership?.endDate }
        )
        return NetworkResult.success(summary)
    }

    override suspend fun getDailyReport(date: LocalDate): NetworkResult<DailyReport> {
        val dayStart = AppClock.startOfDayUtc(date).toString()

        val payments = parsePayments(
            client.get(
                "/payments",
                mapOf(
                    "select" to "*",
                    "status" to "eq.SUCCESS",
                    "paid_at" to "gte.$dayStart"
                )
            )
        )

        val newMembers = client.get(
            "/members",
            mapOf("joined_at" to "eq.$date", "select" to "id")
        ).parseCount()

        val renewals = client.get(
            "/memberships",
            mapOf("start_date" to "eq.$date", "select" to "id")
        ).parseCount()

        val overviewBody = client.get("/member_overview", mapOf("select" to "*")).getOrNull()
        val outstanding = overviewBody?.let(::parseOverviews)?.sumOf { it.dueAmount } ?: 0.0

        val report = DailyReport(
            date = date,
            totalCollection = payments.sumOf { it.amount },
            cashCollection = payments.filter { it.method.name == "CASH" }.sumOf { it.amount },
            onlineCollection = payments.filter { it.method.name != "CASH" }.sumOf { it.amount },
            newMembers = newMembers,
            renewals = renewals,
            paymentsCount = payments.size,
            outstandingDues = outstanding
        )
        return NetworkResult.success(report)
    }

    private fun parseOverviews(body: String): List<MemberSummary> =
        runCatching {
            SupabaseConfig.json.parseToJsonElement(body.ifBlank { "[]" }).asArrayOrEmpty()
                .mapNotNull { item ->
                    runCatching {
                        SupabaseConfig.json
                            .decodeFromJsonElement(MemberOverviewDto.serializer(), item)
                            .toDomain()
                    }.getOrNull()
                }
        }.getOrDefault(emptyList())

    private fun parsePayments(result: NetworkResult<String>): List<Payment> {
        val body = result.getOrNull() ?: return emptyList()
        return runCatching {
            SupabaseConfig.json.parseToJsonElement(body.ifBlank { "[]" }).asArrayOrEmpty()
                .mapNotNull { item ->
                    runCatching {
                        SupabaseConfig.json
                            .decodeFromJsonElement(PaymentDto.serializer(), item)
                            .toDomain()
                    }.getOrNull()
                }
        }.getOrDefault(emptyList())
    }

    private fun NetworkResult<String>.parseCount(): Int {
        val body = getOrNull() ?: return 0
        return runCatching {
            SupabaseConfig.json.parseToJsonElement(body.ifBlank { "[]" }).asArrayOrEmpty().size
        }.getOrDefault(0)
    }
}