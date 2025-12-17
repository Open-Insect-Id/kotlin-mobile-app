package org.openinsectid.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Camera : Screen("camera")
    object History : Screen("history")
    object ViewImage : Screen("view/{fileName}") {
        fun createRoute(fileName: String) = "view/$fileName"
    }
}

@Composable
fun MainNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController = navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.ViewImage.route) { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName")
            fileName?.let {
                ViewImageScreen(navController = navController, fileName = it)
            }
        }
    }
}
