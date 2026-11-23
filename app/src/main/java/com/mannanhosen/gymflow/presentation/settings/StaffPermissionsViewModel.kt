package com.mannanhosen.gymflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.StaffMember
import com.mannanhosen.gymflow.domain.model.StaffPermissions
import com.mannanhosen.gymflow.domain.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The individual grants an owner can toggle for a staff member. */
enum class PermissionField {
    MANAGE_MEMBERS,
    RECEIVE_PAYMENTS,
    RENEW_MEMBERSHIPS,
    VIEW_REPORTS,
    MANAGE_PLANS,
    MANAGE_STAFF
}

data class StaffPermissionsUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val staff: List<StaffMember> = emptyList(),
    val selected: StaffMember? = null,
    val permissions: StaffPermissions? = null,
    val isPermissionsLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val actionError: String? = null
)

/**
 * Owner-only screen state for per-staff grants.
 *
 * The server is authoritative; these switches only mirror what RLS enforces.
 * A staff member with no row is treated as having no permissions.
 */
@HiltViewModel
class StaffPermissionsViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffPermissionsUiState())
    val uiState: StateFlow<StaffPermissionsUiState> = _uiState.asStateFlow()

    init { loadStaff() }

    fun retry() = loadStaff()

    fun select(staff: StaffMember) {
        _uiState.update {
            it.copy(
                selected = staff,
                permissions = null,
                isPermissionsLoading = true,
                actionError = null,
                message = null
            )
        }
        loadPermissions(staff)
    }

    /** Re-fetch the grants after a failed load without closing the sheet. */
    fun retryPermissions() {
        val staff = _uiState.value.selected ?: return
        _uiState.update { it.copy(permissions = null, isPermissionsLoading = true, actionError = null) }
        loadPermissions(staff)
    }

    private fun loadPermissions(staff: StaffMember) {
        viewModelScope.launch {
            when (val result = staffRepository.getPermissions(staff.id)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        permissions = result.data,
                        isPermissionsLoading = false,
                        actionError = null
                    )
                }

                is NetworkResult.Failure -> _uiState.update {
                    it.copy(
                        // A missing row is normal: a staff member simply has no
                        // grants yet. Only genuine failures land here.
                        permissions = if (result.error is AppError.NotFound) {
                            StaffPermissions.none(staff.id)
                        } else {
                            // Never leave stale switches on screen after a failed
                            // load: saving then would overwrite real grants.
                            null
                        },
                        isPermissionsLoading = false,
                        actionError = result.error.message
                    )
                }
            }
        }
    }

    fun close() = _uiState.update { it.copy(selected = null, actionError = null) }

    fun clearMessages() = _uiState.update { it.copy(message = null) }

    fun toggle(field: PermissionField, enabled: Boolean) {
        _uiState.update { state ->
            val current = state.permissions ?: return@update state
            state.copy(
                permissions = when (field) {
                    PermissionField.MANAGE_MEMBERS -> current.copy(canManageMembers = enabled)
                    PermissionField.RECEIVE_PAYMENTS -> current.copy(canReceivePayments = enabled)
                    PermissionField.RENEW_MEMBERSHIPS -> current.copy(canRenewMemberships = enabled)
                    PermissionField.VIEW_REPORTS -> current.copy(canViewReports = enabled)
                    PermissionField.MANAGE_PLANS -> current.copy(canManagePlans = enabled)
                    PermissionField.MANAGE_STAFF -> current.copy(canManageStaff = enabled)
                }
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val permissions = state.permissions ?: return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, actionError = null) }
        viewModelScope.launch {
            when (val result = staffRepository.savePermissions(permissions)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isSaving = false, selected = null, message = "Permissions saved")
                }

                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, actionError = result.error.message)
                }
            }
        }
    }

    private fun loadStaff() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = staffRepository.getStaffMembers()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, staff = result.data, error = null)
                }

                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}