package com.mannanhosen.gymflow.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.DatePickerField
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.InfoBanner
import com.mannanhosen.gymflow.presentation.components.BannerType
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.presentation.components.SkeletonLoader
import com.mannanhosen.gymflow.presentation.components.StatCard
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun DailyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Daily report", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            DatePickerField(
                label = "Report date",
                selectedDate = state.date,
                onDateSelected = viewModel::onDateChange
            )

            when {
                state.isLoading -> Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    SkeletonLoader(height = 110)
                    SkeletonLoader(height = 90)
                }

                state.error != null -> ErrorState(
                    message = state.error?.message ?: "Failed to load report",
                    onRetry = viewModel::retry
                )

                else -> state.report?.let { report ->
                    GymCard {
                        Text(
                            "Collection",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Money.withSymbol(report.totalCollection),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            ReportLine("Cash", Money.withSymbol(report.cashCollection), Modifier.weight(1f))
                            ReportLine("Online", Money.withSymbol(report.onlineCollection), Modifier.weight(1f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatCard(
                            label = "New members",
                            value = report.newMembers.toString(),
                            icon = Icons.Filled.PersonAdd,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Renewals",
                            value = report.renewals.toString(),
                            icon = Icons.Filled.Refresh,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatCard(
                            label = "Payments",
                            value = report.paymentsCount.toString(),
                            icon = Icons.Filled.AttachMoney,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Outstanding",
                            value = Money.withSymbol(report.outstandingDues),
                            icon = Icons.Filled.Summarize,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PrimaryButton(
                        text = "Export report",
                        onClick = {},
                        icon = Icons.Filled.Summarize
                    )
                    InfoBanner(
                        message = "PDF and Excel export will be available in a future release.",
                        type = BannerType.INFO
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}