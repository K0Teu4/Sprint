package ru.sprint.app.domain.model

import androidx.compose.ui.graphics.Color

enum class Category(
    val key: String,
    val displayName: String,
    val color: Color
) {
    WORK(
        key = "work",
        displayName = "Работа",
        color = Color(0xFF6B8ECC)
    ),
    PERSONAL(
        key = "personal",
        displayName = "Личное",
        color = Color(0xFFC7815E)
    ),
    HEALTH(
        key = "health",
        displayName = "Здоровье",
        color = Color(0xFF5B8C5A)
    ),
    STUDY(
        key = "study",
        displayName = "Учёба",
        color = Color(0xFFB885C7)
    ),
    FINANCE(
        key = "finance",
        displayName = "Финансы",
        color = Color(0xFFD4A24C)
    );

    companion object {
        fun fromKey(key: String?): Category =
            values().firstOrNull { it.key == key } ?: PERSONAL
    }
}
