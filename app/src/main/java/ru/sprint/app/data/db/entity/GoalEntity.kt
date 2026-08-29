package ru.sprint.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val progress: Int = 0,          // текущий прогресс
    val target: Int = 100,          // целевое значение
    val unit: String = "",          // "км", "книг", "задач" и т.п.
    val colorHex: String = "#4A7C59",
    val monthYear: Int              // формат YYYYMM, например 202608
)
