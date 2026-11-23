package com.mannanhosen.gymflow.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.DashboardSummary
import com.mannanhosen.gymflow.domain.model.UserRole
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import com.mannanhosen.gymflow.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val role: UserRole = UserRole.OWNER,
    val summary: DashboardSummary? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        if (_uiState.value.isLoading && _uiState.value.summary != null) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val role = authRepository.session().first()?.role?.let(UserRole::from) ?: UserRole.OWNER
            _uiState.update { it.copy(role = role) }

            when (val result = dashboardRepository.getDashboard(role)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, summary = result.data, error = null)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun retry() = load()
}