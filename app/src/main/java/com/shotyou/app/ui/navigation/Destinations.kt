package com.shotyou.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.ui.graphics.vector.ImageVector

/** Route constants for the whole app. */
object Routes {
    const val LIBRARY = "library"
    const val QUEUE = "queue"
    const val TEMPLATES = "templates"
    const val USAGE = "usage"
    const val SETTINGS = "settings"

    const val GROUPS = "groups"
    const val GENERATE = "generate"
    const val RESULT = "result/{jobId}"

    fun result(jobId: String) = "result/$jobId"
}

/** The five bottom-navigation tabs. */
enum class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    LIBRARY(Routes.LIBRARY, "Library", Icons.Outlined.PhotoLibrary),
    QUEUE(Routes.QUEUE, "Queue", Icons.Outlined.ViewCarousel),
    TEMPLATES(Routes.TEMPLATES, "Templates", Icons.Outlined.AutoAwesome),
    USAGE(Routes.USAGE, "Usage", Icons.Outlined.QueryStats),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
}
