package com.mannanhosen.gymflow.presentation.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.PlanCard
import com.mannanhosen.gymflow.presentation.components.SkeletonLoader
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun PlansScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Membership plans", showBack = true, onBack = onBack) }
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SkeletonLoader(height = 100)
                SkeletonLoader(height = 100)
            }

            state.error != null -> ErrorState(
                message = state.error?.message ?: "Failed to load plans",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            state.plans.isEmpty() -> EmptyState(
                icon = Icons.Filled.Sell,
                title = "No plans yet",
                message = "Membership plans will appear here once configured.",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(state.plans, key = { it.id }) { plan ->
                    PlanCard(plan = plan, onClick = {})
                }
            }
        }
    }
}