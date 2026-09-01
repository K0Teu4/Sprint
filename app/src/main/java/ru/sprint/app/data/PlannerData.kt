package ru.sprint.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks", indices = [Index("date"), Index("completed"), Index("priority")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val date: String = "",
    val time: String? = null,
    val priority: Int = 1,
    val recurrence: String = "NONE",
    val reminder: Boolean = false,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, CASE WHEN completed = 0 THEN 0 ELSE 1 END, priority DESC, time ASC, id DESC")
    fun observeAll(): Flow<List<TaskEntity>>
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)
    @Query("DELETE FROM tasks") suspend fun deleteAll()
}

@Database(entities = [TaskEntity::class], version = 4, exportSchema = false)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    companion object {
        @Volatile private var instance: PlannerDatabase? = null
        fun get(context: android.content.Context): PlannerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, PlannerDatabase::class.java, "sprint_planner.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
