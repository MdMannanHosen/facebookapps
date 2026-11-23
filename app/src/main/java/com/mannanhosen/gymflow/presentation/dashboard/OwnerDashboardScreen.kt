package com.mannanhosen.gymflow.presentation.dashboard

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.MemberListItem
import com.mannanhosen.gymflow.presentation.components.PaymentListItem
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SecondaryButton
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.presentation.components.SkeletonLoader
import com.mannanhosen.gymflow.presentation.components.StatCard
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun OwnerDashboardScreen(
    onAddMember: () -> Unit,
    onReceivePayment: () -> Unit,
    onViewMembers: () -> Unit,
    onViewDues: () -> Unit,
    onViewExpiring: () -> Unit,
    onViewReports: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Dashboard",
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.summary == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SkeletonLoader(height = 120)
                SkeletonLoader(height = 90)
                SkeletonLoader(height = 90)
            }

            state.error != null && state.summary == null -> ErrorState(
                message = state.error?.message ?: "Failed to load",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            else -> {
                state.summary?.let { summary ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    item {
                        Column {
                            Text(
                                "Overview",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Here is what's happening at your gym today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            StatCard(
                                label = "Active members",
                                value = summary.activeMembers.toString(),
                                icon = Icons.Filled.Groups,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Expiring soon",
                                value = summary.expiringSoon.toString(),
                                icon = Icons.Filled.Schedule,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            StatCard(
                                label = "Outstanding dues",
                                value = Money.withSymbol(summary.outstandingDues),
                                icon = Icons.Filled.Warning,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Today's collection",
                                value = Money.withSymbol(summary.todaysCollection),
                                icon = Icons.Filled.Payments,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Quick actions")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            QuickAction(
                                label = "Add member",
                                icon = Icons.Filled.Add,
                                onClick = onAddMember,
                                modifier = Modifier.weight(1f)
                            )
                            QuickAction(
                                label = "Receive payment",
                                icon = Icons.Filled.AttachMoney,
                                onClick = onReceivePayment,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            QuickAction(
                                label = "View dues",
                                icon = Icons.Filled.Warning,
                                onClick = onViewDues,
                                modifier = Modifier.weight(1f)
                            )
                            QuickAction(
                                label = "Reports",
                                icon = Icons.Filled.AttachMoney,
                                onClick = onViewReports,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Expiring soon",
                            actionLabel = "View all",
                            onAction = onViewExpiring
                        )
                        if (summary.expiringMembers.isEmpty()) {
                            GymCard {
                                Text(
                                    "No memberships expiring in the next 7 days.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                summary.expiringMembers.take(3).forEach { member ->
                                    MemberListItem(
                                        summary = member,
                                        onClick = { onOpenMember(member.member.id) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Recent payments",
                            actionLabel = "View all",
                            onAction = onViewMembers
                        )
                        if (summary.recentPayments.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Payments,
                                title = "No payments yet",
                                message = "Payment records will appear here.",
                                modifier = Modifier.height(220.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                summary.recentPayments.take(5).forEach { payment ->
                                    PaymentListItem(payment = payment)
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GymCard(modifier = modifier, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.sm))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}