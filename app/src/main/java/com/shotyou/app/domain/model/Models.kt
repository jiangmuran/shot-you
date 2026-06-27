package com.shotyou.app.domain.model

/** A photo from the device gallery (MediaStore). */
data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateTakenMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val bucketName: String? = null,
)

/** A cluster of near-duplicate photos proposed by the VLM. */
@kotlinx.serialization.Serializable
data class PhotoGroup(
    val id: String,
    val title: String,
    val reason: String,
    val photoUris: List<String>,
    val referenceUris: List<String>,
    /** Coarse category from the VLM (e.g. people / scenery / food / other). */
    val category: String? = null,
    /** VLM suggestion: whether this group is worth generating (false = too similar to
     *  another group / low value — pre-unchecked in the curation screen to save cost). */
    val recommended: Boolean = true,
) {
    val size: Int get() = photoUris.size
}

/** A reusable prompt template from the user's library. */
data class Template(
    val id: Long = 0L,
    val name: String,
    val prompt: String,
    val tags: List<String> = emptyList(),
    val builtIn: Boolean = false,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
)

enum class JobStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

/**
 * One image-generation request, tracked through the background queue. Jobs that belong to
 * the same "generate" action for a group share a [batchId]; [variantIndex] orders the
 * candidates within that batch.
 */
data class GenerationJob(
    val id: String,
    val batchId: String = "",
    val variantIndex: Int = 0,
    val variantLabel: String? = null,
    val groupId: String? = null,
    val groupTitle: String? = null,
    val prompt: String,
    val referenceUris: List<String>,
    val status: JobStatus = JobStatus.QUEUED,
    val resultUri: String? = null,
    val errorMessage: String? = null,
    val provider: String,
    val model: String,
    val attempt: Int = 0,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
)

enum class AiOperation { GROUPING, PROMPT_OPTIMIZE, IMAGE_GENERATION }

/** A single billable AI call, for the usage dashboard. */
data class UsageRecord(
    val id: Long = 0L,
    val provider: String,
    val model: String,
    val operation: AiOperation,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val imageCount: Int = 0,
    val estimatedCostUsd: Double = 0.0,
    val success: Boolean = true,
    val timestampMs: Long = 0L,
)
