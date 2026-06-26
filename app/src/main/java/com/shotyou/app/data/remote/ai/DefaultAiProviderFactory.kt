package com.shotyou.app.data.remote.ai

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.ai.ImageGenProvider
import com.shotyou.app.domain.ai.LlmProvider
import com.shotyou.app.domain.ai.VlmProvider
import com.shotyou.app.domain.model.AiSettings
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds OpenAI-compatible provider clients from the user's [AiSettings].
 *
 * The endpoint host is user-configurable: every provider targets a [Retrofit] whose base URL
 * is the (trailing-slash-normalised) [AiSettings.apiBaseUrl], so endpoints `chat/completions`
 * and `images/generations` resolve as relative paths. Retrofit services are cached by their
 * normalised base-url string so repeated calls reuse the same instance. The per-call API key
 * is passed as an `Authorization: Bearer <key>` header.
 */
@Singleton
class DefaultAiProviderFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : AiProviderFactory {

    private val converterFactory = json.asConverterFactory("application/json".toMediaType())

    /** Cache of OpenAI services keyed by their normalised base-url string. */
    private val services = ConcurrentHashMap<String, OpenAiService>()

    override fun vlm(settings: AiSettings): VlmProvider {
        val (service, key) = clientFor(settings)
        return OpenAiVlmProvider(service, key, json, settings.vlmModel)
    }

    override fun llm(settings: AiSettings): LlmProvider {
        val (service, key) = clientFor(settings)
        return OpenAiLlmProvider(service, key, settings.llmModel)
    }

    override fun imageGen(settings: AiSettings): ImageGenProvider {
        val (service, key) = clientFor(settings)
        return OpenAiImageGenProvider(service, key, settings.imageModel)
    }

    /** Validates settings and returns the (cached) service for the configured host + the api key. */
    private fun clientFor(settings: AiSettings): Pair<OpenAiService, String> {
        val key = settings.apiKey
        if (key.isBlank()) throw AiException("Set your API key in Settings")
        val baseUrl = settings.apiBaseUrl
        if (baseUrl.isBlank()) throw AiException("Set your API host (base URL) in Settings")

        val normalised = normaliseBaseUrl(baseUrl)
        val service = services.getOrPut(normalised) {
            Retrofit.Builder()
                .baseUrl(normalised)
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build()
                .create(OpenAiService::class.java)
        }
        return service to key
    }

    /** Retrofit requires the base url to end with a slash for relative endpoints to resolve. */
    private fun normaliseBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
