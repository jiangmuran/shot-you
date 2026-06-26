package com.shotyou.app.ui.screens.generate

import androidx.compose.runtime.Composable
import com.shotyou.app.ui.components.StubScreen

/** P1 stub — owned by the Review/Generate agent in P2. */
@Composable
fun GenerateScreen(onJobEnqueued: (String) -> Unit, onBack: () -> Unit) {
    StubScreen(title = "Generate", subtitle = "Choose a template, optimize, generate")
}
