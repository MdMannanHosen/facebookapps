package com.mannanhosen.gymflow.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CalculateDueTest {

    private val useCase = CalculateDue()

    @Test
    fun `due is fee minus successful payments`() {
        val due = useCase(
            membershipFee = 1000.0,
            successfulPayments = 400.0,
            refunds = 0.0
        )
        assertEquals(BigDecimal("600.00"), due)
    }

    @Test
    fun `partial payment leaves a remainder`() {
        val due = useCase(1000.0, 250.0, 0.0)
        assertEquals(BigDecimal("750.00"), due)
    }

    @Test
    fun `refunds increase the due`() {
        val due = useCase(1000.0, 1000.0, 200.0)
        assertEquals(BigDecimal("200.00"), due)
    }

    @Test
    fun `overpayment never yields a negative due`() {
        val due = useCase(1000.0, 1500.0, 0.0)
        assertEquals(BigDecimal.ZERO, due)
    }

    @Test
    fun `list overload sums only provided successful payments`() {
        val due = useCase(
            membershipFee = 1000.0,
            successfulPaymentAmounts = listOf(300.0, 200.0),
            refunds = 0.0
        )
        assertEquals(BigDecimal("500.00"), due)
    }
}