package com.widgetfactory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.widgetfactory.app.settings.SettingsStore
import com.widgetfactory.app.ui.SnackbarManager
import com.widgetfactory.app.ui.navigation.AppNavGraph
import com.widgetfactory.app.ui.theme.WidgetFactoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dynamicColor = remember { SettingsStore.isDynamicColorEnabled(this) }
            val themeMode = remember { SettingsStore.getThemeMode(this) }
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            WidgetFactoryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val snackbarManager = remember { SnackbarManager(snackbarHostState, scope) }
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        AppNavGraph(
                            navController = navController,
                            snackbarManager = snackbarManager,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
