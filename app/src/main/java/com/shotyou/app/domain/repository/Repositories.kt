package com.shotyou.app.domain.repository

import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.model.UsageRecord
import kotlinx.coroutines.flow.Flow

/** Reads photos from the device gallery and decodes bytes for AI calls. */
interface PhotoRepository {
    suspend fun queryImages(limit: Int = 2000): List<Photo>
    suspend fun loadAiImage(uri: String, maxEdge: Int = 1024): AiImage
    /** Persist generated image bytes to the gallery; returns the new content uri. */
    suspend fun saveGeneratedImage(bytes: ByteArray, mimeType: String, displayName: String): String
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

/** Enqueues and observes background generation jobs. */
interface GenerationRepository {
    fun observeJobs(): Flow<List<GenerationJob>>
    fun observeJob(id: String): Flow<GenerationJob?>
    suspend fun enqueue(
        groupId: String?,
        groupTitle: String?,
        prompt: String,
        referenceUris: List<String>,
    ): String
    suspend fun retry(id: String)
    suspend fun cancel(id: String)
    suspend fun clearFinished()
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
