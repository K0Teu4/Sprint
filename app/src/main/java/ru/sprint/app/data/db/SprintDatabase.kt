package ru.sprint.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.sprint.app.data.db.dao.CompletionDao
import ru.sprint.app.data.db.dao.GoalDao
import ru.sprint.app.data.db.dao.TaskDao
import ru.sprint.app.data.db.entity.CompletionEntity
import ru.sprint.app.data.db.entity.GoalEntity
import ru.sprint.app.data.db.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, GoalEntity::class, CompletionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class SprintDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun completionDao(): CompletionDao

    companion object {
        @Volatile private var instance: SprintDatabase? = null

        fun get(context: Context): SprintDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SprintDatabase::class.java,
                    "sprint.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
