package com.mannanhosen.gymflow.presentation.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Member
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import com.mannanhosen.gymflow.domain.usecase.CalculateDue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberDetailUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val member: Member? = null,
    val memberships: List<Membership> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val currentMembership: Membership? = null,
    val dueAmount: Double = 0.0,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val actionError: String? = null
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val membershipRepository: MembershipRepository,
    private val paymentRepository: PaymentRepository,
    private val calculateDue: CalculateDue
) : ViewModel() {

    private val memberId: String = savedStateHandle["memberId"] ?: ""

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    /** Clears a transient action failure (e.g. a failed delete) after it was shown. */
    fun clearActionError() = _uiState.update { it.copy(actionError = null) }

    /**
     * Deletes the member. The backend cascades to memberships and payments;
     * the UI navigates back once [MemberDetailUiState.isDeleted] flips true.
     */
    fun delete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, actionError = null) }
        viewModelScope.launch {
            when (val result = memberRepository.deleteMember(memberId)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isDeleting = false, actionError = result.error.message)
                }
            }
        }
    }

    private fun load() {
        if (memberId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = AppError.NotFound("Member not found")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val memberResult = memberRepository.getMember(memberId)
            val member = memberResult.getOrNull()
            if (member == null) {
                val error = (memberResult as? NetworkResult.Failure)?.error
                    ?: AppError.NotFound("Member not found")
                _uiState.update { it.copy(isLoading = false, error = error) }
                return@launch
            }

            val memberships = membershipRepository.getMembershipsForMember(memberId).getOrNull().orEmpty()
            val payments = paymentRepository.getPaymentHistory(memberId).getOrNull().orEmpty()

            val current = memberships.maxByOrNull { it.endDate }
            val due = current?.let { membership ->
                val successful = payments
                    .filter { it.isSuccessful && it.membershipId == membership.id }
                    .sumOf { it.amount }
                calculateDue(membership.fee, successful, 0.0).toDouble()
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