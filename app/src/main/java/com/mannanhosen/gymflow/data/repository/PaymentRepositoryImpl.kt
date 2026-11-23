package com.mannanhosen.gymflow.data.repository

import com.mannanhosen.gymflow.core.network.SupabaseClient
import com.mannanhosen.gymflow.core.network.SupabaseConfig
import com.mannanhosen.gymflow.core.session.SessionManager
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.data.remote.dto.MemberOverviewDto
import com.mannanhosen.gymflow.data.remote.dto.PaymentDto
import com.mannanhosen.gymflow.data.remote.mapper.asArrayOrEmpty
import com.mannanhosen.gymflow.data.remote.mapper.toDomain
import com.mannanhosen.gymflow.data.remote.parseFirst
import com.mannanhosen.gymflow.data.remote.parseJson
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import com.mannanhosen.gymflow.domain.repository.RecordPaymentInput
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val sessionManager: SessionManager
) : PaymentRepository {

    override suspend fun recordPayment(input: RecordPaymentInput): NetworkResult<Payment> {
        // All financial validation, due calculation and duplicate protection is
        // enforced server-side inside the record_payment RPC.
        val result = client.post(
            path = "/rpc/record_payment",
            prefer = "return=representation",
            body = buildJsonObject {
                put("p_member_id", input.memberId)
                input.membershipId?.let { put("p_membership_id", it) }
                put("p_amount", input.amount)
                put("p_method", input.method.name)
                input.reference?.let { put("p_reference", it) }
                input.note?.let { put("p_note", it) }
                put("p_idempotency_key", input.idempotencyKey)
                put("p_collected_by", sessionManager.current()?.userId)
            }
        )
        return result.parseFirst { element ->
            SupabaseConfig.json
                .decodeFromJsonElement(PaymentDto.serializer(), element)
                .toDomain()
        }
    }

    override suspend fun refundPayment(input: RefundPaymentInput): NetworkResult<Payment> {
        // Cap enforcement, idempotency and audit all happen inside the RPC.
        val result = client.post(
            path = "/rpc/refund_payment",
            prefer = "return=representation",
            body = buildJsonObject {
                put("p_payment_id", input.paymentId)
                put("p_amount", input.amount)
                input.reason?.takeIf { it.isNotBlank() }?.let { put("p_reason", it) }
                put("p_idempotency_key", input.idempotencyKey)
                put("p_collected_by", sessionManager.current()?.userId)
            }
        )
        return result.parseFirst { element ->
            SupabaseConfig.json
                .decodeFromJsonElement(PaymentDto.serializer(), element)
                .toDomain()
        }
    }

    override suspend fun getPaymentHistory(memberId: String): NetworkResult<List<Payment>> {
        val result = client.get(
            "/payments",
            mapOf(
                "member_id" to "eq.$memberId",
                "select" to "*",
                "order" to "created_at.desc"
            )
        )
        return result.parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(PaymentDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
        }
    }

    override suspend fun getRecentPayments(limit: Int): NetworkResult<List<Payment>> {
        val result = client.get(
            "/payments",
            mapOf(
                "select" to "*",
                "status" to "eq.SUCCESS",
                "order" to "created_at.desc",
                "limit" to limit.toString()
            )
        )
        return result.parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(PaymentDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
        }
    }

    override suspend fun getOutstandingDues(): NetworkResult<List<MemberSummary>> {
        return client.get("/member_overview", mapOf("select" to "*")).parseJson { element ->
            element.asArrayOrEmpty().mapNotNull { item ->
                runCatching {
                    SupabaseConfig.json
                        .decodeFromJsonElement(MemberOverviewDto.serializer(), item)
                        .toDomain()
                }.getOrNull()
            }
                .filter { it.dueAmount > 0.0 }
                .sortedByDescending { it.dueAmount }
        }
    }
}