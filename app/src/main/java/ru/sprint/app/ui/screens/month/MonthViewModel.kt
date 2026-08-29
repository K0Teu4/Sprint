package ru.sprint.app.ui.screens.month

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.sprint.app.SprintApplication
import ru.sprint.app.data.db.entity.TaskEntity
import ru.sprint.app.util.TaskPlanner
import java.time.LocalDate
import java.time.YearMonth

class MonthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as SprintApplication).repository
    private val currentMonth = MutableStateFlow(YearMonth.now())

    val state: StateFlow<MonthUiState> = combine(
        currentMonth,
        repo.allTasks(),
        repo.completions()
    ) { ym, tasks, completions ->
        buildState(ym, tasks, completions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState())

    fun nextMonth() { currentMonth.value = currentMonth.value.plusMonths(1) }
    fun prevMonth() { currentMonth.value = currentMonth.value.minusMonths(1) }

    fun toggleMonthly(id: Long, done: Boolean) {
        viewModelScope.launch { repo.setDone(id, done) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repo.deleteTask(id) }
    }

    private fun buildState(
        ym: YearMonth,
        allTasks: List<TaskEntity>,
        completions: List<ru.sprint.app.data.db.entity.CompletionEntity>
    ): MonthUiState {
        val today = LocalDate.now()
        val firstOfMonth = ym.atDay(1)
        val gridStart = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())

        val days = (0 until 42).map { offset ->
            val d = gridStart.plusDays(offset.toLong())
            val resolved = TaskPlanner.tasksForDay(allTasks, completions, d)
            DayCell(
                date = d,
                taskCount = resolved.size,
                isCurrentMonth = d.month == ym.month && d.year == ym.year,
                isToday = d == today
            )
        }

        var total = 0
        var done = 0
        (1..ym.lengthOfMonth()).forEach { day ->
            val resolved = TaskPlanner.tasksForDay(allTasks, completions, ym.atDay(day))
            total += resolved.size
            done += resolved.count { it.isDone }
        }

        val monthYear = ym.year * 100 + ym.monthValue
        val monthly = allTasks
            .filter { it.period == "month" && it.monthYear == monthYear }
            .sortedBy { it.isDone }
            .map { t ->
                TaskUi(
                    id = t.id,
                    title = t.title,
                    date = firstOfMonth,
                    dueTime = null,
                    isDone = t.isDone,
                    priority = t.priority,
                    category = t.category
                )
            }

        return MonthUiState(
            yearMonth = ym,
            days = days,
            monthlyTasks = monthly,
            totalTasks = total,
            doneTasks = done
        )
    }
}
