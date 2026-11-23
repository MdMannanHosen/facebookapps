package com.mannanhosen.gymflow.presentation.reports

import com.mannanhosen.gymflow.core.util.Csv
import com.mannanhosen.gymflow.domain.model.DailyReport

/**
 * Turns a [DailyReport] into a spreadsheet-friendly CSV.
 *
 * CSV is used instead of a binary .xlsx so the file opens directly in Excel,
 * Google Sheets and LibreOffice without extra dependencies.
 */
object ReportExporter {

    private val header = listOf(
        "Date",
        "Collection",
        "Cash",
        "Online",
        "New members",
        "Renewals",
        "Payments",
        "Outstanding dues"
    )

    fun toCsv(report: DailyReport): String = Csv.document(
        listOf(
            header,
            listOf(
                report.date.toString(),
                report.totalCollection,
                report.cashCollection,
                report.onlineCollection,
                report.newMembers,
                report.renewals,
                report.paymentsCount,
                report.outstandingDues
            )
        )
    )

    fun fileName(report: DailyReport): String = "gymflow-report-${report.date}.csv"
}