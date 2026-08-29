package ru.sprint.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val ruLocale = Locale("ru")

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate): Long =
        date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

    fun millisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun minutesToTimeString(minutes: Int?): String {
        if (minutes == null) return ""
        val h = (minutes / 60).toString().padStart(2, '0')
        val m = (minutes % 60).toString().padStart(2, '0')
        return "$h:$m"
    }

    fun timeStringToMinutes(s: String): Int? = try {
        val parts = s.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (_: Exception) {
        null
    }

    fun formatDayHeader(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Сегодня"
            today.minusDays(1) -> "Вчера"
            today.plusDays(1) -> "Завтра"
            else -> date.format(DateTimeFormatter.ofPattern("d LLLL, EEEE", ruLocale))
                .replaceFirstChar { it.uppercase() }
        }
    }

    fun shortWeekdayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, ruLocale)
            .replaceFirstChar { it.uppercase() }

    fun monthYearRu(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale))
            .replaceFirstChar { it.uppercase() }
}
