package com.mannanhosen.gymflow.presentation.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.core.util.Validators
import com.mannanhosen.gymflow.domain.repository.MemberRepository
import com.mannanhosen.gymflow.domain.repository.NewMemberInput
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

data class MemberEditUiState(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val joinedAt: LocalDate = AppClock.todayInDhaka(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val nameError: String? = null,
    val phoneError: String? = null
) {
    val canSave: Boolean get() = !isSaving && fullName.isNotBlank() && phone.isNotBlank()
}

@HiltViewModel
class MemberEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val memberId: String? = savedStateHandle["memberId"]

    private val _uiState = MutableStateFlow(MemberEditUiState(isEditing = memberId != null))
    val uiState: StateFlow<MemberEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    init { memberId?.let(::loadMember) }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, nameError = null) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, phoneError = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onJoinedAtChange(date: LocalDate) = _uiState.update { it.copy(joinedAt = date) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val nameError = if (state.fullName.isBlank()) "Name is required" else null
        val phoneError = when {
            state.phone.isBlank() -> "Phone is required"
            !Validators.isValidPhone(state.phone) -> "Enter a valid phone number"
            else -> null
        }
        if (nameError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val input = NewMemberInput(
                fullName = state.fullName.trim(),
                phone = Validators.normalizePhone(state.phone),
                email = state.email.trim().ifBlank { null },
                joinedAt = state.joinedAt
            )
            val result = if (memberId != null) {
                memberRepository.updateMember(memberId, input)
            } else {
                memberRepository.createMember(input)
            }
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is NetworkResult.Success -> _effects.emit(UiEffect.NavigateBack)
                is NetworkResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun delete() {
        val id = memberId ?: return
        viewModelScope.launch {
            when (val result = memberRepository.deleteMember(id)) {
                is NetworkResult.Success -> _effects.emit(UiEffect.NavigateBack)
                is NetworkResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    private fun loadMember(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = memberRepository.getMember(id)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        fullName = result.data.fullName,
                        phone = result.data.phone,
                        email = result.data.email.orEmpty(),
                        joinedAt = result.data.joinedAt
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}