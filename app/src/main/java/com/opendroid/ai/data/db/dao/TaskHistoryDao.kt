package com.opendroid.ai.data.db.dao

import androidx.room.*
import com.opendroid.ai.data.db.entities.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskHistoryDao {
    @Query("SELECT * FROM task_history ORDER BY timestamp DESC")
    fun getTaskHistoryFlow(): Flow<List<TaskHistoryEntity>>

    @Query("SELECT * FROM task_history WHERE planId = :planId ORDER BY timestamp ASC, id ASC")
    suspend fun getTaskHistoryForPlan(planId: String): List<TaskHistoryEntity>

    @Query("SELECT * FROM task_history WHERE timestamp >= :fromTimestamp AND timestamp < :toTimestamp AND actionType IN ('SEND_WHATSAPP', 'SEND_SMS', 'SEND_TELEGRAM') AND success = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getSuccessfulMessagingHistory(fromTimestamp: Long, toTimestamp: Long, limit: Int): List<TaskHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(task: TaskHistoryEntity)

    @Query("DELETE FROM task_history")
    suspend fun clearAll()
}
