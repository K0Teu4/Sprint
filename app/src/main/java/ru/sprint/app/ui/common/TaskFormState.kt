package ru.sprint.app.ui.common

import ru.sprint.app.domain.model.Category
import java.time.LocalDate

data class TaskFormState(
    val title: String = "",
    val details: String = "",
    val date: LocalDate = LocalDate.now(),
    val dueTimeMinutes: Int? = null,
    val isRecurring: Boolean = false,
    val period: String = "day",
    val category: Category = Category.PERSONAL,
    val priority: Int = 0
) {
    val isValid: Boolean get() = title.trim().length >= 2
}

object TimePresets {
    val common = listOf(
        "07:00", "08:00", "09:00", "09:15", "09:30", "10:00",
        "11:00", "12:00", "13:00", "14:00",
        "15:00", "16:00", "17:00", "18:00",
        "19:00", "20:00", "21:00", "22:00"
    )
}
