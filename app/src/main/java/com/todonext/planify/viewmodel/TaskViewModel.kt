package com.todonext.planify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todonext.planify.data.local.AppDatabase
import com.todonext.planify.data.local.TaskDao
import com.todonext.planify.data.local.TaskEntity
import com.todonext.planify.data.preferences.PreferencesManager
import com.todonext.planify.data.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaskListState(
    val tasks: List<TaskEntity> = emptyList(),
    val labels: List<String> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.INBOX,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val isServerConfigured: Boolean = false
)

enum class TaskFilter {
    INBOX, TODAY, SCHEDULED, LABEL
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val taskDao: TaskDao = database.taskDao()
    private val preferencesManager: PreferencesManager = PreferencesManager(application)
    private val syncRepository: SyncRepository = SyncRepository(taskDao, preferencesManager)

    private val _currentFilter = MutableStateFlow(TaskFilter.INBOX)
    val currentLabelFilter = MutableStateFlow<String?>(null)

    private val _isSyncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)

    private fun endOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private val tasksFlow = combine(_currentFilter, currentLabelFilter) { filter, label ->
        Pair(filter, label)
    }.flatMapLatest { (filter, label) ->
        when (filter) {
            TaskFilter.INBOX -> taskDao.getActiveTasks()
            TaskFilter.TODAY -> taskDao.getTodayTasks(endOfTodayMillis())
            TaskFilter.SCHEDULED -> taskDao.getScheduledTasks()
            TaskFilter.LABEL -> taskDao.getTasksByLabel(label ?: "")
        }
    }

    val uiState: StateFlow<TaskListState> = combine(
        tasksFlow,
        _currentFilter,
        _isSyncing,
        _syncError,
        taskDao.getAllLabels()
    ) { tasks, filter, syncing, error, labels ->
        TaskListState(
            tasks = tasks,
            labels = labels,
            currentFilter = filter,
            isSyncing = syncing,
            syncError = error,
            isServerConfigured = preferencesManager.isConfigured
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListState()
    )

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun setLabelFilter(label: String) {
        currentLabelFilter.value = label
        _currentFilter.value = TaskFilter.LABEL
    }

    fun addTask(title: String, dueDate: Long? = null, label: String? = null) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                dueDate = dueDate,
                label = label,
                lastModifiedLocally = System.currentTimeMillis(),
                isSynced = false
            )
            taskDao.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                lastModifiedLocally = System.currentTimeMillis(),
                isSynced = false
            )
            taskDao.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val result = syncRepository.sync()
                if (!result.success && result.errors.isNotEmpty()) {
                    _syncError.value = result.errors.first()
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Sync failed"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun getServerUrl(): String = preferencesManager.serverUrl

    fun getUsername(): String = preferencesManager.username

    fun getPassword(): String = preferencesManager.password

    fun saveServerConfig(url: String, username: String, password: String) {
        preferencesManager.serverUrl = url
        preferencesManager.username = username
        preferencesManager.password = password
    }
}
