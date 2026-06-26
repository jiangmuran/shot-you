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

private const val PROVIDER = "OpenAI"

private fun authHeader(apiKey: String) = "Bearer $apiKey"

private fun OpenAiChatResponse.failIfError() {
    error?.let { throw AiException("$PROVIDER API error: ${it.message}") }
}

private fun OpenAiChatResponse.tokenUsage(): TokenUsage = TokenUsage(
    promptTokens = usage?.promptTokens ?: 0,
    completionTokens = usage?.completionTokens ?: 0,
)

private fun OpenAiChatResponse.firstText(): String? =
    choices.firstOrNull()?.message?.content

private fun AiImage.toDataUri(): String = "data:$mimeType;base64,${bytes.base64()}"

internal class OpenAiVlmProvider(
    private val service: OpenAiService,
    private val apiKey: String,
    private val json: Json,
    override val model: String,
) : VlmProvider {

    override suspend fun groupSimilar(images: List<AiImage>, instruction: String?): GroupingResult =
        aiCall(PROVIDER) {
            if (images.isEmpty()) return@aiCall GroupingResult(emptyList())
            val parts = buildList {
                add(OpenAiContentPart.text(GroupingContract.instruction(images.size, instruction)))
                images.forEach { add(OpenAiContentPart.imageDataUri(it.toDataUri())) }
            }
            val response = service.chatCompletions(
                auth = authHeader(apiKey),
                request = OpenAiChatRequest(
                    model = model,
                    messages = listOf(OpenAiMessage(role = "user", content = parts)),
                ),
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

internal class OpenAiLlmProvider(
    private val service: OpenAiService,
    private val apiKey: String,
    override val model: String,
) : LlmProvider {

    override suspend fun optimizePrompt(rawPrompt: String, context: PromptContext): PromptResult =
        aiCall(PROVIDER) {
            val response = service.chatCompletions(
                auth = authHeader(apiKey),
                request = OpenAiChatRequest(
                    model = model,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = listOf(OpenAiContentPart.text(PromptOptimization.system(context.editableAspects))),
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = listOf(
                                OpenAiContentPart.text(
                                    PromptOptimization.user(rawPrompt, context.groupTitle, context.groupReason),
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

internal class OpenAiImageGenProvider(
    private val service: OpenAiService,
    private val apiKey: String,
    override val model: String,
) : ImageGenProvider {

    override suspend fun generate(
        references: List<AiImage>,
        prompt: String,
        options: ImageGenOptions,
    ): ImageGenResult = aiCall(PROVIDER) {
        // The images/generations endpoint takes no reference uploads; note their presence in text.
        val effectivePrompt = if (references.isEmpty()) {
            prompt
        } else {
            "$prompt\n\n(Base the result on ${references.size} reference photo(s) of the same subject; preserve their identity.)"
        }
        val response = service.imageGenerations(
            auth = authHeader(apiKey),
            request = OpenAiImageRequest(
                model = model,
                prompt = effectivePrompt,
                n = options.count.coerceAtLeast(1),
                size = options.size,
            ),
        )
        response.error?.let { throw AiException("$PROVIDER API error: ${it.message}") }
        val images = response.data.mapNotNull { d -> d.b64Json?.let { GeneratedImage(bytes = it.decodeBase64(), mimeType = "image/png") } }
        if (images.isEmpty()) throw AiException("$PROVIDER returned no image data")
        ImageGenResult(images = images, usage = TokenUsage(imageCount = images.size))
    }
}
