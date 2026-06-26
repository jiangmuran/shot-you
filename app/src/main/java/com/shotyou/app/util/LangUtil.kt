package com.shotyou.app.util

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/** Locale helpers for choosing the language of AI prompts/output. */
object LangUtil {

    /** True when the active app/device language is Chinese (prefers the per-app locale). */
    fun isChinese(): Boolean {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) appLocales[0]?.language else null
        return (tag ?: Locale.getDefault().language) == "zh"
    }
}
