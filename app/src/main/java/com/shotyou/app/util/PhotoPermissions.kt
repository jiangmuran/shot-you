package com.shotyou.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Coarse state of the app's access to the device photo library, normalised across
 * the very different permission models of Android 14+, 13 and <=12.
 */
enum class PhotoAccess {
    /** No permission granted yet (or denied). */
    NONE,

    /** Android 14+ only: the user granted access to a limited set of photos. */
    PARTIAL,

    /** Full read access to the photo library. */
    FULL,
}

/**
 * Version-aware helpers for the photo-library runtime permissions.
 *
 * - Android 14+ (API 34): [Manifest.permission.READ_MEDIA_IMAGES] OR
 *   [Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] (partial / user-selected).
 * - Android 13 (API 33): [Manifest.permission.READ_MEDIA_IMAGES].
 * - Android <= 12 (API <= 32): [Manifest.permission.READ_EXTERNAL_STORAGE].
 */
object PhotoPermissions {

    /**
     * The permissions to request for the current OS version. On Android 14+ we request
     * both the full and the user-selected permission so the system shows the photo picker
     * that lets the user choose "selected photos".
     */
    val requestPermissions: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** Computes the current [PhotoAccess] level by inspecting granted permissions. */
    fun access(context: Context): PhotoAccess {
        fun granted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when {
                granted(Manifest.permission.READ_MEDIA_IMAGES) -> PhotoAccess.FULL
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> PhotoAccess.PARTIAL
                else -> PhotoAccess.NONE
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                if (granted(Manifest.permission.READ_MEDIA_IMAGES)) PhotoAccess.FULL else PhotoAccess.NONE

            else ->
                if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) PhotoAccess.FULL else PhotoAccess.NONE
        }
    }

    /** True if we can read at least some photos (full or partial). */
    fun hasAnyAccess(context: Context): Boolean = access(context) != PhotoAccess.NONE
}
