package com.mannanhosen.gymflow.presentation.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.MemberSummary
import com.mannanhosen.gymflow.domain.repository.MemberFilter
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembersUiState(
    val members: List<MemberSummary> = emptyList(),
    val query: String = "",
    val filter: MemberFilter = MemberFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init { load() }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce to avoid excessive network requests
            load(query, _uiState.value.filter)
        }
    }

    fun onFilterChange(filter: MemberFilter) {
        _uiState.update { it.copy(filter = filter) }
        load(_uiState.value.query, filter)
    }

    fun retry() = load(_uiState.value.query, _uiState.value.filter)

    /** Pull-to-refresh: reloads while keeping the current list on screen. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val result =
                memberRepository.getMembers(_uiState.value.query.ifBlank { null }, _uiState.value.filter)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        members = result.data,
                        error = null,
                        isRefreshing = false
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error, isRefreshing = false)
                }
            }
        }
    }

    private fun load(query: String, filter: MemberFilter) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = memberRepository.getMembers(query.ifBlank { null }, filter)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, members = result.data, error = null)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}