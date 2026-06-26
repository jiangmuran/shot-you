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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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
        val n = options.count.coerceAtLeast(1)
        val response = if (references.isEmpty()) {
            // No source photos → plain text-to-image.
            service.imageGenerations(
                auth = authHeader(apiKey),
                request = OpenAiImageRequest(model = model, prompt = prompt, n = n, size = options.size),
            )
        } else {
            // Feed the actual reference photos to the model via the edits endpoint so the
            // result is grounded in the input (not hallucinated from text).
            val faithful = prompt +
                "\n\nIMPORTANT: edit/enhance the SAME subject shown in the reference image(s). " +
                "Preserve their identity, species, key features and overall composition. " +
                "Do not invent a different subject."
            val parts = buildList {
                add(MultipartBody.Part.createFormData("model", model))
                add(MultipartBody.Part.createFormData("prompt", faithful))
                add(MultipartBody.Part.createFormData("n", n.toString()))
                options.size?.let { add(MultipartBody.Part.createFormData("size", it)) }
                references.forEachIndexed { index, ref ->
                    val body = ref.bytes.toRequestBody(ref.mimeType.toMediaTypeOrNull(), 0, ref.bytes.size)
                    val ext = when (ref.mimeType.lowercase()) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                    // gpt-image-1 accepts multiple images under the "image[]" field.
                    add(MultipartBody.Part.createFormData("image[]", "ref_$index.$ext", body))
                }
            }
            service.imageEdits(auth = authHeader(apiKey), parts = parts)
        }
        response.error?.let { throw AiException("$PROVIDER API error: ${it.message}") }
        val images = response.data.mapNotNull { d -> d.b64Json?.let { GeneratedImage(bytes = it.decodeBase64(), mimeType = "image/png") } }
        if (images.isEmpty()) throw AiException("$PROVIDER returned no image data")
        ImageGenResult(images = images, usage = TokenUsage(imageCount = images.size))
    }
}
