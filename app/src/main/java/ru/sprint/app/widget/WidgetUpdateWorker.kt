package ru.sprint.app.widget

import android.content.Context
import android.widget.RemoteViews
import android.view.View
import android.graphics.Color
import android.appwidget.AppWidgetManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import ru.sprint.app.R
import ru.sprint.app.data.PlannerDatabase
import java.time.LocalDate

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val manager = AppWidgetManager.getInstance(applicationContext)
            val component = ComponentName(applicationContext, SprintWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)

            if (ids.isEmpty()) return Result.success()

            runBlocking {
                val tasks = PlannerDatabase.get(applicationContext).taskDao().getAll()
                val today = LocalDate.now().toString()
                val day = tasks.filter { it.date == today && it.parentId == null }
                    .sortedWith(compareBy<ru.sprint.app.data.db.entity.TaskEntity> { it.completed }
                        .thenByDescending { it.priority }
                        .thenBy { it.time ?: "99:99" }
                        .thenBy { it.id })
                val children = tasks.filter { it.date == today && it.parentId != null }.groupBy { it.parentId }
                val active = day.count { !it.completed }
                val done = day.count { it.completed }

                val views = RemoteViews(applicationContext.packageName, R.layout.widget_sprint)
                views.setTextViewText(R.id.widget_title, "Sprint")
                val english = applicationContext.getSharedPreferences("sprint", Context.MODE_PRIVATE)
                    .getBoolean("english", false)
                views.setTextViewText(R.id.widget_date, if (english) "Today" else "Сегодня")
                views.setTextViewText(
                    R.id.widget_count,
                    if (english) " active ·  done" else " активных ·  выполнено"
                )

                val open = PendingIntent.getActivity(
                    applicationContext, 7001,
                    Intent(applicationContext, ru.sprint.app.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val add = PendingIntent.getActivity(
                    applicationContext, 7002,
                    Intent(applicationContext, ru.sprint.app.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("ru.sprint.app.MainActivity.EXTRA_OPEN_QUICK_ADD", true)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                v


iews.setOnClickPendingIntent(R.id.widget_root, open)
                views.setOnClickPendingIntent(R.id.widget_add, add)

                val rows = listOf(
                    R.id.widget_task_1,
                    R.id.widget_task_2,
                    R.id.widget_task_3,
                    R.id.widget_task_4
                )

                rows.forEachIndexed { index, id ->
                    val task = day.getOrNull(index)
                    if (task == null) {
                        views.setTextViewText(
                            id,
                            if (index == 0 && day.isEmpty()) {
                                if (english) "No tasks today" else "Сегодня задач нет"
                            } else {
                                ""
                            }
                        )
                        views.setTextColor(id, Color.rgb(145, 153, 147))
                        views.setViewVisibility(
                            id,
                            if (index == 0 && day.isEmpty()) View.VISIBLE else View.GONE
                        )
                    } else {
                        views.setViewVisibility(id, View.VISIBLE)
                        val prefix = if (task.time.isNullOrBlank()) "" else " "
                        val marker = when (task.priority) {
                            3 -> "! "
                            2 -> "• "
                            else -> ""
                        }
                        val childCount = children[task.id]?.size ?: 0
                        val suffix = if (childCount > 0) " · " else ""
                        views.setTextViewText(id, prefix + marker + task.title + suffix)
                        views.setContentDescription(id, task.title)
                        views.setTextColor(
                            id,
                            if (task.completed) Color.rgb(112, 119, 114) else Color.rgb(243, 244, 239)
                        )
                    }
                }

                ids.forEach { manager.updateAppWidget(it, views) }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
