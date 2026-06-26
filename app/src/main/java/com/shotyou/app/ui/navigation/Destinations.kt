package com.shotyou.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.ui.graphics.vector.ImageVector
import com.shotyou.app.R

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
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LIBRARY(Routes.LIBRARY, R.string.nav_library, Icons.Outlined.PhotoLibrary),
    QUEUE(Routes.QUEUE, R.string.nav_queue, Icons.Outlined.ViewCarousel),
    TEMPLATES(Routes.TEMPLATES, R.string.nav_templates, Icons.Outlined.AutoAwesome),
    USAGE(Routes.USAGE, R.string.nav_usage, Icons.Outlined.QueryStats),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
}
