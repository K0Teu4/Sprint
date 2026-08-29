package ru.sprint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.sprint.app.ui.navigation.SprintNavGraph
import ru.sprint.app.ui.theme.SprintTheme
import ru.sprint.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SprintApplication
            val mode by app.themeMode.collectAsState()
            val accent by app.accent.collectAsState()
            val systemDark = isSystemInDarkTheme()
            SprintTheme(
                darkTheme = when (mode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> systemDark
                },
                accent = accent
            ) {
                SprintNavGraph()
            }
        }
    }
}
