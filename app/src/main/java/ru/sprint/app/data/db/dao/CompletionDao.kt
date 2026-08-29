package ru.sprint.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.sprint.app.data.db.entity.CompletionEntity

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    suspend fun getAll(): List<CompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND dateMillis = :dateMillis")
    suspend fun delete(taskId: Long, dateMillis: Long)

    @Query("DELETE FROM completions")
    suspend fun clear()
}
