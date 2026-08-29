package ru.sprint.app.ui.screens.month

import java.time.LocalDate

data class MonthUiState(
    val yearMonth: java.time.YearMonth = java.time.YearMonth.now(),
    val days: List<DayCell> = emptyList(),
    val monthlyTasks: List<TaskUi> = emptyList(),
    val totalTasks: Int = 0,
    val doneTasks: Int = 0
) {
    val progressPercent: Int
        get() = if (totalTasks == 0) 0 else (doneTasks * 100 / totalTasks)
}

data class DayCell(
    val date: LocalDate,
    val taskCount: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

data class TaskUi(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val dueTime: Int?,
    val isDone: Boolean,
    val isRecurring: Boolean = false,
    val priority: Int,
    val category: String
)
