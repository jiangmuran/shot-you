package com.shotyou.app.ui.screens.result

import androidx.compose.runtime.Composable
import com.shotyou.app.ui.components.StubScreen

/** P1 stub — owned by the Review/Generate agent in P2. */
@Composable
fun ResultScreen(jobId: String, onBack: () -> Unit) {
    StubScreen(title = "Result", subtitle = "Generated image · regenerate if unhappy")
}
