package com.mannanhosen.gymflow.presentation.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.domain.model.Member
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import com.mannanhosen.gymflow.domain.usecase.CalculateDue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class MemberSelfUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val member: Member? = null,
    val memberships: List<Membership> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val currentMembership: Membership? = null,
    val dueAmount: Double = 0.0
) {
    val latestPayment: Payment?
        get() = payments.maxByOrNull { it.paidAt ?: Instant.MIN }
}

@HiltViewModel
class MemberSelfViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val membershipRepository: MembershipRepository,
    private val paymentRepository: PaymentRepository,
    private val calculateDue: CalculateDue
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberSelfUiState())
    val uiState: StateFlow<MemberSelfUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val memberId = authRepository.session().first()?.userId
            if (memberId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(isLoading = false, error = AppError.Unauthorized("Not signed in"))
                }
                return@launch
            }

            val member = memberRepository.getMember(memberId).getOrNull()
            if (member == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = AppError.NotFound("Member not found"))
                }
                return@launch
            }

            val memberships = membershipRepository.getMembershipsForMember(memberId).getOrNull().orEmpty()
            val payments = paymentRepository.getPaymentHistory(memberId).getOrNull().orEmpty()

            val current = memberships.maxByOrNull { it.endDate }
            val due = current?.let {
                val paid = payments
                    .filter { p -> p.isSuccessful && p.membershipId == it.id }
                    .sumOf { p -> p.amount }
                calculateDue(it.fee, paid, 0.0).toDouble()
            } ?: 0.0

            _uiState.update {
                it.copy(
                    isLoading = false,
                    member = member,
                    memberships = memberships,
                    payments = payments,
                    currentMembership = current,
                    dueAmount = due
                )
            }
        }
    }
}