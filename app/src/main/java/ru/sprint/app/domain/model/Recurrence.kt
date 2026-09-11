package ru.sprint.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class Recurrence(
    val type: RecurrenceType,
    val interval: Int = 1,
    val endAfter: Int? = null,
    val excludedDays: Set<DayOfWeek> = emptySet()
)

enum class RecurrenceType {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

fun Recurrence.nextOccurrence(from: LocalDate): LocalDate? {
    return when (type) {
        RecurrenceType.DAILY -> from.plusDays(interval.toLong())
        RecurrenceType.WEEKLY -> {
            var next = from.plusWeeks(interval.toLong())
            while (excludedDays.contains(next.dayOfWeek)) {
                next = next.plusDays(1)
            }
            next
        }
        RecurrenceType.MONTHLY -> from.plusMonths(interval.toLong())
        RecurrenceType.YEARLY -> from.plusYears(interval.toLong())
    }
}
