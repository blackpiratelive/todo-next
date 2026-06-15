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

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs = _syncLogs.asStateFlow()

    @Volatile
    private var cachedCalendarUrl: String? = null

    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncLogs.value = _syncLogs.value + "[$timestamp] $message"
    }

    /**
     * Performs a full bi-directional sync between the local Room database
     * and the remote CalDAV server.
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        if (_isSyncing.value) {
            log("Warning: Sync already in progress, skipping execution")
            return@withContext SyncResult(
                success = false,
                errors = listOf("Sync already in progress")
            )
        }

        if (!preferencesManager.isConfigured) {
            log("Error: CalDAV server credentials not configured in settings")
            return@withContext SyncResult(
                success = false,
                errors = listOf("CalDAV server not configured")
            )
        }

        _isSyncing.value = true
        _syncLogs.value = emptyList() // clear previous logs
        log("Starting sync execution...")
        log("Server URL: ${preferencesManager.serverUrl}")
        log("Username: ${preferencesManager.username}")

        val errors = mutableListOf<String>()
        var uploaded = 0
        var downloaded = 0

        try {
            // Step 1: Build client
            log("Building CalDAV OkHttp client...")
            val client = CalDavClient(
                serverUrl = preferencesManager.serverUrl,
                username = preferencesManager.username,
                password = preferencesManager.password
            )

            // Step 2: Discover calendar URL
            log("Discovering calendar collection URL...")
            val calendarUrl = cachedCalendarUrl ?: run {
                val discovered = client.discoverCalendarUrl()
                if (discovered != null) {
                    cachedCalendarUrl = discovered
                    log("Discovered new calendar URL: $discovered")
                }
                discovered
            }

            if (calendarUrl == null) {
                log("Error: Could not discover any calendar supporting VTODO on the server.")
                return@withContext SyncResult(
                    success = false,
                    errors = listOf("Could not discover CalDAV calendar URL")
                )
            }
            log("Using calendar URL: $calendarUrl")

            // Step 3: Fetch remote todos
            log("Fetching remote tasks from server...")
            val remoteTodos = try {
                val fetched = client.fetchTodos(calendarUrl)
                log("Successfully fetched ${fetched.size} remote tasks.")
                fetched
            } catch (e: Exception) {
                val msg = "Failed to fetch remote tasks: ${e.message}"
                log("Error: $msg")
                errors.add(msg)
                emptyList()
            }

            // Step 4: Merge remote → local
            log("Merging remote tasks into local Room database...")
            for (remoteTodo in remoteTodos) {
                try {
                    val localTask = taskDao.getTaskByHref(remoteTodo.href)

                    if (localTask != null) {
                        // Task exists locally — update if remote eTag differs
                        if (localTask.eTag != remoteTodo.eTag) {
                            log("Task updated on server: '${remoteTodo.summary}'. Updating locally...")
                            val updatedTask = localTask.copy(
                                title = remoteTodo.summary,
                                isCompleted = remoteTodo.completed,
                                dueDate = remoteTodo.due,
                                priority = remoteTodo.priority,
                                label = remoteTodo.categories,
                                description = remoteTodo.description,
                                parentTaskId = remoteTodo.parentUid,
                                eTag = remoteTodo.eTag,
                                isSynced = true
                            )
                            taskDao.updateTask(updatedTask)
                            downloaded++
                        }
                    } else {
                        // New remote task — insert locally
                        log("New task found on server: '${remoteTodo.summary}'. Downloading...")
                        val newTask = TaskEntity(
                            id = remoteTodo.uid.ifBlank { java.util.UUID.randomUUID().toString() },
                            caldavHref = remoteTodo.href,
                            eTag = remoteTodo.eTag,
                            title = remoteTodo.summary,
                            isCompleted = remoteTodo.completed,
                            dueDate = remoteTodo.due,
                            priority = remoteTodo.priority,
                            label = remoteTodo.categories,
                            description = remoteTodo.description,
                            parentTaskId = remoteTodo.parentUid,
                            isSynced = true,
                            lastModifiedLocally = System.currentTimeMillis()
                        )
                        taskDao.insertTask(newTask)
                        downloaded++
                    }
                } catch (e: Exception) {
                    val msg = "Error processing task '${remoteTodo.summary}': ${e.message}"
                    log("Error: $msg")
                    errors.add(msg)
                }
            }

            // Step 4b: Handle remote deletions -> delete locally if not on server
            log("Checking for remote deletions...")
            try {
                val remoteHrefs = remoteTodos.map { it.href }.toSet()
                val allLocalTasks = taskDao.getAllTasksSync()
                for (localTask in allLocalTasks) {
                    if (localTask.caldavHref != null && !remoteHrefs.contains(localTask.caldavHref)) {
                        log("Task deleted on server: '${localTask.title}'. Deleting locally...")
                        taskDao.deleteTask(localTask)
                    }
                }
            } catch (e: Exception) {
                val msg = "Failed to process remote deletions: ${e.message}"
                log("Error: $msg")
                errors.add(msg)
            }

            // Step 5: Push unsynced local tasks → remote
            log("Checking for unsynced local tasks to upload...")
            val unsyncedTasks = try {
                val unsynced = taskDao.getUnsyncedTasks()
                log("Found ${unsynced.size} unsynced local tasks.")
                unsynced
            } catch (e: Exception) {
                val msg = "Failed to query unsynced local tasks: ${e.message}"
                log("Error: $msg")
                errors.add(msg)
                emptyList()
            }

            for (task in unsyncedTasks) {
                try {
                    log("Uploading local task '${task.title}' to server...")
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
                    log("Successfully uploaded task '${task.title}'. href: ${putResult.href}")
                    uploaded++
                } catch (e: Exception) {
                    val msg = "Failed to upload task '${task.title}': ${e.message}"
                    log("Error: $msg")
                    errors.add(msg)
                }
            }

            // Step 5b: Push local deletions → remote
            log("Checking for locally deleted tasks to sync with server...")
            try {
                val deletedTasks = taskDao.getDeletedTasks()
                log("Found ${deletedTasks.size} locally deleted tasks to sync.")
                for (task in deletedTasks) {
                    try {
                        if (task.caldavHref != null) {
                            log("Deleting task '${task.title}' on remote server...")
                            val success = client.deleteTodo(
                                calendarUrl = calendarUrl,
                                href = task.caldavHref,
                                eTag = task.eTag ?: ""
                            )
                            if (success) {
                                log("Successfully deleted task '${task.title}' on server.")
                            } else {
                                log("Warning: Server returned failure when deleting task '${task.title}'. Proceeding to delete locally.")
                            }
                        }
                        // Delete permanently from local DB
                        taskDao.deleteTask(task)
                    } catch (e: Exception) {
                        val msg = "Failed to sync deletion for task '${task.title}': ${e.message}"
                        log("Error: $msg")
                        errors.add(msg)
                    }
                }
            } catch (e: Exception) {
                val msg = "Failed to process local deletions: ${e.message}"
                log("Error: $msg")
                errors.add(msg)
            }

            log("Sync processing completed. Uploaded: $uploaded, Downloaded: $downloaded, Errors: ${errors.size}")
            SyncResult(
                uploaded = uploaded,
                downloaded = downloaded,
                errors = errors,
                success = errors.isEmpty()
            )
        } catch (e: Exception) {
            val msg = "Unexpected critical sync error: ${e.message}"
            log("Error: $msg")
            SyncResult(
                uploaded = uploaded,
                downloaded = downloaded,
                errors = errors + msg,
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
        log("Invalidated cached calendar collection URL.")
        cachedCalendarUrl = null
    }
}
