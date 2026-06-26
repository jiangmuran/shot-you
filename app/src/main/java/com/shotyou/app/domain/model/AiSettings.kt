package com.shotyou.app.domain.model

/** Which backend a given AI role uses. */
enum class AiProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
}

/**
 * Default model ids per provider. Centralised so the UI and providers agree.
 * Users may override any of these in Settings.
 */
object DefaultModels {
    const val GEMINI_VLM = "gemini-2.5-flash"
    const val GEMINI_LLM = "gemini-2.5-flash"
    const val GEMINI_IMAGE = "gemini-2.5-flash-image"

    const val OPENAI_VLM = "gpt-4o"
    const val OPENAI_LLM = "gpt-4o-mini"
    const val OPENAI_IMAGE = "gpt-image-1"
}

/**
 * All user-configurable AI settings. Persisted via DataStore. API keys live on-device
 * only; the app calls providers directly.
 */
data class AiSettings(
    val vlmProvider: AiProviderType = AiProviderType.GEMINI,
    val llmProvider: AiProviderType = AiProviderType.GEMINI,
    val imageProvider: AiProviderType = AiProviderType.GEMINI,

    val vlmModel: String = DefaultModels.GEMINI_VLM,
    val llmModel: String = DefaultModels.GEMINI_LLM,
    val imageModel: String = DefaultModels.GEMINI_IMAGE,

    val geminiKey: String = "",
    val openAiKey: String = "",

    // Background queue behaviour
    val maxConcurrentJobs: Int = 1,
    val minRequestIntervalMs: Long = 0L,
    val autoRetryOnFailure: Boolean = true,
    val maxRetries: Int = 2,

    // Prompt behaviour
    val autoOptimizePrompt: Boolean = true,
    val requireWifi: Boolean = false,
) {
    fun keyFor(type: AiProviderType): String = when (type) {
        AiProviderType.GEMINI -> geminiKey
        AiProviderType.OPENAI -> openAiKey
    }

    val isConfigured: Boolean
        get() = keyFor(vlmProvider).isNotBlank() &&
            keyFor(llmProvider).isNotBlank() &&
            keyFor(imageProvider).isNotBlank()
}
