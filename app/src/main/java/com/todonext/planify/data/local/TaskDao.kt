package com.todonext.planify.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
          AND isDeleted = 0
          AND parentTaskId IS NULL
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, 
            dueDate ASC
        """
    )
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
          AND isDeleted = 0
          AND parentTaskId IS NULL
          AND dueDate IS NOT NULL 
          AND dueDate <= :endOfDay 
        ORDER BY dueDate ASC
        """
    )
    fun getTodayTasks(endOfDay: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
          AND isDeleted = 0
          AND parentTaskId IS NULL
          AND dueDate IS NOT NULL 
        ORDER BY dueDate ASC
        """
    )
    fun getScheduledTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE label = :label AND parentTaskId IS NULL AND isDeleted = 0")
    fun getTasksByLabel(label: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId AND isDeleted = 0")
    fun getSubtasksForTask(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId AND isDeleted = 0")
    suspend fun getSubtasksForTaskSync(parentId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1")
    suspend fun getDeletedTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE caldavHref = :href LIMIT 1")
    suspend fun getTaskByHref(href: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT DISTINCT label FROM tasks WHERE label IS NOT NULL")
    fun getAllLabels(): Flow<List<String>>

    @Query(
        """
        UPDATE tasks 
        SET isSynced = 1, 
            caldavHref = :href, 
            eTag = :eTag 
        WHERE id = :id
        """
    )
    suspend fun markSynced(id: String, href: String, eTag: String)
}
