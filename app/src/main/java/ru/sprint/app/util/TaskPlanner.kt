package ru.sprint.app.util

import ru.sprint.app.data.db.entity.CompletionEntity
import ru.sprint.app.data.db.entity.TaskEntity
import java.time.LocalDate

data class ResolvedTask(
    val entity: TaskEntity,
    val isDone: Boolean
)

object TaskPlanner {
    /** Задачи на день: разовые с этой датой + повторяющиеся. Месячные не попадают. */
    fun tasksForDay(
        allTasks: List<TaskEntity>,
        completions: List<CompletionEntity>,
        date: LocalDate
    ): List<ResolvedTask> {
        val dayStart = DateUtils.startOfDayMillis(date)
        val dayEnd = DateUtils.endOfDayMillis(date)
        val doneSet = completions.map { it.taskId to it.dateMillis }.toSet()
        return allTasks
            .filter { t -> t.period != "month" }
            .filter { t ->
                if (t.isRecurring) true
                else t.date != null && t.date in dayStart..dayEnd
            }
            .map { t ->
                val done = if (t.isRecurring) doneSet.contains(t.id to dayStart) else t.isDone
                ResolvedTask(t, done)
            }
    }
}
