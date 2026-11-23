package com.mannanhosen.gymflow.presentation.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.MembershipPlan
import com.mannanhosen.gymflow.domain.repository.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val plans: List<MembershipPlan> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = membershipRepository.getPlans(activeOnly = false)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, plans = result.data, error = null)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}