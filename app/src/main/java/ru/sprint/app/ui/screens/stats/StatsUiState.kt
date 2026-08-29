package ru.sprint.app.ui.screens.stats

import ru.sprint.app.domain.model.Category
import java.time.LocalDate

data class StatsUiState(
    val monthYear: java.time.YearMonth = java.time.YearMonth.now(),
    val dailyCells: List<HeatCell> = emptyList(),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val weekdayCounts: List<Int> = List(7) { 0 },
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
    val bestDay: LocalDate? = null,
    val streak: Int = 0,
    val avgPerDay: Int = 0,
    val activeDays: Int = 0
)

data class HeatCell(
    val date: LocalDate,
    val count: Int,
    val isCurrentMonth: Boolean
)

data class CategorySlice(
    val category: Category,
    val count: Int,
    val percent: Int
)
