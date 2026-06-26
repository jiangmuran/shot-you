package com.shotyou.app.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.shotyou.app.data.local.GenerationJobDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.data.local.toEntity
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.GenerationVariant
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.util.Clock
import com.shotyou.app.util.IdGenerator
import com.shotyou.app.work.GenerationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues and observes background generation jobs. Each job is backed by a unique
 * [WorkManager] work request keyed `gen-<jobId>` running a [GenerationWorker].
 */
@Singleton
class GenerationRepositoryImpl @Inject constructor(
    private val jobDao: GenerationJobDao,
    private val workManager: WorkManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val settingsRepository: SettingsRepository,
) : GenerationRepository {

    override fun observeJobs(): Flow<List<GenerationJob>> =
        jobDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeJob(id: String): Flow<GenerationJob?> =
        jobDao.observeById(id).map { it?.toDomain() }

    override fun observeBatch(batchId: String): Flow<List<GenerationJob>> =
        jobDao.observeByBatch(batchId).map { list -> list.map { it.toDomain() } }

    override suspend fun enqueue(
        groupId: String?,
        groupTitle: String?,
        prompt: String,
        referenceUris: List<String>,
    ): String {
        val batchId = idGenerator.newId()
        return createJob(batchId, 0, null, groupId, groupTitle, prompt, referenceUris)
    }

    override suspend fun enqueueBatch(
        groupId: String?,
        groupTitle: String?,
        variants: List<GenerationVariant>,
        referenceUris: List<String>,
    ): String {
        val batchId = idGenerator.newId()
        variants.forEachIndexed { index, variant ->
            createJob(batchId, index, variant.label, groupId, groupTitle, variant.prompt, referenceUris)
        }
        return batchId
    }

    override suspend fun addVariant(
        batchId: String,
        prompt: String,
        label: String?,
        referenceUris: List<String>,
    ): String {
        val existing = jobDao.observeByBatch(batchId).first()
        val sample = existing.firstOrNull()?.toDomain()
        val nextIndex = (existing.maxOfOrNull { it.variantIndex } ?: -1) + 1
        return createJob(
            batchId = batchId,
            variantIndex = nextIndex,
            label = label,
            groupId = sample?.groupId,
            groupTitle = sample?.groupTitle,
            prompt = prompt,
            referenceUris = referenceUris,
        )
    }

    private suspend fun createJob(
        batchId: String,
        variantIndex: Int,
        label: String?,
        groupId: String?,
        groupTitle: String?,
        prompt: String,
        referenceUris: List<String>,
    ): String {
        val settings = settingsRepository.current()
        val now = clock.now()
        val id = idGenerator.newId()
        val job = GenerationJob(
            id = id,
            batchId = batchId,
            variantIndex = variantIndex,
            variantLabel = label,
            groupId = groupId,
            groupTitle = groupTitle,
            prompt = prompt,
            referenceUris = referenceUris,
            status = JobStatus.QUEUED,
            provider = providerLabel(settings.apiBaseUrl),
            model = settings.imageModel,
            createdAtMs = now,
            updatedAtMs = now,
        )
        jobDao.insert(job.toEntity())
        enqueueWork(id, settings)
        return id
    }

    override suspend fun retry(id: String) {
        val job = jobDao.getById(id)?.toDomain() ?: return
        val settings = settingsRepository.current()
        val updated = job.copy(
            status = JobStatus.QUEUED,
            attempt = job.attempt + 1,
            errorMessage = null,
            updatedAtMs = clock.now(),
        )
        jobDao.update(updated.toEntity())
        enqueueWork(id, settings)
    }

    override suspend fun cancel(id: String) {
        workManager.cancelUniqueWork(workName(id))
        val job = jobDao.getById(id)?.toDomain() ?: return
        jobDao.update(job.copy(status = JobStatus.CANCELLED, updatedAtMs = clock.now()).toEntity())
    }

    override suspend fun clearFinished() = jobDao.clearFinished()

    override fun observePaused(): Flow<Boolean> =
        settingsRepository.settings.map { it.queuePaused }

    override suspend fun pauseQueue() {
        settingsRepository.update { it.copy(queuePaused = true) }
        jobDao.getActive().forEach { e ->
            workManager.cancelUniqueWork(workName(e.id))
            if (e.status == JobStatus.RUNNING.name) {
                jobDao.update(e.copy(status = JobStatus.QUEUED.name, updatedAtMs = clock.now()))
            }
        }
    }

    override suspend fun resumeQueue() {
        settingsRepository.update { it.copy(queuePaused = false) }
        val settings = settingsRepository.current()
        jobDao.getActive()
            .filter { it.status == JobStatus.QUEUED.name }
            .forEach { e -> enqueueWork(e.id, settings) }
    }

    private fun enqueueWork(id: String, settings: AiSettings) {
        if (settings.queuePaused) return // queued but not started until resumed
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (settings.requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED,
            )
            .build()
        val request = OneTimeWorkRequestBuilder<GenerationWorker>()
            .setInputData(workDataOf(GenerationWorker.KEY_JOB_ID to id))
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        workManager.enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun workName(id: String) = "gen-$id"
}

/** A short, human-readable label for the configured endpoint (its host), for usage records. */
internal fun providerLabel(apiBaseUrl: String): String =
    runCatching { java.net.URI(apiBaseUrl).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: "openai"
