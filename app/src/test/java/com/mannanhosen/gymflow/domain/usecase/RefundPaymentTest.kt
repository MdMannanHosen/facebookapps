package com.mannanhosen.gymflow.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefundPaymentTest {

    private val useCase = RefundPayment()

    @Test
    fun `valid partial refund is accepted`() {
        val result = useCase(
            paymentId = "pay-1",
            amountInput = "200.50",
            reason = "Member overpaid",
            paidAmount = 1000.0,
            alreadyRefunded = 0.0,
            idempotencyKey = "key"
        )

        assertTrue(result is RefundValidation.Valid)
        val input = (result as RefundValidation.Valid).input
        assertEquals("pay-1", input.paymentId)
        assertEquals(200.5, input.amount, 0.0001)
        assertEquals("Member overpaid", input.reason)
        assertEquals("key", input.idempotencyKey)
    }

    @Test
    fun `blank reason becomes null`() {
        val result = useCase(
            paymentId = "pay-1",
            amountInput = "100",
            reason = "   ",
            paidAmount = 1000.0,
            alreadyRefunded = 0.0,
            idempotencyKey = "key"
        )

        assertTrue(result is RefundValidation.Valid)
        assertNull((result as RefundValidation.Valid).input.reason)
    }

    @Test
    fun `missing payment id is rejected`() {
        val result = useCase(
            paymentId = "",
            amountInput = "100",
            reason = null,
            paidAmount = 1000.0,
            alreadyRefunded = 0.0,
            idempotencyKey = "key"
        )

        assertEquals(RefundValidation.Invalid(RefundError.PAYMENT_REQUIRED), result)
    }

    @Test
    fun `blank amount is rejected`() {
        val result = useCase(
            paymentId = "pay-1",
            amountInput = "",
            reason = null,
            paidAmount = 1000.0,
            alreadyRefunded = 0.0,
            idempotencyKey = "key"
        )

        assertEquals(RefundValidation.Invalid(RefundError.AMOUNT_REQUIRED), result)
    }

    @Test
    fun `zero and negative amounts are rejected`() {
        val zero = useCase("pay-1", "0", null, 1000.0, 0.0, "key")
        val negative = useCase("pay-1", "-50", null, 1000.0, 0.0, "key")

        assertEquals(RefundValidation.Invalid(RefundError.AMOUNT_NOT_POSITIVE), zero)
        assertEquals(RefundValidation.Invalid(RefundError.AMOUNT_NOT_POSITIVE), negative)
    }

    @Test
    fun `amount above the paid total is rejected`() {
        val result = useCase("pay-1", "1500", null, 1000.0, 0.0, "key")

        assertEquals(RefundValidation.Invalid(RefundError.EXCEEDS_REFUNDABLE), result)
    }

    @Test
    fun `amount above the remaining balance is rejected`() {
        val result = useCase("pay-1", "900", null, 1000.0, 300.0, "key")

        assertEquals(RefundValidation.Invalid(RefundError.EXCEEDS_REFUNDABLE), result)
    }

    @Test
    fun `refunding the exact remaining balance is accepted`() {
        val result = useCase("pay-1", "700", null, 1000.0, 300.0, "key")

        assertTrue(result is RefundValidation.Valid)
        assertEquals(700.0, (result as RefundValidation.Valid).input.amount, 0.0001)
    }

    @Test
    fun `fully refunded payment has nothing left to refund`() {
        val result = useCase("pay-1", "1", null, 1000.0, 1000.0, "key")

        assertEquals(RefundValidation.Invalid(RefundError.EXCEEDS_REFUNDABLE), result)
    }
}