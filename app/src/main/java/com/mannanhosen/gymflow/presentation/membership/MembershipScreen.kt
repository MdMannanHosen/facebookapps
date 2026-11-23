package com.mannanhosen.gymflow.presentation.membership

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.InfoBanner
import com.mannanhosen.gymflow.presentation.components.BannerType
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.MemberListItem
import com.mannanhosen.gymflow.presentation.components.PlanCard
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SearchBar
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.presentation.components.SkeletonLoader
import com.mannanhosen.gymflow.presentation.members.MembersViewModel
import com.mannanhosen.gymflow.presentation.util.UiEffect
import com.mannanhosen.gymflow.ui.theme.Spacing

/**
 * Entry point for creating or renewing a membership. When no member is supplied,
 * a member picker is shown first.
 */
@Composable
fun MembershipScreen(
    memberId: String?,
    isRenewal: Boolean,
    onBack: () -> Unit,
    onPickedMember: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = memberId.isNullOrBlank(),
        animationSpec = tween(durationMillis = 200),
        label = "membershipStep",
        modifier = modifier
    ) { picking ->
        if (picking) {
            MemberPicker(
                onBack = onBack,
                onPick = onPickedMember
            )
        } else {
            MembershipForm(
                onBack = onBack,
                onDone = onDone
            )
        }
    }
}

@Composable
private fun MemberPicker(
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MembersViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Select member", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
        ) {
            SearchBar(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search members",
                modifier = Modifier.padding(vertical = Spacing.md)
            )
            when {
                state.isLoading -> LoadingState()
                state.members.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "No members found",
                    message = "Add a member first, then try again."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(state.members, key = { it.member.id }) { summary ->
                        MemberListItem(
                            summary = summary,
                            onClick = { onPick(summary.member.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembershipForm(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MembershipViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val range = remember(state.selectedPlanId, state.startDate, state.startAfterCurrent) {
        viewModel.previewRange()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.NavigateBack -> onBack()
                is UiEffect.Navigate -> onDone()
                is UiEffect.ShowMessage -> Unit
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = if (state.isRenewal) "Renew membership" else "Create membership",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null && state.plans.isEmpty() -> ErrorState(
                message = state.error?.message ?: "Failed to load",
                onRetry = onBack,
                modifier = Modifier.padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                GymCard {
                    Text(
                        "Member",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.memberName.ifBlank { "Selected member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    state.currentEndDate?.let {
                        Text(
                            "Current membership ends ${AppClock.formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isRenewal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Renew early",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Start the day after the current membership ends so no days are lost.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.startAfterCurrent,
                            onCheckedChange = viewModel::onToggleStartAfterCurrent
                        )
                    }
                }

                SectionHeader(title = "Select plan")
                state.plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan.id == state.selectedPlanId,
                        onClick = { viewModel.onSelectPlan(plan.id) }
                    )
                }

                SectionHeader(title = "Start date")
                com.mannanhosen.gymflow.presentation.components.DatePickerField(
                    label = "Start date",
                    selectedDate = state.startDate,
                    onDateSelected = viewModel::onStartDateChange
                )

                range?.let {
                    InfoBanner(
                        message = "New term: ${AppClock.formatDate(it.startDate)} → ${AppClock.formatDate(it.endDate)}",
                        type = BannerType.SUCCESS
                    )
                }

                state.error?.let {
                    InfoBanner(message = it.message, type = BannerType.ERROR)
                }

                PrimaryButton(
                    text = if (state.isRenewal) "Renew membership" else "Create membership",
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    loading = state.isSubmitting
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}