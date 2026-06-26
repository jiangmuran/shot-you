package com.shotyou.app.data.remote.ai

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.ai.ImageGenProvider
import com.shotyou.app.domain.ai.LlmProvider
import com.shotyou.app.domain.ai.VlmProvider
import com.shotyou.app.domain.model.AiProviderType
import com.shotyou.app.domain.model.AiSettings
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/"
private const val OPENAI_BASE = "https://api.openai.com/v1/"

/**
 * Builds concrete Gemini / OpenAI provider clients from the user's [AiSettings].
 * Retrofit services are created once and shared; per-call API keys are passed as headers.
 */
@Singleton
class DefaultAiProviderFactory @Inject constructor(
    okHttpClient: OkHttpClient,
    private val json: Json,
) : AiProviderFactory {

    private val converterFactory = json.asConverterFactory("application/json".toMediaType())

    private val geminiService: GeminiService = Retrofit.Builder()
        .baseUrl(GEMINI_BASE)
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(GeminiService::class.java)

    private val openAiService: OpenAiService = Retrofit.Builder()
        .baseUrl(OPENAI_BASE)
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(OpenAiService::class.java)

    override fun vlm(settings: AiSettings): VlmProvider {
        val key = requireKey(settings, settings.vlmProvider)
        return when (settings.vlmProvider) {
            AiProviderType.GEMINI -> GeminiVlmProvider(geminiService, key, json, settings.vlmModel)
            AiProviderType.OPENAI -> OpenAiVlmProvider(openAiService, key, json, settings.vlmModel)
        }
    }

    override fun llm(settings: AiSettings): LlmProvider {
        val key = requireKey(settings, settings.llmProvider)
        return when (settings.llmProvider) {
            AiProviderType.GEMINI -> GeminiLlmProvider(geminiService, key, settings.llmModel)
            AiProviderType.OPENAI -> OpenAiLlmProvider(openAiService, key, settings.llmModel)
        }
    }

    override fun imageGen(settings: AiSettings): ImageGenProvider {
        val key = requireKey(settings, settings.imageProvider)
        return when (settings.imageProvider) {
            AiProviderType.GEMINI -> GeminiImageGenProvider(geminiService, key, settings.imageModel)
            AiProviderType.OPENAI -> OpenAiImageGenProvider(openAiService, key, settings.imageModel)
        }
    }

    private fun requireKey(settings: AiSettings, type: AiProviderType): String {
        val key = settings.keyFor(type)
        if (key.isBlank()) throw AiException("Set your ${type.displayName} API key in Settings")
        return key
    }
}
