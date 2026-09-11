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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ru.sprint.app.MainActivity
import ru.sprint.app.R
import ru.sprint.app.data.PlannerDatabase
import ru.sprint.app.data.db.entity.TaskEntity
import java.time.LocalDate

class SprintWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        // Планируем фоновую работу для обновления виджета
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_update",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ru.sprint.app.ACTION_REFRESH_WIDGET") {
            refresh(context)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SprintWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateAll(context, manager, ids)
            }
        }

        private fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val request = OneTimeWorkRequ


estBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget_update_manual",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
