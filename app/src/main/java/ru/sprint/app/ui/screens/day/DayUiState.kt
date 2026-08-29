package ru.sprint.app.ui.screens.day

import ru.sprint.app.domain.model.Category
import ru.sprint.app.ui.common.TaskFormState
import java.time.LocalDate

data class DayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDates: List<LocalDate> = emptyList(),
    val tasks: List<TaskItem> = emptyList()
) {
    val doneCount: Int get() = tasks.count { it.isDone }
    val totalCount: Int get() = tasks.size
    val headerText: String
        get() {
            val today = LocalDate.now()
            return when (selectedDate) {
                today -> "Сегодня"
                today.minusDays(1) -> "Вчера"
                today.plusDays(1) -> "Завтра"
                else -> selectedDate.dayOfMonth.toString() + " " +
                    selectedDate.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        java.util.Locale("ru")
                    )
            }
        }
}

data class TaskItem(
    val id: Long,
    val title: String,
    val details: String = "",
    val date: LocalDate,
    val dueTimeMinutes: Int?,
    val isDone: Boolean,
    val isRecurring: Boolean,
    val priority: Int,
    val category: Category
) {
    val timeString: String
        get() = if (dueTimeMinutes == null) ""
        else {
            val h = (dueTimeMinutes / 60).toString().padStart(2, '0')
            val m = (dueTimeMinutes % 60).toString().padStart(2, '0')
            "$h:$m"
        }

    val metaLine: String
        get() = listOfNotNull(
            if (isRecurring) "Каждый день" else null,
            if (timeString.isNotEmpty()) timeString else null
        ).joinToString(" · ")

    fun toForm(): TaskFormState = TaskFormState(
        title = title,
        details = details,
        date = date,
        dueTimeMinutes = dueTimeMinutes,
        isRecurring = isRecurring,
        category = category,
        priority = priority
    )
}
