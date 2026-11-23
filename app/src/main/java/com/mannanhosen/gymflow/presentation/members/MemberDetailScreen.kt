package com.mannanhosen.gymflow.presentation.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.R
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.ConfirmationDialog
import com.mannanhosen.gymflow.presentation.components.DestructiveButton
import com.mannanhosen.gymflow.presentation.components.DueAmountBadge
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymBottomSheet
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.MembershipItem
import com.mannanhosen.gymflow.presentation.components.PaymentListItem
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SecondaryButton
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun MemberDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReceivePayment: () -> Unit,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Local UI state for the actions sheet and its confirm dialog.
    var showActions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionError) {
        val message = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearActionError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.member_detail_title),
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showActions = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.cd_open_menu)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null || state.member == null -> ErrorState(
                message = state.error?.message ?: "Member not found",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            else -> {
                val member = state.member!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    item {
                        GymCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    androidx.compose.foundation.layout.Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            member.fullName.take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.size(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        member.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    member.email?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                state.currentMembership?.let {
                                    com.mannanhosen.gymflow.presentation.components.MembershipStatusChip(it.status)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            GymCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Outstanding",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    Money.withSymbol(state.dueAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            GymCard(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Joined",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    AppClock.formatDate(member.joinedAt),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            SecondaryButton(
                                text = "Edit",
                                icon = Icons.Filled.Edit,
                                onClick = onEdit,
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Renew",
                                icon = Icons.Filled.Refresh,
                                onClick = onRenew,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (state.dueAmount > 0.0) {
                        item {
                            PrimaryButton(
                                text = "Receive payment",
                                icon = Icons.Filled.AttachMoney,
                                onClick = onReceivePayment
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Membership")
                        if (state.memberships.isEmpty()) {
                            GymCard {
                                Text(
                                    "No membership yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                state.memberships.forEach { membership ->
                                    MembershipItem(membership)
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Payment history")
                        if (state.payments.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.AttachMoney,
                                title = "No payments yet",
                                message = "Payment records will appear here.",
                                modifier = Modifier.height(200.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                state.payments.forEach { payment ->
                                    PaymentListItem(payment)
                                }
                            }
                        }
                    }
                }
            }

            GymBottomSheet(
                visible = showActions,
                onDismiss = { showActions = false },
                title = stringResource(R.string.member_actions)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.action_edit),
                    icon = Icons.Filled.Edit,
                    onClick = {
                        showActions = false
                        onEdit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
                SecondaryButton(
                    text = stringResource(R.string.renew_membership),
                    icon = Icons.Filled.Refresh,
                    onClick = {
                        showActions = false
                        onRenew()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
                if (state.dueAmount > 0.0) {
                    SecondaryButton(
                        text = stringResource(R.string.receive_payment),
                        icon = Icons.Filled.AttachMoney,
                        onClick = {
                            showActions = false
                            onReceivePayment()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                    )
                }
                DestructiveButton(
                    text = stringResource(
                        if (state.isDeleting) R.string.member_deleting
                        else R.string.member_delete_forever
                    ),
                    icon = Icons.Filled.Delete,
                    enabled = !state.isDeleting,
                    onClick = {
                        showActions = false
                        showDeleteConfirm = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
                Spacer(Modifier.height(Spacing.md))
            }

            if (showDeleteConfirm) {
                ConfirmationDialog(
                    title = stringResource(R.string.member_delete_title),
                    message = stringResource(R.string.member_delete_message),
                    confirmLabel = stringResource(R.string.action_delete),
                    dismissLabel = stringResource(R.string.action_cancel),
                    destructive = true,
                    onConfirm = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                    onDismiss = { showDeleteConfirm = false }
                )
            }
        }
    }
}