package com.shotyou.app.domain.ai

import com.shotyou.app.domain.model.StylePreset

/**
 * Builds the final image-generation prompt from the user's base prompt plus a style preset,
 * an intensity (0..100), optional refinement suggestions, and the editable aspects. When
 * [chinese] is true the whole prompt is written in Simplified Chinese so the model's output
 * matches the user's language.
 */
object PromptComposer {

    fun compose(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        suggestion: String? = null,
        editableAspects: List<String> = listOf("hair", "expression", "pose", "position"),
        chinese: Boolean = false,
    ): String {
        val clamped = intensity.coerceIn(0, 100)
        return if (chinese) {
            val strength = when {
                clamped < 34 -> "克制、含蓄"
                clamped < 67 -> "适中"
                else -> "强烈、明显"
            }
            buildString {
                append(basePrompt.trim())
                append("\n\n风格:${style.directiveZh}。风格强度$strength(强度 $clamped%)。")
                if (!suggestion.isNullOrBlank()) append("\n\n用户希望的调整:${suggestion.trim()}。")
                append("\n\n").append(REALISM_GUIDANCE_ZH)
                append(
                    "\n\n可以调整发型、表情、姿势、位置、光线与背景构图让画面更出彩,但必须保留每个人真实的身份与样貌。" +
                        "若提供了多张参考图,可将它们自然融合成一张(保持一致的光线、透视、比例与阴影)," +
                        "但不要凭空捏造原图中不存在的场景或元素。只输出一张照片。",
                )
            }
        } else {
            val strength = when {
                clamped < 34 -> "subtle and restrained"
                clamped < 67 -> "balanced"
                else -> "strong and pronounced"
            }
            buildString {
                append(basePrompt.trim())
                append("\n\nStyle: ${style.directive}. Apply the styling $strength (intensity $clamped%).")
                if (!suggestion.isNullOrBlank()) append("\n\nRequested adjustments from the user: ${suggestion.trim()}.")
                append("\n\n").append(REALISM_GUIDANCE)
                append(
                    "\n\nYou may adjust ${editableAspects.joinToString(", ")}, lighting and background " +
                        "composition to make the shot more flattering, but preserve each person's true " +
                        "identity and likeness. If several reference photos are provided you may blend " +
                        "them into one with consistent lighting, perspective, scale and shadows — but do " +
                        "NOT invent scenery or elements that are not present in the reference photos. " +
                        "Output a single photo.",
                )
            }
        }
    }

    /** "beautiful but believable" — not the plastic AI look. */
    const val REALISM_GUIDANCE: String =
        "Render it with the eye of a skilled professional photographer. " +
            "Composition: strong intentional framing — rule of thirds, balanced negative space, clean " +
            "leading lines, a flattering camera angle and focal length (35-85mm look), clear subject " +
            "separation from a tidy, non-distracting background. " +
            "Light: deliberate, directional yet soft lighting with natural falloff and gentle shadows " +
            "(golden hour or soft window light), harmonious true-to-life color. " +
            "Realism: it must look like a genuine candid photograph from a real camera — natural skin " +
            "texture with subtle pores, physically-plausible depth of field, fine grain. Avoid the " +
            "over-smoothed, waxy, over-sharpened, over-saturated 'AI look', plastic skin or mangled " +
            "hands/teeth. Beautiful but believable — a viewer should not be able to tell it was AI."

    const val REALISM_GUIDANCE_ZH: String =
        "以专业摄影师的眼光来呈现。构图:刻意而讲究的取景——三分法、留白平衡、干净的引导线、" +
            "讨喜的拍摄角度与焦段(35-85mm 观感),主体与简洁不杂乱的背景清晰分离。" +
            "光线:有意识的定向柔光,自然的明暗过渡与柔和阴影(黄金时刻或柔和窗光),和谐真实的色彩。" +
            "真实感:必须像真实相机拍出的自然抓拍——保留自然的肤质与毛孔、合理的景深与细腻颗粒感。" +
            "避免过度磨皮、蜡感、过锐、过饱和的「AI 感」、塑料肌肤或畸形的手/牙。" +
            "要美,但更要可信——让人看不出是 AI 生成的。"

    private val flavors: List<Triple<String, String, String>> = listOf(
        Triple("Natural", "favor a natural, candid, true-to-life rendition", "偏自然、抓拍、真实的呈现"),
        Triple("Polished", "favor a crisp, polished, professional rendition", "偏清晰、精致、专业的呈现"),
        Triple("Warm", "favor a warm, soft, flattering rendition", "偏温暖、柔和、讨喜的呈现"),
        Triple("Vivid", "favor a vivid, vibrant, high-impact rendition", "偏鲜明、明快、有冲击力的呈现"),
    )

    /** Build [count] distinct candidate prompts (label → prompt) from one base. */
    fun variants(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        count: Int,
        editableAspects: List<String> = listOf("hair", "expression", "pose", "position"),
        chinese: Boolean = false,
    ): List<Pair<String?, String>> {
        val base = compose(basePrompt, style, intensity, null, editableAspects, chinese)
        val n = count.coerceIn(1, 4)
        if (n == 1) return listOf(null to base)
        return flavors.take(n).map { (labelEn, dirEn, dirZh) ->
            if (chinese) labelEn to "$base\n\n本张特别:${dirZh}。"
            else labelEn to "$base\n\nFor this candidate specifically: $dirEn."
        }
    }
}
