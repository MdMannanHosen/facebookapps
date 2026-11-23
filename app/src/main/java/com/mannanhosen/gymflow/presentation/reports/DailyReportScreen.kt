package com.mannanhosen.gymflow.presentation.reports

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.IosShare
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.domain.model.DailyReport
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
import java.io.File

@Composable
fun DailyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        text = "Export CSV",
                        onClick = { exportReport(context, report) },
                        icon = Icons.Filled.IosShare
                    )
                    InfoBanner(
                        message = "Opens the report as a CSV file — ready for Excel or Google Sheets.",
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

/**
 * Writes the report to a cache file and opens the system share sheet.
 *
 * The file is handed to other apps through a content:// URI granted by
 * FileProvider, so no storage permission is required.
 */
private fun exportReport(context: Context, report: DailyReport) {
    runCatching {
        val csv = ReportExporter.toCsv(report)
        val file = File(context.cacheDir, ReportExporter.fileName(report))
        file.writeText(csv)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "GymFlow report ${report.date}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }.onFailure {
        Toast.makeText(context, "Could not export the report", Toast.LENGTH_SHORT).show()
    }
}