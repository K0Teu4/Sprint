package ru.sprint.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sprint.app.MainActivity
import ru.sprint.app.R
import ru.sprint.app.data.PlannerDatabase
import ru.sprint.app.data.TaskEntity
import java.time.LocalDate

class SprintWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context, manager, ids)
    }

    override fun onEnabled(context: Context) { refresh(context) }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SprintWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) updateAll(context, manager, ids)
        }

        private fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = PlannerDatabase.get(context).taskDao().observeAll()
                kotlinx.coroutines.flow.first(tasks).let { all ->
                    val today = LocalDate.now().toString()
                    val day = all.filter { it.date == today && it.parentId == null }
                        .sortedWith(compareBy<TaskEntity> { it.completed }.thenByDescending { it.priority }.thenBy { it.time ?: "99:99" }.thenBy { it.id })
                    val children = all.filter { it.date == today && it.parentId != null }.groupBy { it.parentId }
                    val active = day.count { !it.completed }
                    val done = day.count { it.completed }
                    val views = RemoteViews(context.packageName, R.layout.widget_sprint)
                    views.setTextViewText(R.id.widget_title, "Sprint")
                    val english = context.getSharedPreferences("sprint", Context.MODE_PRIVATE).getBoolean("english", false)
                    views.setTextViewText(R.id.widget_date, if (english) "Today" else "Сегодня")
                    views.setTextViewText(R.id.widget_count, if (english) "$active active · $done done" else "$active активных · $done выполнено")
                    val open = PendingIntent.getActivity(context, 7001, Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val add = PendingIntent.getActivity(context, 7002, Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra(MainActivity.EXTRA_OPEN_QUICK_ADD, true) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_root, open)
                    views.setOnClickPendingIntent(R.id.widget_add, add)
                    val rows = listOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3, R.id.widget_task_4)
                    rows.forEachIndexed { index, id ->
                        val task = day.getOrNull(index)
                        if (task == null) {
                            views.setTextViewText(id, if (index == 0 && day.isEmpty()) if (english) "No tasks today" else "Сегодня задач нет" else "")
                            views.setTextColor(id, Color.rgb(145, 153, 147))
                            views.setViewVisibility(id, if (index == 0 && day.isEmpty()) View.VISIBLE else View.GONE)
                        } else {
                            views.setViewVisibility(id, View.VISIBLE)
                            val prefix = if (task.time.isNullOrBlank()) "" else "${task.time}  "
                            val marker = when (task.priority) { 3 -> "!  "; 2 -> "•  "; else -> "" }
                            val childCount = children[task.id]?.size ?: 0
                            val suffix = if (childCount > 0) "  ·  $childCount" else ""
                            views.setTextViewText(id, prefix + marker + task.title + suffix)
                            views.setContentDescription(id, task.title)
                            views.setTextColor(id, if (task.completed) Color.rgb(112, 119, 114) else Color.rgb(243, 244, 239))
                        }
                    }
                    ids.forEach { manager.updateAppWidget(it, views) }
                }
            }
        }
    }
}
