package com.shotyou.app.data.remote.ai

import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.ai.GroupingResult
import com.shotyou.app.domain.ai.ImageGenOptions
import com.shotyou.app.domain.ai.ImageGenProvider
import com.shotyou.app.domain.ai.ImageGenResult
import com.shotyou.app.domain.ai.LlmProvider
import com.shotyou.app.domain.ai.PromptContext
import com.shotyou.app.domain.ai.PromptResult
import com.shotyou.app.domain.ai.VlmProvider
import com.shotyou.app.domain.model.AiSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1 placeholder so the DI graph is complete and the app compiles/runs before the real
 * Gemini/OpenAI clients land. The AI-networking agent (P2) replaces this entire file +
 * package and the AiModule binding with concrete provider implementations.
 */
private const val MSG = "AI provider not implemented yet. Configure keys in Settings once the networking layer lands."

@Singleton
class PlaceholderAiProviderFactory @Inject constructor() : AiProviderFactory {
    override fun vlm(settings: AiSettings): VlmProvider = object : VlmProvider {
        override val model: String = settings.vlmModel
        override suspend fun groupSimilar(images: List<AiImage>, instruction: String?): GroupingResult =
            throw AiException(MSG)
    }

    override fun llm(settings: AiSettings): LlmProvider = object : LlmProvider {
        override val model: String = settings.llmModel
        override suspend fun optimizePrompt(rawPrompt: String, context: PromptContext): PromptResult =
            throw AiException(MSG)
    }

    override fun imageGen(settings: AiSettings): ImageGenProvider = object : ImageGenProvider {
        override val model: String = settings.imageModel
        override suspend fun generate(
            references: List<AiImage>,
            prompt: String,
            options: ImageGenOptions,
        ): ImageGenResult = throw AiException(MSG)
    }
}
