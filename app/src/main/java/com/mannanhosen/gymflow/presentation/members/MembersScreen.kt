package com.mannanhosen.gymflow.presentation.members
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.domain.repository.MemberFilter
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.MemberListItem
import com.mannanhosen.gymflow.presentation.components.PullToRefreshContainer
import com.mannanhosen.gymflow.presentation.components.SearchBar
import com.mannanhosen.gymflow.presentation.components.SkeletonLoader
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun MembersScreen(
    onAddMember: () -> Unit,
    onOpenMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Members",
                actions = {
                    IconButton(onClick = onAddMember) {
                        Icon(Icons.Filled.Add, contentDescription = "Add member")
                    }
                }
            )
        }
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
                placeholder = "Search by name or phone",
                modifier = Modifier.padding(vertical = Spacing.md)
            )

            FilterRow(
                selected = state.filter,
                onSelect = viewModel::onFilterChange
            )

            when {
                state.isLoading && state.members.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    SkeletonLoader(height = 72)
                    SkeletonLoader(height = 72)
                    SkeletonLoader(height = 72)
                }

                state.error != null && state.members.isEmpty() -> ErrorState(
                    message = state.error?.message ?: "Failed to load members",
                    onRetry = viewModel::retry
                )

                state.members.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "No members found",
                    message = "Add your first member or adjust your filters."
                )

                else -> PullToRefreshContainer(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(state.members, key = { it.member.id }) { summary ->
                            MemberListItem(
                                summary = summary,
                                onClick = { onOpenMember(summary.member.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: MemberFilter,
    onSelect: (MemberFilter) -> Unit
) {
    val filters = listOf(
        MemberFilter.ALL to "All",
        MemberFilter.ACTIVE to "Active",
        MemberFilter.EXPIRING to "Expiring",
        MemberFilter.EXPIRED to "Expired",
        MemberFilter.WITH_DUES to "With dues"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { androidx.compose.material3.Text(label) },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }
}