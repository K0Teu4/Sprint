package ru.sprint.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val displayName: String,
    val light: Color,
    val dark: Color
) {
    GREEN("Зелёный", Color(0xFF2F8F63), Color(0xFF65D69B)),
    BLUE("Синий", Color(0xFF3D73D9), Color(0xFF82A9FF)),
    PURPLE("Фиолетовый", Color(0xFF7752D8), Color(0xFFB39AFF)),
    TERRACOTTA("Коралловый", Color(0xFFD7654C), Color(0xFFFF9B82));

    companion object {
        fun fromInt(i: Int): AccentColor = values().getOrNull(i) ?: GREEN
    }
}
