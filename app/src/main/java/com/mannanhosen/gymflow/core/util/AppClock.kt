package com.mannanhosen.gymflow.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Time handling for GymFlow.
 *
 * Rules:
 * - All timestamps are stored in UTC (Instant / ISO-8601).
 * - Business "today" and schedule calculations use Asia/Dhaka.
 */
object AppClock {

    val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")
    val UTC: ZoneId = ZoneId.of("UTC")

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

    fun nowUtc(): ZonedDateTime = ZonedDateTime.now(UTC)

    fun todayInDhaka(): LocalDate = LocalDate.now(DHAKA)

    /** Convert an ISO-8601 UTC string to a business date in Dhaka. */
    fun businessDate(isoUtc: String): LocalDate =
        Instant.parse(isoUtc).atZone(DHAKA).toLocalDate()

    fun businessDate(instant: Instant): LocalDate = instant.atZone(DHAKA).toLocalDate()

    /** Format a business date for display. */
    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatDateTime(instant: Instant): String =
        instant.atZone(DHAKA).format(dateTimeFormatter)

    fun formatMonthYear(date: LocalDate): String = date.format(monthYearFormatter)

    /** Combine a business date with a wall-clock time in Dhaka into an Instant. */
    fun toInstant(date: LocalDate, time: LocalDateTime): Instant =
        ZonedDateTime.of(date, time.toLocalTime(), DHAKA).toInstant()

    fun startOfDayUtc(date: LocalDate): Instant =
        date.atStartOfDay(DHAKA).toInstant()

    fun endOfDayUtc(date: LocalDate): Instant =
        date.plusDays(1).atStartOfDay(DHAKA).toInstant().minusNanos(1)
}