package com.shotyou.app.data.remote.ai

import android.util.Base64
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.ai.VlmGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Base64-encode bytes for inline image payloads (no line wrapping). */
internal fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

/** Decode a base64 string (tolerant of wrapping / data-uri prefixes). */
internal fun String.decodeBase64(): ByteArray {
    val cleaned = substringAfterLast(",").trim()
    return Base64.decode(cleaned, Base64.DEFAULT)
}

/**
 * Runs a provider HTTP block, translating Retrofit/HTTP/parse failures into a user-facing
 * [AiException] that includes the upstream error text when available.
 */
internal suspend fun <T> aiCall(provider: String, block: suspend () -> T): T =
    try {
        block()
    } catch (e: AiException) {
        throw e
    } catch (e: HttpException) {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        throw AiException("$provider API error ${e.code()}: ${body?.takeIf { it.isNotBlank() } ?: e.message()}", e)
    } catch (e: Exception) {
        throw AiException("$provider request failed: ${e.message ?: e.javaClass.simpleName}", e)
    }

/**
 * Strict-JSON grouping contract shared by all VLM providers. Photos are referenced by their
 * 0-based index in the order they were sent to the model.
 */
internal object GroupingContract {

    fun instruction(imageCount: Int, userInstruction: String?): String = buildString {
        appendLine("You are given $imageCount photos, indexed 0 to ${imageCount - 1} in the order provided.")
        appendLine("Cluster together photos that are visually similar or near-duplicates: the same scene or people, burst shots, or only minor differences in pose, expression or framing.")
        appendLine("Every photo index must appear in exactly one group. A photo with no similar match forms its own single-member group.")
        appendLine("For each group, nominate 1-2 best reference frames (sharpest, eyes open, best composition).")
        userInstruction?.takeIf { it.isNotBlank() }?.let {
            appendLine("Additional user guidance: $it")
        }
        appendLine("Respond with STRICT JSON only — no markdown, no code fences, no commentary — exactly in this shape:")
        appendLine("""{"groups":[{"members":[0,2],"references":[0],"title":"short title","reason":"why these belong together"}]}""")
        append("members and references are 0-based indices into the provided photos.")
    }

    /** Parse the model's raw text into [VlmGroup]s, mapping indices back to image ids. */
    fun parse(json: Json, raw: String, images: List<AiImage>): List<VlmGroup> {
        val payload = extractJson(raw)
        val parsed = runCatching { json.decodeFromString<GroupingJson>(payload) }.getOrElse {
            throw AiException("Could not parse grouping response as JSON: ${raw.take(300)}", it)
        }
        return parsed.groups.mapNotNull { g ->
            val memberIds = g.members.mapNotNull { images.getOrNull(it)?.id }.distinct()
            if (memberIds.isEmpty()) return@mapNotNull null
            val referenceIds = g.references.mapNotNull { images.getOrNull(it)?.id }
                .distinct()
                .ifEmpty { listOf(memberIds.first()) }
            VlmGroup(
                memberIds = memberIds,
                referenceIds = referenceIds,
                title = g.title.ifBlank { "Group" },
                reason = g.reason,
            )
        }
    }

    /** Strip markdown fences / surrounding prose and isolate the JSON object. */
    private fun extractJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").trim()
            val fence = s.lastIndexOf("```")
            if (fence >= 0) s = s.substring(0, fence).trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start in 0 until end) s = s.substring(start, end + 1)
        return s
    }
}

@Serializable
internal data class GroupingJson(val groups: List<GroupJson> = emptyList())

@Serializable
internal data class GroupJson(
    val members: List<Int> = emptyList(),
    val references: List<Int> = emptyList(),
    val title: String = "",
    val reason: String = "",
)

/** Shared system-style instruction for prompt optimisation. */
internal object PromptOptimization {
    fun system(editableAspects: List<String>): String = buildString {
        appendLine("You are an expert prompt engineer for AI image generation.")
        appendLine("Rewrite the user's request into a single vivid, specific, photorealistic image-generation prompt.")
        appendLine("Preserve the subject's identity and key facial/physical features.")
        appendLine("You MAY freely adjust these aspects if it improves the result: ${editableAspects.joinToString(", ")}.")
        appendLine("Keep it under 120 words. Return ONLY the rewritten prompt text — no preamble, quotes, or explanation.")
    }

    fun user(rawPrompt: String, groupTitle: String?, groupReason: String?): String = buildString {
        groupTitle?.takeIf { it.isNotBlank() }?.let { appendLine("Photo group: $it") }
        groupReason?.takeIf { it.isNotBlank() }?.let { appendLine("Why grouped: $it") }
        append("User request: ")
        append(rawPrompt)
    }
}
