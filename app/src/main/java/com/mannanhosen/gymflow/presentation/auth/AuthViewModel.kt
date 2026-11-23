package com.mannanhosen.gymflow.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.R
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.core.util.Validators
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import com.mannanhosen.gymflow.presentation.util.UiEffect
import com.mannanhosen.gymflow.presentation.util.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val showPassword: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotBlank() &&
            (!isSignUp || fullName.isNotBlank())
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }
    fun toggleSignUp() = _uiState.update { it.copy(isSignUp = !it.isSignUp, error = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = if (state.isSignUp) {
                authRepository.signUp(state.fullName.trim(), state.email.trim(), state.password)
            } else {
                authRepository.signIn(state.email.trim(), state.password)
            }

            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is NetworkResult.Success -> _effects.emit(UiEffect.Navigate("dashboard"))
                is NetworkResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    private fun validate(state: AuthUiState): AppError? {
        if (state.email.isBlank()) return AppError.Validation("Email required")
        if (!Validators.isValidEmail(state.email)) {
            return AppError.Validation("Invalid email")
        }
        if (state.password.isBlank()) return AppError.Validation("Password required")
        if (state.password.length < 6) return AppError.Validation("Password too short")
        if (state.isSignUp && state.fullName.isBlank()) {
            return AppError.Validation("Name required")
        }
        return null
    }

    fun errorMessageRes(error: AppError): Int = error.messageRes()

    companion object {
        val emailRequired = R.string.auth_error_email_required
        val passwordRequired = R.string.auth_error_password_required
    }
}