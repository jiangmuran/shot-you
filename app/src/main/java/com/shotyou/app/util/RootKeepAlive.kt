package com.shotyou.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optional root-based keep-alive. When the device is rooted, it whitelists the app from
 * Doze / battery optimization via `su`, which greatly reduces the chance of the system
 * killing background generation — WITHOUT holding a wakelock, so battery impact is minimal.
 * Completely safe / no-op when root is unavailable.
 */
object RootKeepAlive {

    /** Returns true if the root commands ran successfully. */
    suspend fun apply(packageName: String): Boolean = withContext(Dispatchers.IO) {
        runRoot(
            listOf(
                "dumpsys deviceidle whitelist +$packageName",
                "cmd appops set $packageName RUN_IN_BACKGROUND allow",
                "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow",
            ),
        )
    }

    private fun runRoot(commands: List<String>): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", commands.joinToString(" ; ")))
        process.waitFor() == 0
    } catch (_: Exception) {
        false // no root / su not available
    }
}
