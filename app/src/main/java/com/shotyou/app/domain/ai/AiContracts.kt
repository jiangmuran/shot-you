package com.shotyou.app.domain.ai

import com.shotyou.app.domain.model.AiSettings

/** An image handed to an AI provider, already decoded to bytes. */
data class AiImage(
    val id: String,
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg",
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is AiImage && id == other.id && mimeType == other.mimeType)

    override fun hashCode(): Int = 31 * id.hashCode() + mimeType.hashCode()
}

/** Token / image counters returned by a provider call, used for the usage dashboard. */
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val imageCount: Int = 0,
    val estimatedCostUsd: Double = 0.0,
)

/** One cluster proposed by the VLM, referencing input image ids. */
data class VlmGroup(
    val memberIds: List<String>,
    val referenceIds: List<String>,
    val title: String,
    val reason: String,
)

data class GroupingResult(
    val groups: List<VlmGroup>,
    val usage: TokenUsage = TokenUsage(),
)

data class PromptContext(
    val groupTitle: String? = null,
    val groupReason: String? = null,
    val editableAspects: List<String> = listOf("hair", "expression", "pose", "position"),
)

data class PromptResult(
    val prompt: String,
    val usage: TokenUsage = TokenUsage(),
)

data class GeneratedImage(
    val bytes: ByteArray,
    val mimeType: String = "image/png",
)

data class ImageGenOptions(
    val count: Int = 1,
    val size: String? = null,
)

data class ImageGenResult(
    val images: List<GeneratedImage>,
    val usage: TokenUsage = TokenUsage(),
)

/** Thrown by providers for configuration / network / API errors with a user-facing message. */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Groups visually-similar photos and nominates reference frames. */
interface VlmProvider {
    val model: String
    suspend fun groupSimilar(images: List<AiImage>, instruction: String? = null): GroupingResult
}

/** Refines a raw user prompt into a high-quality generation prompt. */
interface LlmProvider {
    val model: String
    suspend fun optimizePrompt(rawPrompt: String, context: PromptContext): PromptResult
}

/** Generates an image from reference photos + a prompt. */
interface ImageGenProvider {
    val model: String
    suspend fun generate(
        references: List<AiImage>,
        prompt: String,
        options: ImageGenOptions = ImageGenOptions(),
    ): ImageGenResult
}

/**
 * Builds provider instances from the current [AiSettings]. The concrete factory
 * (Gemini / OpenAI clients) is provided by the data layer via DI.
 */
interface AiProviderFactory {
    fun vlm(settings: AiSettings): VlmProvider
    fun llm(settings: AiSettings): LlmProvider
    fun imageGen(settings: AiSettings): ImageGenProvider
}
