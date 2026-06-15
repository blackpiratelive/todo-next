package com.todonext.planify.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todonext.planify.ui.theme.AccentBlue
import com.todonext.planify.ui.theme.InboxBlue
import com.todonext.planify.ui.theme.LabelsBrown
import com.todonext.planify.ui.theme.ScheduledPurple
import com.todonext.planify.ui.theme.TodayGreen
import com.todonext.planify.viewmodel.TaskFilter

@Composable
fun NavigationDrawerContent(
    currentFilter: TaskFilter,
    labels: List<String>,
    currentLabel: String?,
    onFilterSelected: (TaskFilter) -> Unit,
    onLabelSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var labelsExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Planify",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = AccentBlue,
                modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items
            DrawerNavItem(
                icon = Icons.Outlined.AllInbox,
                label = "Inbox",
                accentColor = InboxBlue,
                isSelected = currentFilter == TaskFilter.INBOX,
                onClick = { onFilterSelected(TaskFilter.INBOX) }
            )

            DrawerNavItem(
                icon = Icons.Outlined.Today,
                label = "Today",
                accentColor = TodayGreen,
                isSelected = currentFilter == TaskFilter.TODAY,
                onClick = { onFilterSelected(TaskFilter.TODAY) }
            )

            DrawerNavItem(
                icon = Icons.Outlined.CalendarMonth,
                label = "Scheduled",
                accentColor = ScheduledPurple,
                isSelected = currentFilter == TaskFilter.SCHEDULED,
                onClick = { onFilterSelected(TaskFilter.SCHEDULED) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Labels section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { labelsExpanded = !labelsExpanded }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Label,
                    contentDescription = null,
                    tint = LabelsBrown,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Labels",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (labelsExpanded) Icons.Outlined.ExpandLess
                    else Icons.Outlined.ExpandMore,
                    contentDescription = if (labelsExpanded) "Collapse labels" else "Expand labels",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = labelsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    if (labels.isEmpty()) {
                        Text(
                            text = "No labels yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 64.dp, top = 4.dp, bottom = 8.dp)
                        )
                    } else {
                        labels.forEach { label ->
                            val isSelected = currentFilter == TaskFilter.LABEL && currentLabel == label
                            DrawerNavItem(
                                icon = Icons.Outlined.Label,
                                label = label,
                                accentColor = LabelsBrown,
                                isSelected = isSelected,
                                onClick = { onLabelSelected(label) },
                                indented = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Settings
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            DrawerNavItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                accentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                isSelected = false,
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    indented: Boolean = false
) {
    val backgroundColor = if (isSelected)
        accentColor.copy(alpha = 0.12f)
    else
        Color.Transparent

    val contentColor = if (isSelected)
        accentColor
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (indented) 20.dp else 12.dp)
            .padding(start = if (indented) 24.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}
