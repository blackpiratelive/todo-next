package com.todonext.planify.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todonext.planify.data.local.TaskEntity
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

@Composable
fun TaskRow(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.5.dp
        ) {
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

                IconButton(
                    onClick = {
                        isVisible = false
                        onDelete()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
