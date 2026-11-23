package com.mannanhosen.gymflow.presentation.member

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.DueAmountBadge
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.InfoBanner
import com.mannanhosen.gymflow.presentation.components.BannerType
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.MembershipItem
import com.mannanhosen.gymflow.presentation.components.MembershipStatusChip
import com.mannanhosen.gymflow.presentation.components.PaymentListItem
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SecondaryButton
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun MemberDashboardScreen(
    onRenew: () -> Unit,
    onPayOnline: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberSelfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "My gym") }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null -> ErrorState(
                message = state.error?.message ?: "Failed to load",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item {
                    Column {
                        Text(
                            "Hello, ${state.member?.fullName?.split(" ")?.firstOrNull().orEmpty()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Here is your membership at a glance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    val membership = state.currentMembership
                    GymCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Membership",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (membership != null) MembershipStatusChip(membership.status)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        if (membership != null) {
                            Text(
                                membership.planName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "Valid until ${AppClock.formatDate(membership.endDate)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Fee: ${Money.withSymbol(membership.fee)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (state.dueAmount > 0) DueAmountBadge(state.dueAmount)
                            }
                        } else {
                            Text(
                                "No active membership",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "Renew or join a plan to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (state.dueAmount > 0.0) {
                    item {
                        InfoBanner(
                            message = "You have an outstanding due of ${Money.withSymbol(state.dueAmount)}.",
                            type = BannerType.WARNING
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        SecondaryButton(
                            text = "Renew",
                            icon = Icons.Filled.Refresh,
                            onClick = onRenew,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Pay online",
                            icon = Icons.Filled.CreditCard,
                            onClick = onPayOnline,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SecondaryButton(
                        text = "Payment history",
                        icon = Icons.Filled.History,
                        onClick = onViewHistory
                    )
                }

                item {
                    SectionHeader(title = "Latest payment")
                    val latest = state.latestPayment
                    if (latest == null) {
                        GymCard {
                            Text(
                                "No payments yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        PaymentListItem(latest)
                    }
                }
            }
        }
    }
}

@Composable
fun MyMembershipScreen(
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberSelfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "My membership") }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null -> ErrorState(
                message = state.error?.message ?: "Failed to load",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )
            state.memberships.isEmpty() -> EmptyState(
                icon = Icons.Filled.AttachMoney,
                title = "No membership yet",
                message = "Choose a plan to get started.",
                modifier = Modifier.padding(padding),
                actionLabel = "Renew / Join",
                onAction = onRenew
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(state.memberships, key = { it.id }) { membership ->
                    MembershipItem(membership)
                }
                item {
                    PrimaryButton(text = "Renew membership", onClick = onRenew)
                }
            }
        }
    }
}