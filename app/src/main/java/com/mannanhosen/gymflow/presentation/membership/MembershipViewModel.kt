package com.mannanhosen.gymflow.presentation.membership

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Membership
import com.mannanhosen.gymflow.domain.model.MembershipPlan
import com.mannanhosen.gymflow.domain.model.MembershipStatus
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import com.mannanhosen.gymflow.domain.usecase.CalculateRenewalRange
import com.mannanhosen.gymflow.domain.usecase.CreateMembership
import com.mannanhosen.gymflow.domain.usecase.MembershipRange
import com.mannanhosen.gymflow.domain.usecase.RenewMembership
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
import java.time.LocalDate
import javax.inject.Inject

data class MembershipUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val isRenewal: Boolean = false,
    val memberId: String = "",
    val memberName: String = "",
    val currentEndDate: LocalDate? = null,
    val plans: List<MembershipPlan> = emptyList(),
    val selectedPlanId: String? = null,
    val startDate: LocalDate = AppClock.todayInDhaka(),
    val startAfterCurrent: Boolean = true
) {
    val selectedPlan: MembershipPlan?
        get() = plans.firstOrNull { it.id == selectedPlanId }

    val canSubmit: Boolean
        get() = !isSubmitting && memberId.isNotBlank() && selectedPlanId != null
}

@HiltViewModel
class MembershipViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val membershipRepository: MembershipRepository,
    private val createMembership: CreateMembership,
    private val renewMembership: RenewMembership,
    private val calculateRange: CalculateRenewalRange
) : ViewModel() {

    private val memberId: String = savedStateHandle["memberId"] ?: ""
    private val isRenewal: Boolean = savedStateHandle["mode"]?.equals("renew", true) ?: false

    private val _uiState = MutableStateFlow(
        MembershipUiState(isLoading = true, isRenewal = isRenewal, memberId = memberId)
    )
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    init { load() }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val plans = when (val r = membershipRepository.getPlans(activeOnly = true)) {
                is NetworkResult.Success -> r.data
                is NetworkResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = r.error) }
                    return@launch
                }
            }

            var name = ""
            var currentEnd: LocalDate? = null
            if (memberId.isNotBlank()) {
                name = memberRepository.getMember(memberId).getOrNull()?.fullName.orEmpty()
                if (isRenewal) {
                    currentEnd = membershipRepository
                        .getMembershipsForMember(memberId)
                        .getOrNull()
                        ?.maxByOrNull { it.endDate }
                        ?.endDate
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    plans = plans,
                    memberName = name,
                    currentEndDate = currentEnd,
                    selectedPlanId = it.selectedPlanId ?: plans.firstOrNull()?.id,
                    startAfterCurrent = isRenewal
                )
            }
        }
    }

    fun onSelectPlan(planId: String) = _uiState.update { it.copy(selectedPlanId = planId) }
    fun onStartDateChange(date: LocalDate) = _uiState.update { it.copy(startDate = date) }
    fun onToggleStartAfterCurrent(value: Boolean) =
        _uiState.update { it.copy(startAfterCurrent = value) }

    fun previewRange(): MembershipRange? {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return null
        val today = AppClock.todayInDhaka()
        val current = state.currentEndDate?.let { end ->
            Membership(
                id = "",
                memberId = state.memberId,
                planId = "",
                planName = "",
                startDate = today,
                endDate = end,
                fee = 0.0,
                status = MembershipStatus.ACTIVE
            )
        }
        return calculateRange(
            currentMembership = current,
            durationMonths = plan.durationMonths,
            requestedStart = state.startDate,
            today = today,
            startAfterCurrent = state.startAfterCurrent
        )
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val planId = state.selectedPlanId ?: return@launch
            val result = if (isRenewal) {
                renewMembership(
                    memberId = state.memberId,
                    planId = planId,
                    requestedStart = state.startDate,
                    today = AppClock.todayInDhaka()
                )
            } else {
                createMembership(
                    memberId = state.memberId,
                    planId = planId,
                    startDate = state.startDate
                )
            }
            _uiState.update { it.copy(isSubmitting = false) }
            when (result) {
                is NetworkResult.Success -> _effects.emit(UiEffect.NavigateBack)
                is NetworkResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }
}