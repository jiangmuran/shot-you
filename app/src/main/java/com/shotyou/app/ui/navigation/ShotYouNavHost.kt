package com.shotyou.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
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
                    QueueScreen(onOpenResult = { jobId -> navController.navigate(Routes.result(jobId)) })
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
                    onOpenGroup = { navController.navigate(Routes.GENERATE) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.GENERATE) {
                GenerateScreen(
                    onJobEnqueued = { _ ->
                        navController.navigate(Routes.QUEUE) {
                            popUpTo(Routes.LIBRARY)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RESULT) { entry ->
                val jobId = entry.arguments?.getString("jobId").orEmpty()
                ResultScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(Modifier.padding(padding)) { content() }
}
