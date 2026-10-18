package com.widgetfactory.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.widgetfactory.app.ui.SnackbarManager
import com.widgetfactory.app.ui.editor.EditorScreen
import com.widgetfactory.app.ui.library.LibraryScreen
import com.widgetfactory.app.ui.settings.SettingsScreen

object Routes {
    const val LIBRARY = "library"
    const val EDITOR = "editor/{componentId}"
    const val EDITOR_NEW = "editor/new"
    const val SETTINGS = "settings"

    fun editor(componentId: String) = "editor/$componentId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarManager: SnackbarManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = modifier
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                snackbarManager = snackbarManager,
                onNewComponent = { navController.navigate(Routes.EDITOR_NEW) },
                onEditComponent = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("componentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val componentId = backStackEntry.arguments?.getString("componentId") ?: "new"
            EditorScreen(
                componentId = componentId,
                snackbarManager = snackbarManager,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                snackbarManager = snackbarManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
