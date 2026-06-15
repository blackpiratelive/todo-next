package com.todonext.planify.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.todonext.planify.ui.components.NavigationDrawerContent
import com.todonext.planify.ui.components.TaskRow
import com.todonext.planify.ui.theme.AccentBlue
import com.todonext.planify.viewmodel.TaskFilter
import com.todonext.planify.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var quickAddText by rememberSaveable { mutableStateOf("") }
    var currentLabelFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Show sync error in snackbar
    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { error ->
            snackbarHostState.showSnackbar(message = error)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerContent(
                    currentFilter = uiState.currentFilter,
                    labels = uiState.labels,
                    currentLabel = currentLabelFilter,
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                        currentLabelFilter = null
                        scope.launch { drawerState.close() }
                    },
                    onLabelSelected = { label ->
                        viewModel.setLabelFilter(label)
                        currentLabelFilter = label
                        scope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (uiState.currentFilter) {
                                TaskFilter.INBOX -> "Inbox"
                                TaskFilter.TODAY -> "Today"
                                TaskFilter.SCHEDULED -> "Scheduled"
                                TaskFilter.LABEL -> currentLabelFilter ?: "Label"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Open navigation drawer"
                            )
                        }
                    },
                    actions = {
                        SyncButton(
                            isSyncing = uiState.isSyncing,
                            onClick = { viewModel.triggerSync() }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = { /* Focus quick add */ },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = AccentBlue,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                if (uiState.tasks.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                            Text(
                                text = "No tasks yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Add a task to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(
                            items = uiState.tasks,
                            key = { it.id }
                        ) { task ->
                            TaskRow(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                modifier = Modifier.animateItem()
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }

                // Quick Add Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                        )
                        TextField(
                            value = quickAddText,
                            onValueChange = { quickAddText = it },
                            placeholder = {
                                Text(
                                    "Add a task...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (quickAddText.isNotBlank()) {
                                        viewModel.addTask(title = quickAddText.trim())
                                        quickAddText = ""
                                    }
                                }
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncButton(
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotationAngle"
    )

    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Sync,
            contentDescription = if (isSyncing) "Syncing..." else "Sync",
            modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier,
            tint = if (isSyncing)
                AccentBlue
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
