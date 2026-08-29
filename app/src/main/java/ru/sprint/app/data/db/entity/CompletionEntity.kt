package ru.sprint.app.data.db.entity

import androidx.room.Entity

@Entity(tableName = "completions", primaryKeys = ["taskId", "dateMillis"])
data class CompletionEntity(
    val taskId: Long,
    val dateMillis: Long
)
