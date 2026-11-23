package com.mannanhosen.gymflow.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.DestructiveButton
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymBottomSheet
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.ui.theme.Spacing

private data class PermissionRow(
    val field: PermissionField,
    val label: String,
    val description: String
)

private val permissionRows = listOf(
    PermissionRow(PermissionField.MANAGE_MEMBERS, "Manage members", "Add, edit and delete members"),
    PermissionRow(PermissionField.RECEIVE_PAYMENTS, "Receive payments", "Record cash and online payments"),
    PermissionRow(PermissionField.RENEW_MEMBERSHIPS, "Renew memberships", "Create and renew memberships"),
    PermissionRow(PermissionField.VIEW_REPORTS, "View reports", "Open the daily business report"),
    PermissionRow(PermissionField.MANAGE_PLANS, "Manage plans", "Create and edit membership plans"),
    PermissionRow(PermissionField.MANAGE_STAFF, "Manage staff", "Grant permissions to other staff")
)

private fun StaffPermissionsUiState.isEnabled(field: PermissionField): Boolean {
    val p = permissions ?: return false
    return when (field) {
        PermissionField.MANAGE_MEMBERS -> p.canManageMembers
        PermissionField.RECEIVE_PAYMENTS -> p.canReceivePayments
        PermissionField.RENEW_MEMBERSHIPS -> p.canRenewMemberships
        PermissionField.VIEW_REPORTS -> p.canViewReports
        PermissionField.MANAGE_PLANS -> p.canManagePlans
        PermissionField.MANAGE_STAFF -> p.canManageStaff
    }
}

private fun StaffPermissionsUiState.grantedCount(): Int = permissionRows.count { isEnabled(it.field) }

@Composable
fun StaffPermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StaffPermissionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessages()
    }

    LaunchedEffect(state.actionError) {
        val message = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Staff permissions",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null -> ErrorState(
                message = state.error?.message ?: "Failed to load staff",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            state.staff.isEmpty() -> EmptyState(
                icon = Icons.Filled.SupervisedUserCircle,
                title = "No staff yet",
                message = "Staff accounts appear here once their role is set to STAFF.",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(state.staff, key = { it.id }) { staff ->
                    GymCard(
                        modifier = Modifier.animateItem(),
                        onClick = { viewModel.select(staff) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    staff.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (staff.email.isNotBlank()) {
                                    Text(
                                        staff.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    GymBottomSheet(
        visible = state.selected != null,
        onDismiss = viewModel::close,
        title = state.selected?.fullName
    ) {
        if (state.isPermissionsLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.permissions == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    "Could not load permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    state.actionError ?: "Check your connection and try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(Spacing.xs))
                PrimaryButton(
                    text = "Try again",
                    onClick = viewModel::retryPermissions,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(Spacing.sm))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    "${state.grantedCount()} of ${permissionRows.size} permissions granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(Spacing.sm))
                permissionRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                row.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isEnabled(row.field),
                            onCheckedChange = { viewModel.toggle(row.field, it) },
                            enabled = !state.isSaving
                        )
                    }
                }
                Spacer(Modifier.size(Spacing.sm))
                PrimaryButton(
                    text = if (state.isSaving) "Saving…" else "Save permissions",
                    enabled = !state.isSaving,
                    loading = state.isSaving,
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(Spacing.xs))
                DestructiveButton(
                    text = "Revoke all",
                    enabled = !state.isSaving,
                    onClick = {
                        permissionRows.forEach { viewModel.toggle(it.field, false) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(Spacing.md))
            }
        }
    }
}