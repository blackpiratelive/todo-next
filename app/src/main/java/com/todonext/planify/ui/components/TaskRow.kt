package com.todonext.planify.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todonext.planify.data.local.TaskEntity
import com.todonext.planify.ui.theme.AccentBlue
import com.todonext.planify.ui.theme.AccentBlueLight
import com.todonext.planify.ui.theme.CompletedGreen
import com.todonext.planify.ui.theme.OverdueRed

fun formatRelativeDate(epochMillis: Long): Pair<String, Boolean> {
    val now = System.currentTimeMillis()
    val days = ((epochMillis - now) / (24 * 60 * 60 * 1000)).toInt()
    val isOverdue = days < 0
    val text = when {
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        days == -1 -> "Yesterday"
        days > 1 -> "in $days days"
        else -> "${-days} days ago"
    }
    return text to isOverdue
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    isExpanded: Boolean,
    labels: List<String>,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onUpdateTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showLabelMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { if (!isExpanded) onClick() },
            shape = RoundedCornerShape(16.dp),
            color = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isExpanded) 2.dp else 1.dp,
            shadowElevation = if (isExpanded) 1.dp else 0.5.dp
        ) {
            if (!isExpanded) {
                // COLLAPSED LAYOUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedCheckbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        accentColor = if (task.isCompleted) CompletedGreen
                        else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (task.isCompleted)
                                    TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (task.dueDate != null) {
                            val (dateText, isOverdue) = remember(task.dueDate) {
                                formatRelativeDate(task.dueDate)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isOverdue) OverdueRed
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverdue) OverdueRed
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (task.label != null) {
                            Text(
                                text = task.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }

                    if (task.isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = AccentBlueLight
                        )
                    }
                }
            } else {
                // EXPANDED LAYOUT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1: Checkbox + Title TextField + Pin + Collapse Chevron
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedCheckbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleComplete() },
                            accentColor = if (task.isCompleted) CompletedGreen
                            else MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        var titleText by remember { mutableStateOf(task.title) }
                        BasicTextField(
                            value = titleText,
                            onValueChange = {
                                titleText = it
                                onUpdateTask(task.copy(title = it))
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                if (titleText.isEmpty()) {
                                    Text(
                                        text = "Task title",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(onClick = { onUpdateTask(task.copy(isPinned = !task.isPinned)) }) {
                            Icon(
                                imageVector = if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin task",
                                tint = if (task.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = onClick) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Collapse task"
                            )
                        }
                    }

                    // Row 2: Description text field (clean BasicTextField)
                    var descText by remember { mutableStateOf(task.description ?: "") }
                    BasicTextField(
                        value = descText,
                        onValueChange = {
                            descText = it
                            onUpdateTask(task.copy(description = it.ifBlank { null }))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        decorationBox = { innerTextField ->
                            if (descText.isEmpty()) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Row 3: Date Title
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Row 4: Due date text & Relative date
                        val (dateText, isOverdue) = if (task.dueDate != null) {
                            formatRelativeDate(task.dueDate)
                        } else {
                            "No due date" to false
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clickable { showDatePicker = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (task.dueDate != null) {
                                    val sdf = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault())
                                    "${sdf.format(java.util.Date(task.dueDate))} ($dateText)"
                                } else {
                                    "Set a due date"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Row 5: Action Icons & More options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { /* Attachments action */ }) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = "Attachment",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = { showLabelMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Label,
                                    contentDescription = "Labels",
                                    tint = if (task.label != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                // Label select dropdown
                                DropdownMenu(
                                    expanded = showLabelMenu,
                                    onDismissRequest = { showLabelMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            onUpdateTask(task.copy(label = null))
                                            showLabelMenu = false
                                        }
                                    )
                                    labels.forEach { label ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                onUpdateTask(task.copy(label = label))
                                                showLabelMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showPriorityMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Priority",
                                    tint = when (task.priority) {
                                        1 -> OverdueRed
                                        5 -> MaterialTheme.colorScheme.primary
                                        9 -> CompletedGreen
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                                // Priority select dropdown
                                DropdownMenu(
                                    expanded = showPriorityMenu,
                                    onDismissRequest = { showPriorityMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            onUpdateTask(task.copy(priority = 0))
                                            showPriorityMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Low") },
                                        onClick = {
                                            onUpdateTask(task.copy(priority = 9))
                                            showPriorityMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Medium") },
                                        onClick = {
                                            onUpdateTask(task.copy(priority = 5))
                                            showPriorityMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("High") },
                                        onClick = {
                                            onUpdateTask(task.copy(priority = 1))
                                            showPriorityMenu = false
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { /* Reminder Alarm action */ }) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = "Reminder",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(onClick = {
                            isVisible = false
                            onDelete()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Delete or more options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Native Inline Add Subtasks row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Subtask action */ }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Add Subtasks",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Material 3 Native DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = task.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateTask(task.copy(dueDate = datePickerState.selectedDateMillis))
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateTask(task.copy(dueDate = null))
                    showDatePicker = false
                }) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
