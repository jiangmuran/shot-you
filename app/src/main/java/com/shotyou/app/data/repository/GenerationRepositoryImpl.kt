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
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.util.Clock
import com.shotyou.app.util.IdGenerator
import com.shotyou.app.work.GenerationWorker
import kotlinx.coroutines.flow.Flow
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

    override suspend fun enqueue(
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
            groupId = groupId,
            groupTitle = groupTitle,
            prompt = prompt,
            referenceUris = referenceUris,
            status = JobStatus.QUEUED,
            provider = settings.imageProvider.name,
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

    private fun enqueueWork(id: String, settings: AiSettings) {
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
