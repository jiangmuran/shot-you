package com.shotyou.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shotyou.app.ui.screens.batch.BatchScreen
import com.shotyou.app.ui.screens.generate.GenerateScreen
import com.shotyou.app.ui.screens.groups.GroupsScreen
import com.shotyou.app.ui.screens.library.LibraryScreen
import com.shotyou.app.ui.screens.queue.QueueScreen
import com.shotyou.app.ui.screens.result.ResultScreen
import com.shotyou.app.ui.screens.settings.SettingsScreen
import com.shotyou.app.ui.screens.templates.TemplatesScreen
import com.shotyou.app.ui.screens.usage.UsageScreen

@Composable
fun ShotYouNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelTab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    val destinations = backStackEntry?.destination
                    TopLevelTab.entries.forEach { tab ->
                        val selected = destinations?.hierarchy?.any { it.route == tab.route } == true
                        val label = stringResource(tab.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        // The bottom bar handles its own bottom inset; each screen has its own
        // Scaffold/TopAppBar that handles the status-bar (top) inset. Without this,
        // the top inset would be applied twice, leaving a white band at the top.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier,
            route = "root",
        ) {
            composable(Routes.LIBRARY) {
                ScreenContainer(padding) {
                    LibraryScreen(onPhotosGrouped = { navController.navigate(Routes.GROUPS) })
                }
            }
            composable(Routes.QUEUE) {
                ScreenContainer(padding) {
                    QueueScreen(onOpenTask = { batchId -> navController.navigate(Routes.batch(batchId)) })
                }
            }
            composable(Routes.TEMPLATES) {
                ScreenContainer(padding) { TemplatesScreen() }
            }
            composable(Routes.USAGE) {
                ScreenContainer(padding) { UsageScreen() }
            }
            composable(Routes.SETTINGS) {
                ScreenContainer(padding) { SettingsScreen() }
            }
            composable(Routes.GROUPS) {
                GroupsScreen(
                    onStarted = { navController.navigateToTab(Routes.QUEUE) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.GENERATE) {
                GenerateScreen(
                    onBatchEnqueued = { batchId ->
                        navController.navigate(Routes.batch(batchId)) {
                            popUpTo(Routes.GROUPS)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RESULT) { entry ->
                val jobId = entry.arguments?.getString("jobId").orEmpty()
                ResultScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
            composable(Routes.BATCH) { entry ->
                val batchId = entry.arguments?.getString("batchId").orEmpty()
                BatchScreen(
                    batchId = batchId,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.navigateToTab(Routes.LIBRARY) },
                )
            }
        }
    }
}

/** Navigate to a bottom-tab destination with consistent saved-state handling so every
 *  tab stays reachable (also used by "Start"/"Done" so they don't break the back stack). */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ScreenContainer(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    // Only reserve space for the bottom navigation bar; the screen's own Scaffold
    // handles the top status-bar inset, so we must NOT re-apply it here.
    Box(Modifier.padding(bottom = padding.calculateBottomPadding())) { content() }
}
