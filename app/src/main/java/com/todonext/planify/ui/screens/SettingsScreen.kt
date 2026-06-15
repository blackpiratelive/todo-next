package com.todonext.planify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.todonext.planify.ui.theme.AccentBlue
import com.todonext.planify.ui.theme.CompletedGreen
import com.todonext.planify.ui.theme.OverdueRed
import com.todonext.planify.viewmodel.TaskViewModel

enum class ConnectionStatus {
    IDLE, TESTING, SUCCESS, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    
    var serverUrl by rememberSaveable { mutableStateOf(viewModel.getServerUrl()) }
    var username by rememberSaveable { mutableStateOf(viewModel.getUsername()) }
    var password by rememberSaveable { mutableStateOf(viewModel.getPassword()) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf(ConnectionStatus.IDLE) }
    var isSaved by remember { mutableStateOf(false) }

    // Reactively update connection status based on sync outcomes
    LaunchedEffect(uiState.isSyncing) {
        if (uiState.isSyncing) {
            connectionStatus = ConnectionStatus.TESTING
        } else if (connectionStatus == ConnectionStatus.TESTING) {
            connectionStatus = if (uiState.syncError != null) {
                ConnectionStatus.ERROR
            } else {
                ConnectionStatus.SUCCESS
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Nextcloud Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Nextcloud Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Server URL
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            isSaved = false
                            connectionStatus = ConnectionStatus.IDLE
                        },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://cloud.example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            cursorColor = AccentBlue
                        )
                    )

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            isSaved = false
                            connectionStatus = ConnectionStatus.IDLE
                        },
                        label = { Text("Username") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            cursorColor = AccentBlue
                        )
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            isSaved = false
                            connectionStatus = ConnectionStatus.IDLE
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Outlined.VisibilityOff
                                    else
                                        Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible)
                                        "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            cursorColor = AccentBlue
                        )
                    )

                    // Connection status
                    AnimatedVisibility(
                        visible = connectionStatus != ConnectionStatus.IDLE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (connectionStatus) {
                                ConnectionStatus.TESTING -> {
                                    Text(
                                        text = "Syncing and testing connection...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                ConnectionStatus.SUCCESS -> {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = CompletedGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sync completed successfully",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CompletedGreen
                                    )
                                }
                                ConnectionStatus.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Error,
                                        contentDescription = null,
                                        tint = OverdueRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sync failed: ${uiState.syncError ?: "unknown error"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OverdueRed
                                    )
                                }
                                else -> {}
                            }
                        }
                    }

                    // Saved indicator
                    AnimatedVisibility(
                        visible = isSaved && connectionStatus == ConnectionStatus.IDLE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = CompletedGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Settings saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = CompletedGreen
                            )
                        }
                    }

                    // Buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveServerConfig(
                                    url = serverUrl,
                                    username = username,
                                    password = password
                                )
                                isSaved = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue
                            ),
                            enabled = serverUrl.isNotBlank() &&
                                    username.isNotBlank() &&
                                    password.isNotBlank()
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.saveServerConfig(
                                    url = serverUrl,
                                    username = username,
                                    password = password
                                )
                                viewModel.triggerSync()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = serverUrl.isNotBlank() &&
                                    username.isNotBlank() &&
                                    password.isNotBlank() &&
                                    !uiState.isSyncing
                        ) {
                            Text(
                                text = "Test & Sync Now",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // Live Sync Logs Console
                    AnimatedVisibility(
                        visible = connectionStatus != ConnectionStatus.IDLE || syncLogs.isNotEmpty()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Sync Log Traces",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131314),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                val logScrollState = rememberScrollState()
                                LaunchedEffect(syncLogs.size) {
                                    logScrollState.animateScrollTo(logScrollState.maxValue)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(logScrollState)
                                        .padding(12.dp)
                                ) {
                                    if (syncLogs.isEmpty()) {
                                        Text(
                                            text = "Sync traces will appear here...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                            color = Color.Gray
                                        )
                                    } else {
                                        syncLogs.forEach { logLine ->
                                            Text(
                                                text = logLine,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                color = if (logLine.contains("Error", ignoreCase = true)) {
                                                    OverdueRed
                                                } else if (logLine.contains("Success", ignoreCase = true) || logLine.contains("completed", ignoreCase = true)) {
                                                    CompletedGreen
                                                } else {
                                                    Color.LightGray
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
