package ru.sprint.app.ui.screens.day

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.sprint.app.SprintApplication
import ru.sprint.app.data.db.entity.TaskEntity
import ru.sprint.app.domain.model.Category
import ru.sprint.app.ui.common.TaskFormState
import ru.sprint.app.util.DateUtils
import ru.sprint.app.util.ResolvedTask
import ru.sprint.app.util.TaskPlanner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class DayViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as SprintApplication).repository

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbar: MutableSharedFlow<String> = _snackbar

    private var lastDeleted: TaskEntity? = null

    val state: StateFlow<DayUiState> = combine(
        selectedDate,
        weekStart,
        repo.allTasks(),
        repo.completions()
    ) { date, ws, allTasks, completions ->
        val weekDates = (0 until 7).map { ws.plusDays(it.toLong()) }
        val tasks = TaskPlanner.tasksForDay(allTasks, completions, date)
            .sortedWith(
                compareBy<ResolvedTask> { it.entity.dueTime ?: Int.MAX_VALUE }
                    .thenBy { it.isDone }
            )
            .map { r ->
                TaskItem(
                    id = r.entity.id,
                    title = r.entity.title,
                    details = r.entity.details,
                    date = date,
                    dueTimeMinutes = r.entity.dueTime,
                    isDone = r.isDone,
                    isRecurring = r.entity.isRecurring,
                    priority = r.entity.priority,
                    category = Category.fromKey(r.entity.category)
                )
            }
        DayUiState(selectedDate = date, weekDates = weekDates, tasks = tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        weekStart.value = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun nextWeek() = selectDate(selectedDate.value.plusWeeks(1))
    fun prevWeek() = selectDate(selectedDate.value.minusWeeks(1))

    fun toggleDone(task: TaskItem, done: Boolean) {
        viewModelScope.launch {
            if (task.isRecurring) {
                repo.markCompletion(task.id, DateUtils.startOfDayMillis(task.date), done)
            } else {
                repo.setDone(task.id, done)
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        lastDeleted = TaskEntity(
            id = task.id,
            title = task.title,
            details = task.details,
            date = DateUtils.startOfDayMillis(task.date),
            dueTime = task.dueTimeMinutes,
            isDone = task.isDone,
            isRecurring = task.isRecurring,
            priority = task.priority,
            category = task.category.key
        )
        viewModelScope.launch {
            repo.deleteTask(task.id)
            _snackbar.emit("Задача удалена")
        }
    }

    fun undoDelete() {
        lastDeleted?.let { e ->
            viewModelScope.launch { repo.addTask(e) }
        }
        lastDeleted = null
    }

    fun updateTask(id: Long, form: TaskFormState) {
        viewModelScope.launch {
            val existing = repo.getTask(id) ?: return@launch
            repo.updateTask(
                existing.copy(
                    title = form.title.trim(),
                    details = form.details,
                    date = DateUtils.startOfDayMillis(form.date),
                    dueTime = form.dueTimeMinutes,
                    isRecurring = form.isRecurring,
                    priority = form.priority,
                    category = form.category.key
                )
            )
        }
    }
}
