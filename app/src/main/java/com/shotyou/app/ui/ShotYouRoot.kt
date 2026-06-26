package com.shotyou.app.ui

import androidx.compose.runtime.Composable
import com.shotyou.app.ui.navigation.ShotYouNavHost

/** Root composable: hosts the app's navigation graph. */
@Composable
fun ShotYouRoot() {
    ShotYouNavHost()
}
