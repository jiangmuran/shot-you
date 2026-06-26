package com.shotyou.app.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI REST API. Key is sent via the `Authorization: Bearer <key>` header.
 */
internal interface OpenAiService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") auth: String,
        @Body request: OpenAiChatRequest,
    ): OpenAiChatResponse

    @POST("images/generations")
    suspend fun imageGenerations(
        @Header("Authorization") auth: String,
        @Body request: OpenAiImageRequest,
    ): OpenAiImageResponse
}

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>,
)

@Serializable
internal data class OpenAiContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: OpenAiImageUrl? = null,
) {
    companion object {
        fun text(text: String) = OpenAiContentPart(type = "text", text = text)
        fun imageDataUri(uri: String) = OpenAiContentPart(type = "image_url", imageUrl = OpenAiImageUrl(uri))
    }
}

@Serializable
internal data class OpenAiImageUrl(val url: String)

@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null,
    val error: OpenAiError? = null,
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiResponseMessage? = null,
)

@Serializable
internal data class OpenAiResponseMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
internal data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
internal data class OpenAiImageRequest(
    val model: String,
    val prompt: String,
    val n: Int,
    val size: String? = null,
)

@Serializable
internal data class OpenAiImageResponse(
    val data: List<OpenAiImageData> = emptyList(),
    val usage: OpenAiImageUsage? = null,
    val error: OpenAiError? = null,
)

@Serializable
internal data class OpenAiImageData(
    @SerialName("b64_json") val b64Json: String? = null,
    val url: String? = null,
)

@Serializable
internal data class OpenAiImageUsage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
internal data class OpenAiError(
    val message: String = "",
    val type: String? = null,
    val code: String? = null,
)
