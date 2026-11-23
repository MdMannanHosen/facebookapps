package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.MembershipStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculateRenewalRangeTest {

    private val useCase = CalculateRenewalRange()

    private val today = LocalDate.of(2026, 1, 10)

    private fun membership(endDate: LocalDate) = Membership(
        id = "m1",
        memberId = "member",
        planId = "plan",
        planName = "Plan",
        startDate = endDate.minusMonths(1),
        endDate = endDate,
        fee = 1000.0,
        status = MembershipStatus.ACTIVE
    )

    @Test
    fun `new membership starts today and spans the plan duration`() {
        val range = useCase(
            currentMembership = null,
            durationMonths = 1,
            requestedStart = today,
            today = today
        )
        assertEquals(today, range.startDate)
        assertEquals(LocalDate.of(2026, 2, 9), range.endDate)
    }

    @Test
    fun `early renewal starts the day after current end`() {
        val current = membership(endDate = LocalDate.of(2026, 1, 31))
        val range = useCase(
            currentMembership = current,
            durationMonths = 1,
            requestedStart = today,
            today = today,
            startAfterCurrent = true
        )
        assertEquals(LocalDate.of(2026, 2, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate)
    }

    @Test
    fun `expired membership renews from today`() {
        val current = membership(endDate = LocalDate.of(2026, 1, 5))
        val range = useCase(
            currentMembership = current,
            durationMonths = 1,
            requestedStart = today,
            today = today,
            startAfterCurrent = true
        )
        assertEquals(today, range.startDate)
    }

    @Test
    fun `requesting a past start date is clamped to today`() {
        val range = useCase(
            currentMembership = null,
            durationMonths = 1,
            requestedStart = today.minusDays(5),
            today = today
        )
        assertEquals(today, range.startDate)
    }

    @Test
    fun `yearly plan spans twelve months`() {
        val range = useCase(
            currentMembership = null,
            durationMonths = 12,
            requestedStart = today,
            today = today
        )
        assertEquals(LocalDate.of(2027, 1, 9), range.endDate)
    }
}