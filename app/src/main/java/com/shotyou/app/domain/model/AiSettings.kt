package com.shotyou.app.domain.model

/** Default OpenAI-compatible model ids. Users may override any of these in Settings. */
object DefaultModels {
    const val API_BASE_URL = "https://api.openai.com/v1"
    const val VLM = "gpt-4o"
    const val LLM = "gpt-4o-mini"
    const val IMAGE = "gpt-image-1"
}

/**
 * A look the user can pick for a generation. [directive] is the (English) instruction
 * fed to the image model; the display label is localized in the UI by [id].
 */
enum class StylePreset(val id: String, val directive: String) {
    REALISTIC(
        "realistic",
        "photorealistic and faithful to the original subject, natural skin texture and lighting, no over-processing",
    ),
    BEAUTIFY(
        "beautify",
        "subtly beautified: clear smooth skin, flattering soft light, bright eyes, polished yet still natural",
    ),
    CINEMATIC(
        "cinematic",
        "cinematic look with dramatic lighting, shallow depth of field and filmic color grading",
    ),
    FRESH(
        "fresh",
        "clean fresh natural style, soft daylight, airy bright tones and true-to-life colors",
    ),
    ARTISTIC(
        "artistic",
        "artistic stylized rendering with expressive color and light and a tasteful creative interpretation",
    );

    companion object {
        fun fromId(id: String): StylePreset = entries.firstOrNull { it.id == id } ?: REALISTIC
    }
}

/**
 * All user-configurable settings. Persisted via DataStore. The API key lives on-device
 * only; the app calls the (OpenAI-compatible) endpoint directly — including custom hosts.
 */
data class AiSettings(
    // OpenAI-compatible endpoint
    val apiBaseUrl: String = DefaultModels.API_BASE_URL,
    val apiKey: String = "",
    val vlmModel: String = DefaultModels.VLM,
    val llmModel: String = DefaultModels.LLM,
    val imageModel: String = DefaultModels.IMAGE,

    // Background queue behaviour
    val maxConcurrentJobs: Int = 1,
    val minRequestIntervalMs: Long = 0L,
    val autoRetryOnFailure: Boolean = true,
    val maxRetries: Int = 2,
    val requireWifi: Boolean = false,

    // Background persistence & notifications
    val runInBackground: Boolean = false,
    val progressNotifications: Boolean = true,

    // Prompt behaviour
    val autoOptimizePrompt: Boolean = true,
    val defaultStyle: String = StylePreset.REALISTIC.id,
    val defaultIntensity: Int = 50,

    // Pricing — used to compute the usage/cost dashboard. All default to 0.0 (cost shown
    // as 0 until the user fills these in).
    val pricePerImage: Double = 0.0,
    val pricePer1kInputTokens: Double = 0.0,
    val pricePer1kOutputTokens: Double = 0.0,
    val currencySymbol: String = "$",

    // UI
    val appLanguage: String = "system", // "system" | "en" | "zh"
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiBaseUrl.isNotBlank()
}
