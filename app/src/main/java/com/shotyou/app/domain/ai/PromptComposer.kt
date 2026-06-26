package com.shotyou.app.domain.ai

import com.shotyou.app.domain.model.StylePreset

/**
 * Builds the final image-generation prompt from the user's base prompt plus a style
 * preset, an intensity (0..100), optional free-text refinement suggestions, and the
 * editable aspects. Keeping all of these as inputs to one composer means style, intensity
 * and result-page "ask for changes" iterations all flow through the same mechanism.
 */
object PromptComposer {

    fun compose(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        suggestion: String? = null,
        editableAspects: List<String> = listOf("hair", "expression", "pose", "position"),
    ): String {
        val clamped = intensity.coerceIn(0, 100)
        val strength = when {
            clamped < 34 -> "subtle and restrained"
            clamped < 67 -> "balanced"
            else -> "strong and pronounced"
        }
        return buildString {
            append(basePrompt.trim())
            append("\n\nStyle: ${style.directive}. Apply the styling $strength (intensity $clamped%).")
            if (!suggestion.isNullOrBlank()) {
                append("\n\nRequested adjustments from the user: ${suggestion.trim()}.")
            }
            append(
                "\n\nYou may adjust ${editableAspects.joinToString(", ")} as needed, " +
                    "but preserve each person's true identity and likeness. Output a single polished photo.",
            )
        }
    }
}
