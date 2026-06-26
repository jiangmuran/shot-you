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

    /** Distinct "flavors" used to vary candidates within a batch (label → extra directive). */
    private val flavors: List<Pair<String, String>> = listOf(
        "Natural" to "favor a natural, candid, true-to-life rendition",
        "Polished" to "favor a crisp, polished, professional rendition",
        "Warm" to "favor a warm, soft, flattering rendition",
        "Vivid" to "favor a vivid, vibrant, high-impact rendition",
    )

    /**
     * Build [count] candidate prompts from one base, each nudged toward a different flavor so
     * the user has visibly distinct options to choose between. Returns (label → prompt) pairs.
     */
    fun variants(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        count: Int,
        editableAspects: List<String> = listOf("hair", "expression", "pose", "position"),
    ): List<Pair<String?, String>> {
        val base = compose(basePrompt, style, intensity, null, editableAspects)
        val n = count.coerceIn(1, 4)
        if (n == 1) return listOf(null to base)
        return flavors.take(n).map { (label, directive) ->
            label to "$base\n\nFor this candidate specifically: $directive."
        }
    }
}
