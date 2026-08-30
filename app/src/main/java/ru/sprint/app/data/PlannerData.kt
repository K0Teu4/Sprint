package ru.sprint.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks", indices = [Index("date"), Index("completed"), Index("category"), Index("priority")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val date: String = "",
    val time: String? = null,
    val duration: Int = 30,
    val priority: Int = 1,
    val category: String = "Личное",
    val recurrence: String = "NONE",
    val completed: Boolean = false,
    val energy: Int = 1,
    val goal: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val month: String,
    val target: Int = 1,
    val progress: Int = 0,
    val color: Int = 0
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val duration: Int = 30,
    val priority: Int = 1,
    val category: String = "Личное",
    val energy: Int = 1,
    val recurrence: String = "NONE"
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, time ASC, completed ASC, priority DESC, id DESC") fun observeAll(): Flow<List<TaskEntity>>
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)
    @Query("DELETE FROM tasks") suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY month DESC, id DESC") fun observeAll(): Flow<List<GoalEntity>>
    @Insert suspend fun insert(goal: GoalEntity): Long
    @Update suspend fun update(goal: GoalEntity)
    @Delete suspend fun delete(goal: GoalEntity)
    @Query("DELETE FROM goals") suspend fun deleteAll()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY id DESC") fun observeAll(): Flow<List<TemplateEntity>>
    @Insert suspend fun insert(template: TemplateEntity): Long
    @Delete suspend fun delete(template: TemplateEntity)
    @Query("DELETE FROM templates") suspend fun deleteAll()
}

@Database(entities = [TaskEntity::class, GoalEntity::class, TemplateEntity::class], version = 3, exportSchema = false)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun templateDao(): TemplateDao
    companion object {
        @Volatile private var instance: PlannerDatabase? = null
        fun get(context: android.content.Context): PlannerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, PlannerDatabase::class.java, "sprint_planner.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
