package com.bluebird.messaging.core.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Inbox-style timestamps: time today, "Yesterday", the weekday within the last week, otherwise a short date.
 */
fun formatConversationTimestamp(
    instant: Instant,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val date = instant.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    return when {
        daysAgo <= 0L -> formatTime(instant, zone, locale)
        daysAgo == 1L -> "Yesterday"
        daysAgo < 7L -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        else -> formatters(locale).shortDate.format(date)
    }
}

/**
 * Timeline separators: "Today 9:41 AM", "Yesterday 9:41 AM", "Tuesday 9:41 AM", then "Sat, Oct 12 at 9:41 AM",
 * with the year added for older messages.
 */
fun formatSeparator(
    instant: Instant,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val at = instant.atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(at.toLocalDate(), today)
    val time = formatTime(instant, zone, locale)
    return when {
        daysAgo <= 0L -> "Today $time"
        daysAgo == 1L -> "Yesterday $time"
        daysAgo < 7L -> "${at.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} $time"
        at.year == today.year -> "${formatters(locale).dayMonth.format(at)} at $time"
        else -> "${formatters(locale).monthDayYear.format(at)} at $time"
    }
}

fun formatTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
    formatters(locale).time.format(instant.atZone(zone))

/** Formatters are costly to build and safe to share, so each locale's set is built once rather than per row. */
private class Formatters(val locale: Locale) {
    val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
    val monthDayYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
}

@Volatile
private var cachedFormatters: Formatters? = null

private fun formatters(locale: Locale): Formatters =
    cachedFormatters?.takeIf { it.locale == locale } ?: Formatters(locale).also { cachedFormatters = it }
