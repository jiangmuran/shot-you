package com.shotyou.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shotyou.app.domain.repository.GroupingRepository
import com.shotyou.app.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Runs VLM classification for a session in the background, so the Library never blocks on it.
 * On success the session moves to READY_FOR_REVIEW (the user curates it from the Queue).
 */
@HiltWorker
class ClassificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val groupingRepository: GroupingRepository,
    private val sessionRepository: SessionRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val session = sessionRepository.getSession(id) ?: return Result.failure()
        return try {
            val groups = groupingRepository.groupPhotos(session.photoUris)
            sessionRepository.completeClassification(id, groups)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            sessionRepository.failClassification(id, e.message ?: "Classification failed")
            Result.failure()
        }
    }

    companion object {
        const val KEY_SESSION_ID = "sessionId"
    }
}
