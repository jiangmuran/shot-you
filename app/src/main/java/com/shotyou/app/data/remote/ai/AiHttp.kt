package com.shotyou.app.data.remote.ai

import android.util.Base64
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.ai.VlmGroup
import kotlinx.coroutines.CancellationException
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
    } catch (e: CancellationException) {
        throw e // never swallow coroutine cancellation
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
        appendLine("Describe ONLY what is actually visible in the photos. Never invent or add subjects, scenery, people or content that are not present. Base the title and reason strictly on what you see.")
        appendLine("Cluster together photos that are visually similar or near-duplicates: the same subject/scene/people, burst shots, or only minor differences in pose, expression or framing.")
        appendLine("Every photo index must appear in exactly one group. A photo with no similar match forms its own single-member group.")
        appendLine("When there are many photos, stay organized and consistent — do NOT merge unrelated content just to reduce the number of groups, and do not get confused by the volume.")
        appendLine("For each group, nominate 1-2 best reference frames (sharpest, best subject, best composition).")
        appendLine("Also classify each group with a category: one of \"people\", \"scenery\", \"food\", \"animal\", \"object\", or \"other\".")
        appendLine("Set \"recommended\" to false for groups that are NOT worth generating a new image for — e.g. a group that is essentially redundant with another group, or low-value content. Set it to true otherwise. Image generation is expensive, so be selective.")
        userInstruction?.takeIf { it.isNotBlank() }?.let {
            appendLine("Additional user guidance: $it")
        }
        appendLine("Respond with STRICT JSON only — no markdown, no code fences, no commentary — exactly in this shape:")
        appendLine("""{"groups":[{"members":[0,2],"references":[0],"title":"short title","reason":"why these belong together","category":"people","recommended":true}]}""")
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
                category = g.category?.lowercase()?.takeIf { it.isNotBlank() },
                recommended = g.recommended,
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
    val category: String? = null,
    val recommended: Boolean = true,
)

/** Shared system-style instruction for prompt optimisation. */
internal object PromptOptimization {
    fun system(editableAspects: List<String>): String = buildString {
        appendLine("You are a professional portrait photographer and prompt designer for AI image generation (Nano Banana / gpt-image style).")
        appendLine("Rewrite the user's request into one vivid, specific, photorealistic image-generation prompt.")
        appendLine("Use precise photographic and facial terminology: name facial features exactly (eyes, brows, lashes, nose bridge and tip, lips, jawline, skin) and use real camera/optics terms (e.g. 85mm portrait lens, soft directional light, golden hour, shallow depth of field, gentle bokeh, vertical orientation).")
        appendLine("Apply tasteful, NATURAL beautification while strictly preserving the subject's true identity and likeness: clean even-toned skin that keeps natural texture and pores (never plastic or waxy), subtle brightening, bright clear eyes, neat lashes and brows, gently refined nose and lip shaping, light natural makeup.")
        appendLine("Stay believable, not over-processed — it must look like a real photograph, not obviously AI-generated.")
        appendLine("Only describe the scene/environment if the user asked for one; otherwise do NOT invent or change the scene.")
        appendLine("You MAY refine ${editableAspects.joinToString(", ")} when it improves the shot.")
        appendLine("Keep each sentence concise (roughly under 20 words). No coordinates, no code, no markdown.")
        appendLine("Write the rewritten prompt in the SAME language as the user's request (reply in Chinese if the request is in Chinese).")
        append("Return ONLY the rewritten prompt text — no preamble, quotes, or explanation.")
    }

    fun user(rawPrompt: String, groupTitle: String?, groupReason: String?): String = buildString {
        groupTitle?.takeIf { it.isNotBlank() }?.let { appendLine("Photo group: $it") }
        groupReason?.takeIf { it.isNotBlank() }?.let { appendLine("Why grouped: $it") }
        append("User request: ")
        append(rawPrompt)
    }
}
