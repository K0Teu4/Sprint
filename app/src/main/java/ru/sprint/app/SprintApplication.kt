package ru.sprint.app

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sprint.app.data.db.SprintDatabase
import ru.sprint.app.data.repository.TaskRepository
import ru.sprint.app.ui.theme.AccentColor
import ru.sprint.app.ui.theme.ThemeMode

class SprintApplication : Application() {

    val database: SprintDatabase by lazy { SprintDatabase.get(this) }
    val repository: TaskRepository by lazy { TaskRepository(database) }
    val prefs by lazy { getSharedPreferences("sprint_prefs", MODE_PRIVATE) }

    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val accent = MutableStateFlow(AccentColor.GREEN)

    override fun onCreate() {
        super.onCreate()
        themeMode.value = ThemeMode.fromInt(prefs.getInt("theme_mode", 0))
        accent.value = AccentColor.fromInt(prefs.getInt("accent", 0))
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        prefs.edit().putInt("theme_mode", mode.ordinal).apply()
    }

    fun setAccent(a: AccentColor) {
        accent.value = a
        prefs.edit().putInt("accent", a.ordinal).apply()
    }
}
