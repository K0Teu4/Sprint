package ru.sprint.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = "",
    val date: Long? = null,
    val dueTime: Int? = null,
    val isDone: Boolean = false,
    val isRecurring: Boolean = false,
    val period: String = "day",     // "day" | "month"
    val monthYear: Int = 0,         // YYYYMM для period="month"
    val priority: Int = 0,
    val category: String = "personal"
)
