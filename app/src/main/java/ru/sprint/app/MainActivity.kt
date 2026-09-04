package ru.sprint.app

import android.app.*
import android.content.*
import android.os.Bundle
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.sprint.app.data.PlannerDatabase
import ru.sprint.app.ui.SprintApp
import ru.sprint.app.widget.SprintWidgetProvider

class MainActivity : ComponentActivity() {
    private var openQuickAdd by androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        openQuickAdd = intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)
        val db = PlannerDatabase.get(applicationContext)
        setContent { SprintApp(db.taskDao(), openQuickAdd = openQuickAdd, onQuickAddConsumed = { openQuickAdd = false }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)) {
            openQuickAdd = true
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }

    companion object {
        const val EXTRA_OPEN_QUICK_ADD = "open_quick_add"
    }

    private fun hideNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Задача"
        val taskId = intent.getLongExtra("task_id", 0L)
        val english = context.getSharedPreferences("sprint", Context.MODE_PRIVATE).getBoolean("english", false)
        val channel = "reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channel, if (english) "Reminders" else "Напоминания", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val openIntent = PendingIntent.getActivity(context, taskId.hashCode(), Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Sprint")
            .setContentText(title)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(if (taskId != 0L) taskId.hashCode() else (System.currentTimeMillis() % 100000).toInt(), notification)
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_TIMEZONE_CHANGED && action != Intent.ACTION_TIME_CHANGED) return
        if (!context.getSharedPreferences("sprint", Context.MODE_PRIVATE).getBoolean("notifications", true)) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = PlannerDatabase.get(context).taskDao().pendingReminders(LocalDate.now().toString())
                tasks.forEach { ru.sprint.app.ui.scheduleReminderFromSystem(context, it) }
                SprintWidgetProvider.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
