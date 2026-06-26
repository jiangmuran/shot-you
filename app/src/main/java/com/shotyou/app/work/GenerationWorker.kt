package com.shotyou.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
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
import com.shotyou.app.domain.model.GenerationJob
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

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val job = jobDao.getById(jobId)?.toDomain() ?: return Result.failure()

        // Mark RUNNING.
        jobDao.update(job.copy(status = JobStatus.RUNNING, updatedAtMs = clock.now()).toEntity())
        // Optional foreground notification — guarded so a missing permission never crashes the job.
        runCatching { setForeground(buildForegroundInfo(job)) }

        val settings = settingsRepository.current()

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
                Result.failure()
            }
        }
    }

    private fun buildForegroundInfo(job: GenerationJob): ForegroundInfo {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Image generation",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Background image generation jobs" }
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Generating image")
            .setContentText(job.groupTitle ?: job.prompt.take(48))
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(job.id.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(job.id.hashCode(), notification)
        }
    }

    companion object {
        const val KEY_JOB_ID = "jobId"
        private const val CHANNEL_ID = "shotyou_generation"
    }
}
