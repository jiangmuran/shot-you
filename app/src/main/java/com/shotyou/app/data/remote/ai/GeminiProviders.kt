package com.shotyou.app.data.remote.ai

import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.ai.GeneratedImage
import com.shotyou.app.domain.ai.GroupingResult
import com.shotyou.app.domain.ai.ImageGenOptions
import com.shotyou.app.domain.ai.ImageGenProvider
import com.shotyou.app.domain.ai.ImageGenResult
import com.shotyou.app.domain.ai.LlmProvider
import com.shotyou.app.domain.ai.PromptContext
import com.shotyou.app.domain.ai.PromptResult
import com.shotyou.app.domain.ai.TokenUsage
import com.shotyou.app.domain.ai.VlmProvider
import kotlinx.serialization.json.Json

private const val PROVIDER = "Gemini"

private fun GeminiGenerateResponse.failIfError() {
    error?.let { throw AiException("$PROVIDER API error ${it.code}: ${it.message}") }
}

private fun GeminiGenerateResponse.tokenUsage(imageCount: Int = 0): TokenUsage {
    val u = usageMetadata
    return TokenUsage(
        promptTokens = u?.promptTokenCount ?: 0,
        completionTokens = u?.candidatesTokenCount ?: 0,
        imageCount = imageCount,
    )
}

private fun GeminiGenerateResponse.firstText(): String? =
    candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }

private fun AiImage.toPart(): GeminiPart =
    GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = bytes.base64()))

private fun url(model: String): String = "models/$model:generateContent"

internal class GeminiVlmProvider(
    private val service: GeminiService,
    private val apiKey: String,
    private val json: Json,
    override val model: String,
) : VlmProvider {

    override suspend fun groupSimilar(images: List<AiImage>, instruction: String?): GroupingResult =
        aiCall(PROVIDER) {
            if (images.isEmpty()) return@aiCall GroupingResult(emptyList())
            val parts = buildList {
                add(GeminiPart(text = GroupingContract.instruction(images.size, instruction)))
                images.forEach { add(it.toPart()) }
            }
            val response = service.generateContent(
                url = url(model),
                apiKey = apiKey,
                request = GeminiGenerateRequest(contents = listOf(GeminiContent(role = "user", parts = parts))),
            )
            response.failIfError()
            val text = response.firstText()
                ?: throw AiException("$PROVIDER returned no grouping content")
            GroupingResult(
                groups = GroupingContract.parse(json, text, images),
                usage = response.tokenUsage(),
            )
        }
}

internal class GeminiLlmProvider(
    private val service: GeminiService,
    private val apiKey: String,
    override val model: String,
) : LlmProvider {

    override suspend fun optimizePrompt(rawPrompt: String, context: PromptContext): PromptResult =
        aiCall(PROVIDER) {
            val response = service.generateContent(
                url = url(model),
                apiKey = apiKey,
                request = GeminiGenerateRequest(
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = PromptOptimization.system(context.editableAspects))),
                    ),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(
                                GeminiPart(
                                    text = PromptOptimization.user(rawPrompt, context.groupTitle, context.groupReason),
                                ),
                            ),
                        ),
                    ),
                ),
            )
            response.failIfError()
            val text = response.firstText()?.trim()
                ?: throw AiException("$PROVIDER returned no optimized prompt")
            PromptResult(prompt = text, usage = response.tokenUsage())
        }
}

internal class GeminiImageGenProvider(
    private val service: GeminiService,
    private val apiKey: String,
    override val model: String,
) : ImageGenProvider {

    override suspend fun generate(
        references: List<AiImage>,
        prompt: String,
        options: ImageGenOptions,
    ): ImageGenResult = aiCall(PROVIDER) {
        val parts = buildList {
            add(GeminiPart(text = prompt))
            references.forEach { add(it.toPart()) }
        }
        val response = service.generateContent(
            url = url(model),
            apiKey = apiKey,
            request = GeminiGenerateRequest(contents = listOf(GeminiContent(role = "user", parts = parts))),
        )
        response.failIfError()
        val imagePart = response.candidates.firstOrNull()?.content?.parts
            ?.firstOrNull { it.inlineData != null }?.inlineData
            ?: throw AiException("$PROVIDER returned no image data")
        ImageGenResult(
            images = listOf(
                GeneratedImage(bytes = imagePart.data.decodeBase64(), mimeType = imagePart.mimeType),
            ),
            usage = response.tokenUsage(imageCount = 1),
        )
    }
}
