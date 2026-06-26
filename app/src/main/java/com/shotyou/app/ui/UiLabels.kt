package com.shotyou.app.ui

import androidx.annotation.StringRes
import com.shotyou.app.R
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.StylePreset

/** Localized display label for a [StylePreset]. The underlying directive stays English. */
@StringRes
fun StylePreset.labelRes(): Int = when (this) {
    StylePreset.REALISTIC -> R.string.style_realistic
    StylePreset.BEAUTIFY -> R.string.style_beautify
    StylePreset.CINEMATIC -> R.string.style_cinematic
    StylePreset.FRESH -> R.string.style_fresh
    StylePreset.ARTISTIC -> R.string.style_artistic
}

/** Localized label for an editable-aspect chip identifier (the id stays English). */
@StringRes
fun aspectLabelRes(aspect: String): Int = when (aspect) {
    "hair" -> R.string.aspect_hair
    "expression" -> R.string.aspect_expression
    "pose" -> R.string.aspect_pose
    "position" -> R.string.aspect_position
    else -> R.string.aspect_hair
}

/** Localized label for a usage-dashboard [AiOperation]. */
@StringRes
fun AiOperation.labelRes(): Int = when (this) {
    AiOperation.GROUPING -> R.string.usage_op_grouping
    AiOperation.PROMPT_OPTIMIZE -> R.string.usage_op_prompt
    AiOperation.IMAGE_GENERATION -> R.string.usage_op_image
}
