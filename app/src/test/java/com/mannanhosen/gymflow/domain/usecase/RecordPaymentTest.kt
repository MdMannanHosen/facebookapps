package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordPaymentTest {

    private val useCase = RecordPayment()

    @Test
    fun `valid partial payment is accepted`() {
        val result = useCase(
            memberId = "member",
            membershipId = "membership",
            amountInput = "250.50",
            method = PaymentMethod.CASH,
            reference = null,
            note = null,
            dueAmount = 1000.0,
            idempotencyKey = "key"
        )
        assertTrue(result is PaymentValidation.Valid)
        val input = (result as PaymentValidation.Valid).input
        assertEquals(250.5, input.amount, 0.0001)
        assertEquals(PaymentMethod.CASH, input.method)
    }

    @Test
    fun `blank amount is rejected`() {
        val result = useCase(
            memberId = "member",
            membershipId = null,
            amountInput = "",
            method = PaymentMethod.CASH,
            reference = null,
            note = null,
            dueAmount = 1000.0,
            idempotencyKey = "key"
        )
        assertTrue(result is PaymentValidation.Invalid)
        assertEquals(PaymentError.AMOUNT_REQUIRED, (result as PaymentValidation.Invalid).error)
    }

    @Test
    fun `non positive amount is rejected`() {
        val result = useCase(
            memberId = "member",
            membershipId = null,
            amountInput = "0",
            method = PaymentMethod.CASH,
            reference = null,
            note = null,
            dueAmount = 1000.0,
            idempotencyKey = "key"
        )
        assertEquals(PaymentError.AMOUNT_NOT_POSITIVE, (result as PaymentValidation.Invalid).error)
    }

    @Test
    fun `amount above the due is rejected`() {
        val result = useCase(
            memberId = "member",
            membershipId = null,
            amountInput = "1500",
            method = PaymentMethod.CASH,
            reference = null,
            note = null,
            dueAmount = 1000.0,
            idempotencyKey = "key"
        )
        assertEquals(PaymentError.EXCEEDS_DUE, (result as PaymentValidation.Invalid).error)
    }

    @Test
    fun `full payment equal to the due is accepted`() {
        val result = useCase(
            memberId = "member",
            membershipId = null,
            amountInput = "1000",
            method = PaymentMethod.BKASH,
            reference = "TXN1",
            note = null,
            dueAmount = 1000.0,
            idempotencyKey = "key"
        )
        assertTrue(result is PaymentValidation.Valid)
    }

    @Test
    fun `amount is accepted when no due amount is supplied`() {
        val result = useCase(
            memberId = "member",
            membershipId = null,
            amountInput = "5000",
            method = PaymentMethod.CASH,
            reference = null,
            note = null,
            dueAmount = null,
            idempotencyKey = "key"
        )
        assertTrue(result is PaymentValidation.Valid)
    }
}