package com.shotyou.app.domain.repository

import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.model.UsageRecord
import kotlinx.coroutines.flow.Flow

/** Outcome of a delete request. On Android 11+ deleting another app's media needs user
 *  consent, surfaced as an [IntentSender] the UI must launch. */
sealed interface DeleteOutcome {
    /** Deleted directly (legacy / owned media). */
    data class Deleted(val count: Int) : DeleteOutcome
    /** The UI must launch this IntentSender to get the user's consent (Android 11+). */
    data class NeedsConsent(val intentSender: android.content.IntentSender) : DeleteOutcome
}

/** Reads photos from the device gallery and decodes bytes for AI calls. */
interface PhotoRepository {
    suspend fun queryImages(limit: Int = 2000): List<Photo>
    suspend fun loadAiImage(uri: String, maxEdge: Int = 1024): AiImage
    /** Persist generated image bytes to the gallery; returns the new content uri. */
    suspend fun saveGeneratedImage(bytes: ByteArray, mimeType: String, displayName: String): String
    /** Delete the given gallery images. May require user consent (see [DeleteOutcome]). */
    suspend fun deletePhotos(uris: List<String>): DeleteOutcome
}

/** Runs VLM grouping over a selection of photos. */
interface GroupingRepository {
    suspend fun groupPhotos(uris: List<String>, instruction: String? = null): List<PhotoGroup>
}

/** Prompt-template library (Room-backed). */
interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>
    suspend fun getTemplate(id: Long): Template?
    suspend fun upsert(template: Template): Long
    suspend fun delete(id: Long)
    suspend fun optimizePrompt(rawPrompt: String, groupTitle: String?, groupReason: String?): String
    /** Seeds built-in templates on first run. Safe to call repeatedly. */
    suspend fun ensureSeeded()
}

/** One prompt variant to generate as a candidate within a batch. */
data class GenerationVariant(
    val prompt: String,
    val label: String? = null,
)

/** Enqueues and observes background generation jobs. */
interface GenerationRepository {
    fun observeJobs(): Flow<List<GenerationJob>>
    fun observeJob(id: String): Flow<GenerationJob?>
    fun observeBatch(batchId: String): Flow<List<GenerationJob>>
    suspend fun enqueue(
        groupId: String?,
        groupTitle: String?,
        prompt: String,
        referenceUris: List<String>,
    ): String
    /** Enqueue several candidate variants for one group; returns the shared batchId. */
    suspend fun enqueueBatch(
        groupId: String?,
        groupTitle: String?,
        variants: List<GenerationVariant>,
        referenceUris: List<String>,
    ): String
    /** Add one more candidate (e.g. an "ask for changes" iteration) to an existing batch. */
    suspend fun addVariant(
        batchId: String,
        prompt: String,
        label: String?,
        referenceUris: List<String>,
    ): String
    suspend fun retry(id: String)
    suspend fun cancel(id: String)
    suspend fun clearFinished()

    /** Whether the queue is paused (no new jobs start until resumed). */
    fun observePaused(): Flow<Boolean>
    /** Pause: stop active work and keep jobs queued. */
    suspend fun pauseQueue()
    /** Resume: re-enqueue all queued jobs. */
    suspend fun resumeQueue()
}

/** Usage / call-count dashboard data. */
interface UsageRepository {
    fun observeRecords(): Flow<List<UsageRecord>>
    suspend fun record(record: UsageRecord)
    suspend fun clear()
}

/** Persisted AI settings (DataStore). */
interface SettingsRepository {
    val settings: Flow<AiSettings>
    suspend fun current(): AiSettings
    suspend fun update(transform: (AiSettings) -> AiSettings)
}
