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
            append("\n\n").append(REALISM_GUIDANCE)
            append(
                "\n\nYou may adjust ${editableAspects.joinToString(", ")}, lighting and background " +
                    "composition to make the shot more flattering, but preserve each person's true " +
                    "identity and likeness. If several reference photos are provided you may combine " +
                    "them — e.g. place the subject from one photo into the nicer setting/background of " +
                    "another — blending them seamlessly with consistent lighting, perspective, scale and " +
                    "shadows. Output a single photo.",
            )
        }
    }

    /** Shared instruction that pushes for "beautiful but believable" — not the plastic AI look. */
    const val REALISM_GUIDANCE: String =
        "Render it with the eye of a skilled professional photographer. " +
            "Composition: use strong, intentional framing — rule of thirds, balanced negative space, " +
            "clean leading lines, a flattering camera angle and focal length (e.g. 35-85mm look), and " +
            "clear subject separation from a tidy, non-distracting background. " +
            "Light: deliberate, directional yet soft lighting with natural falloff and gentle shadows " +
            "(think golden hour or soft window light), harmonious and true-to-life color grading. " +
            "Realism: it must look like a genuine candid photograph from a real camera — natural skin " +
            "texture with subtle pores and imperfections, physically-plausible reflections and depth of " +
            "field, fine grain. Avoid the tell-tale over-smoothed, waxy, over-sharpened, over-saturated " +
            "'AI look', plastic skin, mangled hands/teeth, or impossible lighting. " +
            "Make it genuinely beautiful, but never at the cost of believability — a viewer should not be " +
            "able to tell it was AI-generated. Strike the balance between flattering and authentic."

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
