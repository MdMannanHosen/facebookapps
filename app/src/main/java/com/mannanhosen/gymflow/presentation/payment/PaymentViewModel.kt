package com.mannanhosen.gymflow.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.PaymentMethod
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import com.mannanhosen.gymflow.domain.usecase.CalculateDue
import com.mannanhosen.gymflow.domain.usecase.PaymentError
import com.mannanhosen.gymflow.domain.usecase.PaymentValidation
import com.mannanhosen.gymflow.domain.usecase.RecordPayment
import com.mannanhosen.gymflow.presentation.util.UiEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val memberId: String = "",
    val memberName: String = "",
    val membershipId: String? = null,
    val dueAmount: Double = 0.0,
    val amountInput: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val reference: String = "",
    val note: String = "",
    val amountError: String? = null
) {
    val canSubmit: Boolean
        get() = !isSubmitting && amountInput.isNotBlank() && !isLoading && memberName.isNotBlank()
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val membershipRepository: MembershipRepository,
    private val paymentRepository: PaymentRepository,
    private val calculateDue: CalculateDue,
    private val recordPayment: RecordPayment
) : ViewModel() {

    private val memberId: String = savedStateHandle["memberId"] ?: ""
    private val idempotencyKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(PaymentUiState(memberId = memberId))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    init { load() }

    fun onAmountChange(value: String) = _uiState.update {
        it.copy(amountInput = value, amountError = null)
    }
    fun onMethodChange(method: PaymentMethod) = _uiState.update { it.copy(method = method) }
    fun onReferenceChange(value: String) = _uiState.update { it.copy(reference = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun useFullDue() = _uiState.update { it.copy(amountInput = it.dueAmount.toString()) }

    private fun load() {
        if (memberId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = AppError.NotFound("Member not found")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
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
                    memberName = member.fullName,
                    membershipId = current?.id,
                    dueAmount = due
                )
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        when (val validation = recordPayment(
            memberId = state.memberId,
            membershipId = state.membershipId,
            amountInput = state.amountInput,
            method = state.method,
            reference = state.reference.ifBlank { null },
            note = state.note.ifBlank { null },
            dueAmount = state.dueAmount,
            idempotencyKey = idempotencyKey
        )) {
            is PaymentValidation.Invalid -> {
                val message = when (validation.error) {
                    PaymentError.AMOUNT_REQUIRED -> "Enter an amount"
                    PaymentError.AMOUNT_NOT_POSITIVE -> "Amount must be greater than zero"
                    PaymentError.EXCEEDS_DUE -> "Amount exceeds the outstanding due"
                }
                _uiState.update { it.copy(amountError = message) }
            }
            is PaymentValidation.Valid -> {
                _uiState.update { it.copy(isSubmitting = true, error = null) }
                viewModelScope.launch {
                    when (val result = paymentRepository.recordPayment(validation.input)) {
                        is NetworkResult.Success -> _effects.emit(UiEffect.NavigateBack)
                        is NetworkResult.Failure -> _uiState.update {
                            it.copy(isSubmitting = false, error = result.error)
                        }
                    }
                }
            }
        }
    }
}