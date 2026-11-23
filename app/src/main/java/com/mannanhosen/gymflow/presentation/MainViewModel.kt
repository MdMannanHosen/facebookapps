package com.mannanhosen.gymflow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.session.UserSession
import com.mannanhosen.gymflow.domain.model.UserRole
import com.mannanhosen.gymflow.domain.repository.AuthRepository
import com.mannanhosen.gymflow.presentation.navigation.Routes
import com.mannanhosen.gymflow.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level state: current session, resolved start destination and theme mode.
 * Shared by MainActivity and Settings.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.session().collect { session ->
                _session.value = session
                if (_startDestination.value == null) {
                    _startDestination.value =
                        if (session == null) Routes.LOGIN else Routes.DASHBOARD
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun currentRole(): UserRole = UserRole.from(_session.value?.role)
}