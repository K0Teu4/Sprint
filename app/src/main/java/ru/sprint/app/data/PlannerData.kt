package ru.sprint.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "tasks",
    indices = [Index("date"), Index("completed"), Index("priority"), Index("category"), Index("parentId"), Index("seriesId")]
)
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
    val completedAt: Long? = null,
    val category: String = "PERSONAL",
    val parentId: Long? = null,
    val seriesId: Long? = null
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, CASE WHEN parentId IS NULL THEN 0 ELSE 1 END, CASE WHEN completed = 0 THEN 0 ELSE 1 END, priority DESC, time ASC, id DESC")
    fun observeAll(): Flow<List<TaskEntity>>
    @Insert suspend fun insert(task: TaskEntity): Long
    @Insert suspend fun insertAll(tasks: List<TaskEntity>): List<Long>
    @Update suspend fun update(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)
    @Query("SELECT * FROM tasks WHERE reminder = 1 AND completed = 0 AND date >= :today") suspend fun pendingReminders(today: String): List<TaskEntity>
    @Query("DELETE FROM tasks") suspend fun deleteAll()
    @Query("DELETE FROM tasks WHERE parentId = :parentId") suspend fun deleteChildren(parentId: Long)
    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY date ASC, time ASC, id ASC") suspend fun childrenOf(parentId: Long): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId ORDER BY date ASC, time ASC, id ASC") suspend fun seriesOf(seriesId: Long): List<TaskEntity>
    @Query("DELETE FROM tasks WHERE seriesId = :seriesId") suspend fun deleteSeries(seriesId: Long)
}

@Database(entities = [TaskEntity::class], version = 6, exportSchema = false)
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
