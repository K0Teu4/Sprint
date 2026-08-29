package ru.sprint.app.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.sprint.app.SprintApplication
import ru.sprint.app.domain.model.Category
import ru.sprint.app.util.TaskPlanner
import java.time.LocalDate
import java.time.YearMonth

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as SprintApplication).repository
    private val currentMonth = MutableStateFlow(YearMonth.now())

    val state: StateFlow<StatsUiState> = combine(
        currentMonth,
        repo.allTasks(),
        repo.completions()
    ) { ym, allTasks, completions ->
        var total = 0
        var done = 0
        val dailyTotal = mutableMapOf<LocalDate, Int>()
        val dailyDone = mutableMapOf<LocalDate, Int>()
        val catCounter = mutableMapOf<Category, Int>()
        val weekday = MutableList(7) { 0 }

        (1..ym.lengthOfMonth()).forEach { day ->
            val d = ym.atDay(day)
            val resolved = TaskPlanner.tasksForDay(allTasks, completions, d)
            dailyTotal[d] = resolved.size
            dailyDone[d] = resolved.count { it.isDone }
            total += resolved.size
            done += resolved.count { it.isDone }
            weekday[d.dayOfWeek.value - 1] += resolved.size
            resolved.forEach { r ->
                val cat = Category.fromKey(r.entity.category)
                catCounter[cat] = (catCounter[cat] ?: 0) + 1
            }
        }

        val gridStart = ym.atDay(1).let {
            it.minusDays((it.dayOfWeek.value - 1).toLong())
        }
        val cells = (0 until 42).map { offset ->
            val d = gridStart.plusDays(offset.toLong())
            HeatCell(
                date = d,
                count = dailyTotal[d] ?: 0,
                isCurrentMonth = d.month == ym.month && d.year == ym.year
            )
        }

        val catTotal = catCounter.values.sum()
        val slices = catCounter.map { (cat, count) ->
            CategorySlice(
                category = cat,
                count = count,
                percent = if (catTotal == 0) 0 else count * 100 / catTotal
            )
        }.sortedByDescending { it.count }

        val today = LocalDate.now()
        var anchor = if (ym == YearMonth.from(today)) today else ym.atEndOfMonth()
        var streak = 0
        while (anchor.month == ym.month && (dailyDone[anchor] ?: 0) > 0) {
            streak++
            anchor = anchor.minusDays(1)
        }

        val daysBase = if (ym == YearMonth.from(today)) today.dayOfMonth
        else ym.lengthOfMonth()

        StatsUiState(
            monthYear = ym,
            dailyCells = cells,
            categoryBreakdown = slices,
            weekdayCounts = weekday,
            totalTasks = total,
            doneTasks = done,
            bestDay = dailyTotal.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key,
            streak = streak,
            avgPerDay = if (daysBase == 0) 0 else total / daysBase,
            activeDays = dailyDone.count { it.value > 0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun nextMonth() { currentMonth.value = currentMonth.value.plusMonths(1) }
    fun prevMonth() { currentMonth.value = currentMonth.value.minusMonths(1) }
}
