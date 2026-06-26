package com.shotyou.app.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.shotyou.app.data.local.GenerationJobDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.data.local.toEntity
import com.shotyou.app.domain.ai.AiException
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.JobStatus
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.UsageRepository
import com.shotyou.app.util.Clock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Runs one image-generation [GenerationJob] in the background. Enqueued by
 * [com.shotyou.app.data.repository.GenerationRepositoryImpl] as unique work `gen-<jobId>`.
 *
 * Must be a [HiltWorker] because the default WorkManager initializer is removed from the
 * manifest and the factory is provided via Hilt ([com.shotyou.app.ShotYouApp]).
 *
 * Background persistence & notifications (driven by [AiSettings]):
 *  - `runInBackground`: the work is promoted to a foreground service so the OS keeps it alive
 *    while the screen is off / the app is backgrounded. The foreground notification doubles as
 *    the progress notification.
 *  - `progressNotifications`: when not already running as a foreground service, an ongoing
 *    progress notification is shown while running. Either flag results in a terminal
 *    (success/failure) notification at the end.
 */
@HiltWorker
class GenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val jobDao: GenerationJobDao,
    private val factory: AiProviderFactory,
    private val settingsRepository: SettingsRepository,
    private val photoRepository: PhotoRepository,
    private val usageRepository: UsageRepository,
    private val clock: Clock,
) : CoroutineWorker(appContext, params) {

    private val notifications by lazy { GenerationNotifications(applicationContext) }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val job = jobDao.getById(jobId)?.toDomain() ?: return Result.failure()

        // Mark RUNNING.
        jobDao.update(job.copy(status = JobStatus.RUNNING, updatedAtMs = clock.now()).toEntity())

        val settings = settingsRepository.current()

        // Decide how progress is surfaced. Foreground promotion is best-effort: wrapped in
        // runCatching so the OS refusing to start the service can never crash the job.
        val foreground = settings.runInBackground
        if (foreground) {
            runCatching { setForeground(getForegroundInfo()) }
        } else if (settings.progressNotifications) {
            runCatching { notifications.notifyProgress() }
        }
        // Either flag means we post a terminal notification when the job finishes.
        val notify = foreground || settings.progressNotifications

        return try {
            if (settings.minRequestIntervalMs > 0) delay(settings.minRequestIntervalMs)

            val refs: List<AiImage> = job.referenceUris.map { photoRepository.loadAiImage(it) }
            val provider = factory.imageGen(settings)
            val result = provider.generate(refs, job.prompt)
            val image = result.images.firstOrNull()
                ?: throw AiException("Provider returned no image")
            val uri = photoRepository.saveGeneratedImage(image.bytes, image.mimeType, "ShotYou_${job.id}")

            val now = clock.now()
            jobDao.update(
                job.copy(
                    status = JobStatus.SUCCEEDED,
                    resultUri = uri,
                    errorMessage = null,
                    updatedAtMs = now,
                ).toEntity(),
            )
            usageRepository.record(
                UsageRecord(
                    provider = job.provider,
                    model = provider.model,
                    operation = AiOperation.IMAGE_GENERATION,
                    imageCount = 1,
                    estimatedCostUsd = result.usage.estimatedCostUsd,
                    success = true,
                    timestampMs = now,
                ),
            )
            if (notify) runCatching { notifications.notifySuccess() }
            Result.success()
        } catch (t: Throwable) {
            if (settings.autoRetryOnFailure && runAttemptCount < settings.maxRetries) {
                jobDao.update(
                    job.copy(
                        status = JobStatus.QUEUED,
                        attempt = runAttemptCount + 1,
                        updatedAtMs = clock.now(),
                    ).toEntity(),
                )
                // Not terminal yet — the next attempt will re-surface progress.
                Result.retry()
            } else {
                val now = clock.now()
                jobDao.update(
                    job.copy(
                        status = JobStatus.FAILED,
                        errorMessage = t.message ?: "Generation failed",
                        updatedAtMs = now,
                    ).toEntity(),
                )
                runCatching {
                    usageRepository.record(
                        UsageRecord(
                            provider = job.provider,
                            model = job.model,
                            operation = AiOperation.IMAGE_GENERATION,
                            imageCount = 0,
                            estimatedCostUsd = 0.0,
                            success = false,
                            timestampMs = now,
                        ),
                    )
                }
                if (notify) runCatching { notifications.notifyFailure() }
                Result.failure()
            }
        }
    }

    /**
     * Foreground notification used when `runInBackground` is enabled. The ongoing progress
     * notification doubles as the foreground-service notification.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notifications.buildProgress()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                GenerationNotifications.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(GenerationNotifications.NOTIF_ID, notification)
        }
    }

    companion object {
        const val KEY_JOB_ID = "jobId"
    }
}
