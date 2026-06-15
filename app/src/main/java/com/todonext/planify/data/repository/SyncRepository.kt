package com.todonext.planify.data.repository

import com.todonext.planify.data.local.TaskDao
import com.todonext.planify.data.local.TaskEntity
import com.todonext.planify.data.preferences.PreferencesManager
import com.todonext.planify.data.remote.CalDavClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val errors: List<String> = emptyList(),
    val success: Boolean = true
)

class SyncRepository(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager
) {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    @Volatile
    private var cachedCalendarUrl: String? = null

    /**
     * Performs a full bi-directional sync between the local Room database
     * and the remote CalDAV server.
     *
     * Steps:
     * 1. Build CalDavClient from stored credentials
     * 2. Discover (or use cached) calendar URL
     * 3. Fetch all remote VTODOs
     * 4. Merge remote → local (insert new, update if remote eTag differs)
     * 5. Push unsynced local tasks → remote
     * 6. Return sync summary
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        if (_isSyncing.value) {
            return@withContext SyncResult(
                success = false,
                errors = listOf("Sync already in progress")
            )
        }

        if (!preferencesManager.isConfigured) {
            return@withContext SyncResult(
                success = false,
                errors = listOf("CalDAV server not configured")
            )
        }

        _isSyncing.value = true
        val errors = mutableListOf<String>()
        var uploaded = 0
        var downloaded = 0

        try {
            // Step 1: Build client
            val client = CalDavClient(
                serverUrl = preferencesManager.serverUrl,
                username = preferencesManager.username,
                password = preferencesManager.password
            )

            // Step 2: Discover calendar URL
            val calendarUrl = cachedCalendarUrl ?: run {
                val discovered = client.discoverCalendarUrl()
                if (discovered != null) {
                    cachedCalendarUrl = discovered
                }
                discovered
            }

            if (calendarUrl == null) {
                return@withContext SyncResult(
                    success = false,
                    errors = listOf("Could not discover CalDAV calendar URL")
                )
            }

            // Step 3: Fetch remote todos
            val remoteTodos = try {
                client.fetchTodos(calendarUrl)
            } catch (e: Exception) {
                errors.add("Failed to fetch remote todos: ${e.message}")
                emptyList()
            }

            // Step 4: Merge remote → local
            for (remoteTodo in remoteTodos) {
                try {
                    val localTask = taskDao.getTaskByHref(remoteTodo.href)

                    if (localTask != null) {
                        // Task exists locally — update if remote eTag differs
                        if (localTask.eTag != remoteTodo.eTag) {
                            val updatedTask = localTask.copy(
                                title = remoteTodo.summary,
                                isCompleted = remoteTodo.completed,
                                dueDate = remoteTodo.due,
                                priority = remoteTodo.priority,
                                eTag = remoteTodo.eTag,
                                isSynced = true
                            )
                            taskDao.updateTask(updatedTask)
                            downloaded++
                        }
                    } else {
                        // New remote task — insert locally
                        val newTask = TaskEntity(
                            id = remoteTodo.uid.ifBlank { java.util.UUID.randomUUID().toString() },
                            caldavHref = remoteTodo.href,
                            eTag = remoteTodo.eTag,
                            title = remoteTodo.summary,
                            isCompleted = remoteTodo.completed,
                            dueDate = remoteTodo.due,
                            priority = remoteTodo.priority,
                            isSynced = true,
                            lastModifiedLocally = System.currentTimeMillis()
                        )
                        taskDao.insertTask(newTask)
                        downloaded++
                    }
                } catch (e: Exception) {
                    errors.add("Error processing remote todo '${remoteTodo.summary}': ${e.message}")
                }
            }

            // Step 5: Push unsynced local tasks → remote
            val unsyncedTasks = try {
                taskDao.getUnsyncedTasks()
            } catch (e: Exception) {
                errors.add("Failed to query unsynced tasks: ${e.message}")
                emptyList()
            }

            for (task in unsyncedTasks) {
                try {
                    val vtodoIcal = client.buildVTodo(task)
                    val putResult = client.putTodo(
                        calendarUrl = calendarUrl,
                        href = task.caldavHref,
                        vtodoIcal = vtodoIcal
                    )

                    // Mark as synced with the returned href and eTag
                    val eTag = putResult.eTag ?: ""
                    taskDao.markSynced(
                        id = task.id,
                        href = putResult.href,
                        eTag = eTag
                    )
                    uploaded++
                } catch (e: Exception) {
                    errors.add("Failed to upload task '${task.title}': ${e.message}")
                }
            }

            SyncResult(
                uploaded = uploaded,
                downloaded = downloaded,
                errors = errors,
                success = errors.isEmpty()
            )
        } catch (e: Exception) {
            SyncResult(
                uploaded = uploaded,
                downloaded = downloaded,
                errors = errors + "Unexpected sync error: ${e.message}",
                success = false
            )
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Clears the cached calendar URL, forcing re-discovery on next sync.
     */
    fun invalidateCalendarCache() {
        cachedCalendarUrl = null
    }
}
