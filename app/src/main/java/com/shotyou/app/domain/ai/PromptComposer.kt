package com.shotyou.app.domain.ai

import com.shotyou.app.domain.model.StylePreset

/**
 * Builds the final image-generation prompt from a base prompt + style preset + intensity
 * (0..100) + optional refinement suggestion + editable aspects. When [chinese] is true the
 * whole prompt is written in Simplified Chinese.
 *
 * For a batch of candidates, [variants] produces visibly different versions spanning
 * conservative → balanced → bold so the user has a real choice (not three near-identical
 * results).
 */
object PromptComposer {

    /** Aspects the model is allowed to improve. Broad on purpose ("怎么好看怎么来"). */
    val DEFAULT_ASPECTS_EN = listOf(
        "hair and hair movement/flow", "expression", "pose", "position",
        "clothing and accessories", "camera angle and viewpoint", "composition and framing",
        "lighting, ambiance and weather",
    )
    private const val ASPECTS_ZH =
        "发型与发丝飘逸感、表情、姿势、位置、服饰与配饰、拍摄视角、画面构图、光线氛围与天气"

    fun compose(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        suggestion: String? = null,
        editableAspects: List<String> = DEFAULT_ASPECTS_EN,
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
                    "\n\n你可以自由调整$ASPECTS_ZH,怎么好看怎么来,把成片做得明显比原图更精致出彩;" +
                        "但必须保留主体真实的身份与样貌(同一个人/同一只动物/同一物体)。" +
                        "若提供了多张参考图,可自然融合成一张(保持一致的光线、透视、比例与阴影)," +
                        "但不要凭空捏造原图中不存在的人物、场景或元素。只输出一张照片。",
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
                    "\n\nYou may freely improve ${editableAspects.joinToString(", ")} — whatever makes " +
                        "the photo look best — and make the result clearly more polished than the original; " +
                        "but preserve the subject's true identity and likeness (same person / same animal / " +
                        "same object). If several reference photos are provided, blend them into one with " +
                        "consistent lighting, perspective, scale and shadows — but do NOT invent subjects, " +
                        "scenery or elements that are not present in the reference photos. Output a single photo.",
                )
            }
        }
    }

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
            "避免过度磨皮、蜡感、过锐、过饱和的「AI 感」、塑料肌肤或畸形的手/牙。要美,但更要可信。"

    /** A candidate variant: label + target intensity + an extra steering line. */
    private data class VariantSpec(
        val labelEn: String, val labelZh: String,
        val intensity: Int,
        val extraEn: String?, val extraZh: String?,
    )

    private val CONSERVATIVE = VariantSpec(
        "Conservative", "保守", 22,
        "Conservative version: stay very close to the original — only subtle cleanup, light retouch and small improvements. Minimal change.",
        "保守版:尽量贴近原图,只做轻微清理、淡淡精修与小幅提升,改动最小。",
    )
    private val BALANCED = VariantSpec(
        "Balanced", "中立", 55,
        "Balanced version: a tasteful middle ground between faithful and enhanced.",
        "中立版:在忠于原图与美化之间取得恰到好处的平衡。",
    )
    private val BOLD = VariantSpec(
        "Bold", "大胆", 90,
        "Bold version: take real creative license to make it strikingly better — improve the camera angle and composition, elevate the lighting, ambiance and weather, refine the hair (including flattering movement/flow), clothing and accessories, and the pose. The result should look noticeably more polished and beautiful than the original, while still clearly being the same subject with their true identity.",
        "大胆版:放手让画面明显更出彩——优化拍摄视角与构图,提升光线、氛围与天气,精修发型(含讨喜的发丝飘逸感)、服饰与配饰,改善姿势。成片要明显比原图更精致好看,但仍清晰是同一主体、保留其真实身份。",
    )

    /**
     * Build [count] distinct candidate prompts (label → prompt). count==3 → conservative /
     * balanced / bold; count==2 → conservative / bold; count==1 → one at [intensity].
     */
    fun variants(
        basePrompt: String,
        style: StylePreset,
        intensity: Int,
        count: Int,
        editableAspects: List<String> = DEFAULT_ASPECTS_EN,
        chinese: Boolean = false,
    ): List<Pair<String?, String>> {
        val n = count.coerceIn(1, 3)
        val specs = when (n) {
            1 -> listOf(VariantSpec("", "", intensity, null, null))
            2 -> listOf(CONSERVATIVE, BOLD)
            else -> listOf(CONSERVATIVE, BALANCED, BOLD)
        }
        return specs.map { spec ->
            val base = compose(basePrompt, style, spec.intensity, null, editableAspects, chinese)
            val extra = if (chinese) spec.extraZh else spec.extraEn
            val prompt = if (extra.isNullOrBlank()) base else "$base\n\n$extra"
            val label = if (n == 1) null else if (chinese) spec.labelZh else spec.labelEn
            label to prompt
        }
    }
}
