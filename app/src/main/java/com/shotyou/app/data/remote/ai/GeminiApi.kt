package com.shotyou.app.data.remote.ai

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Gemini generateContent API. The model id is part of the path
 * (`models/{model}:generateContent`) so we pass the full relative url per call.
 * Key is sent via the `x-goog-api-key` header.
 */
internal interface GeminiService {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateRequest,
    ): GeminiGenerateResponse
}

@Serializable
internal data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
internal data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

@Serializable
internal data class GeminiInlineData(
    val mimeType: String,
    val data: String,
)

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseModalities: List<String>? = null,
)

@Serializable
internal data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
    val error: GeminiError? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

@Serializable
internal data class GeminiUsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
)

@Serializable
internal data class GeminiError(
    val code: Int = 0,
    val message: String = "",
    val status: String = "",
)
