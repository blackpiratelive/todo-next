package com.todonext.planify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val caldavHref: String? = null,
    val eTag: String? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val priority: Int = 0,
    val label: String? = null,
    val description: String? = null,
    val isPinned: Boolean = false,
    val lastModifiedLocally: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
