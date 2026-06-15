package com.todonext.planify

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.todonext.planify.ui.screens.SettingsScreen
import com.todonext.planify.ui.screens.TaskListScreen
import com.todonext.planify.viewmodel.TaskViewModel

@Composable
fun PlanifyApp(viewModel: TaskViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {
        composable("tasks") {
            TaskListScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
