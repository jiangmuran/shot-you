package com.shotyou.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language switching via AppCompat. With the `autoStoreLocales` metadata in the
 * manifest, the choice is persisted and re-applied automatically across API levels
 * (delegating to the framework on Android 13+).
 *
 * Accepted tags: "system" (follow device), "en", "zh".
 */
object AppLocale {

    fun apply(tag: String) {
        val locales = if (tag.isBlank() || tag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** The current tag ("system" if following the device). */
    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "system" else locales[0]?.language ?: "system"
    }
}
