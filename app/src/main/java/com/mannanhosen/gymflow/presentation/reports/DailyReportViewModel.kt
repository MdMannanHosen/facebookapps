package com.mannanhosen.gymflow.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.DailyReport
import com.mannanhosen.gymflow.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReportUiState(
    val date: LocalDate = AppClock.todayInDhaka(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val report: DailyReport? = null
)

@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init { load(_uiState.value.date) }

    fun retry() = load(_uiState.value.date)

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
        load(date)
    }

    private fun load(date: LocalDate) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = dashboardRepository.getDailyReport(date)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, report = result.data, error = null)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}