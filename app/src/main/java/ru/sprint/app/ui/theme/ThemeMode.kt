package ru.sprint.app.ui.theme

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    companion object {
        fun fromInt(i: Int): ThemeMode = values().getOrNull(i) ?: SYSTEM
    }
}
