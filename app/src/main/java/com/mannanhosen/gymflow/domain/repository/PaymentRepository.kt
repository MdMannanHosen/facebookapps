package com.mannanhosen.gymflow.domain.repository

import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.model.PaymentMethod

data class RecordPaymentInput(
    val memberId: String,
    val membershipId: String? = null,
    val amount: Double,
    val method: PaymentMethod,
    val reference: String? = null,
    val note: String? = null,
    /** Idempotency key to prevent duplicate financial writes on retry. */
    val idempotencyKey: String
)

interface PaymentRepository {

    suspend fun recordPayment(input: RecordPaymentInput): NetworkResult<Payment>

    suspend fun getPaymentHistory(memberId: String): NetworkResult<List<Payment>>

    suspend fun getRecentPayments(limit: Int = 10): NetworkResult<List<Payment>>

    suspend fun getOutstandingDues(): NetworkResult<List<MemberSummary>>
}