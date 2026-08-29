package ru.sprint.app.data.repository

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import ru.sprint.app.data.db.SprintDatabase
import ru.sprint.app.data.db.entity.CompletionEntity
import ru.sprint.app.data.db.entity.GoalEntity
import ru.sprint.app.data.db.entity.TaskEntity

class TaskRepository(private val db: SprintDatabase) {
    private val taskDao = db.taskDao()
    private val goalDao = db.goalDao()
    private val completionDao = db.completionDao()

    fun allTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun completions(): Flow<List<CompletionEntity>> = completionDao.observeAll()
    fun allGoals(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun getTask(id: Long): TaskEntity? = taskDao.getById(id)
    suspend fun addTask(task: TaskEntity): Long = taskDao.insert(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)
    suspend fun setDone(id: Long, done: Boolean) = taskDao.setDone(id, done)
    suspend fun moveTaskToDay(id: Long, newDateMillis: Long) = taskDao.moveToDay(id, newDateMillis)

    suspend fun markCompletion(taskId: Long, dateMillis: Long, done: Boolean) {
        if (done) completionDao.insert(CompletionEntity(taskId, dateMillis))
        else completionDao.delete(taskId, dateMillis)
    }

    suspend fun addGoal(goal: GoalEntity): Long = goalDao.insert(goal)
    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)
    suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)

    suspend fun clearAll() {
        taskDao.clear(); goalDao.clear(); completionDao.clear()
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        val tArr = JSONArray()
        taskDao.getAll().forEach { t ->
            tArr.put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("details", t.details)
                put("date", t.date ?: JSONObject.NULL)
                put("dueTime", t.dueTime ?: JSONObject.NULL)
                put("isDone", t.isDone); put("isRecurring", t.isRecurring)
                put("priority", t.priority); put("category", t.category)
            })
        }
        root.put("tasks", tArr)
        val gArr = JSONArray()
        goalDao.getAll().forEach { g ->
            gArr.put(JSONObject().apply {
                put("id", g.id); put("title", g.title); put("description", g.description)
                put("progress", g.progress); put("target", g.target); put("unit", g.unit)
                put("colorHex", g.colorHex); put("monthYear", g.monthYear)
            })
        }
        root.put("goals", gArr)
        val cArr = JSONArray()
        completionDao.getAll().forEach { c ->
            cArr.put(JSONObject().apply {
                put("taskId", c.taskId); put("dateMillis", c.dateMillis)
            })
        }
        root.put("completions", cArr)
        return root.toString(2)
    }

    suspend fun importJson(text: String) {
        val root = JSONObject(text)
        val tasks = mutableListOf<TaskEntity>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks += TaskEntity(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    details = o.optString("details"),
                    date = if (o.isNull("date")) null else o.optLong("date"),
                    dueTime = if (o.isNull("dueTime")) null else o.optInt("dueTime"),
                    isDone = o.optBoolean("isDone"),
                    isRecurring = o.optBoolean("isRecurring"),
                    priority = o.optInt("priority"),
                    category = o.optString("category", "personal")
                )
            }
        }
        val goals = mutableListOf<GoalEntity>()
        root.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                goals += GoalEntity(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    progress = o.optInt("progress"),
                    target = o.optInt("target", 100),
                    unit = o.optString("unit"),
                    colorHex = o.optString("colorHex", "#4A7C59"),
                    monthYear = o.optInt("monthYear")
                )
            }
        }
        val completions = mutableListOf<CompletionEntity>()
        root.optJSONArray("completions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                completions += CompletionEntity(o.optLong("taskId"), o.optLong("dateMillis"))
            }
        }
        clearAll()
        taskDao.insertAll(tasks)
        goalDao.insertAll(goals)
        completionDao.insertAll(completions)
    }
}
