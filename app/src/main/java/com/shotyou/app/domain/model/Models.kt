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
data class PhotoGroup(
    val id: String,
    val title: String,
    val reason: String,
    val photoUris: List<String>,
    val referenceUris: List<String>,
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

/** One image-generation request, tracked through the background queue. */
data class GenerationJob(
    val id: String,
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
