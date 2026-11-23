package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.MembershipStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculateMembershipStatusTest {

    private val useCase = CalculateMembershipStatus()

    @Test
    fun `today before start is PENDING`() {
        val status = useCase(
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 3, 1),
            today = LocalDate.of(2026, 1, 15)
        )
        assertEquals(MembershipStatus.PENDING, status)
    }

    @Test
    fun `today after end is EXPIRED`() {
        val status = useCase(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            today = LocalDate.of(2026, 2, 5)
        )
        assertEquals(MembershipStatus.EXPIRED, status)
    }

    @Test
    fun `today on end date is ACTIVE`() {
        val status = useCase(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            today = LocalDate.of(2026, 1, 31)
        )
        assertEquals(MembershipStatus.ACTIVE, status)
    }

    @Test
    fun `within window before end is EXPIRING_SOON`() {
        val status = useCase(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            today = LocalDate.of(2026, 1, 25),
            expiringWindowDays = 7
        )
        assertEquals(MembershipStatus.EXPIRING_SOON, status)
    }

    @Test
    fun `outside window is ACTIVE`() {
        val status = useCase(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            today = LocalDate.of(2026, 1, 10),
            expiringWindowDays = 7
        )
        assertEquals(MembershipStatus.ACTIVE, status)
    }
}